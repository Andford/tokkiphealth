package mx.loperstack.tokkiphealth.corpsefinder.ui.details

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.flow.SingleEventFlow
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.uix.PagedDetailsViewModel
import mx.loperstack.tokkiphealth.common.uix.resolveTarget
import mx.loperstack.tokkiphealth.corpsefinder.core.Corpse
import mx.loperstack.tokkiphealth.corpsefinder.core.CorpseFinder
import mx.loperstack.tokkiphealth.corpsefinder.core.CorpseIdentifier
import mx.loperstack.tokkiphealth.corpsefinder.core.hasData
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderDeleteTask
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderTask
import mx.loperstack.tokkiphealth.corpsefinder.ui.CorpseDetailsRoute
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskSubmitter
import mx.loperstack.tokkiphealth.main.core.taskmanager.uniqueTaskResults
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class CorpseDetailsViewModel @Inject constructor(
    handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val corpseFinder: CorpseFinder,
    private val taskSubmitter: TaskSubmitter,
) : PagedDetailsViewModel<CorpseDetailsRoute, CorpseIdentifier>(handle, dispatcherProvider, tag = TAG) {

    val events = SingleEventFlow<Event>()

    override fun bindRouteLogValue(route: CorpseDetailsRoute): Any? = route.corpsePath

    init {
        // `mapNotNull { it.data }` skips the null transitions that performScan publishes at
        // the start of a refresh, so navUp fires only on real "drain to empty", not loading.
        autoNavUpOnEmpty(corpseFinder.state.mapNotNull { it.data }.map { it.hasData })

        taskSubmitter.uniqueTaskResults<CorpseFinderTask.Result>(SDMTool.Type.CORPSEFINDER)
            .onEach { events.tryEmit(Event.TaskResult(it)) }
            .launchInViewModel()
    }

    val state: StateFlow<State> = routeFlow.filterNotNull().flatMapLatest { route ->
        // Item production excludes progress so high-frequency progress ticks during a scan don't
        // re-sort the corpse list and re-run resolveTarget. Progress is merged in last (below) as a
        // cheap field swap that preserves the items List instance, letting keyed pager pages skip.
        val itemsState = corpseFinder.state
            .map { it.data }
            .filterNotNull()
            .distinctUntilChanged()
            .map { data ->
                val sortedCorpses = data.corpses
                    .sortedByDescending { it.size }
                    .toList()

                val availableTarget = resolveTarget(
                    items = sortedCorpses,
                    requestedTarget = currentTarget ?: route.corpsePath,
                    lastPosition = lastPosition,
                    identifierOf = { it.identifier },
                    onPositionTracked = { lastPosition = it },
                )

                State(
                    items = sortedCorpses,
                    target = availableTarget,
                )
            }

        combine(itemsState, corpseFinder.progress) { base, progress ->
            base.copy(progress = progress)
        }
    }.safeStateIn(
        initialValue = State(),
        onError = { State() },
    )

    fun onPageChanged(identifier: CorpseIdentifier) {
        log(TAG) { "onPageChanged($identifier)" }
        currentTarget = identifier
    }

    fun onConfirmDeleteCorpse(corpseId: CorpseIdentifier) = launch {
        log(TAG, INFO) { "onConfirmDeleteCorpse($corpseId)" }
        val snapshot = corpseFinder.state.first().data ?: return@launch
        if (snapshot.corpses.none { it.identifier == corpseId }) return@launch
        taskSubmitter.submit(CorpseFinderDeleteTask(targetCorpses = setOf(corpseId)))
    }

    fun onConfirmDeleteContent(corpseId: CorpseIdentifier, paths: Set<APath>) = launch {
        log(TAG, INFO) { "onConfirmDeleteContent($corpseId, ${paths.size})" }
        if (paths.isEmpty()) return@launch
        val snapshot = corpseFinder.state.first().data ?: return@launch
        val corpse = snapshot.corpses.firstOrNull { it.identifier == corpseId } ?: return@launch
        val validPaths = paths.filter { p -> corpse.content.any { it.lookedUp == p } }.toSet()
        if (validPaths.isEmpty()) return@launch
        taskSubmitter.submit(
            CorpseFinderDeleteTask(
                targetCorpses = setOf(corpseId),
                targetContent = validPaths,
            ),
        )
    }

    fun onExcludeCorpse(corpseId: CorpseIdentifier) = launch {
        log(TAG, INFO) { "onExcludeCorpse($corpseId)" }
        val snapshot = corpseFinder.state.first().data ?: return@launch
        if (snapshot.corpses.none { it.identifier == corpseId }) return@launch
        val undo = corpseFinder.exclude(setOf(corpseId))
        events.tryEmit(
            Event.ExclusionsCreated(
                count = undo.exclusionIds.size,
                undo = undo,
                restoreTarget = corpseId,
            ),
        )
    }

    fun onUndoExclude(undo: CorpseFinder.ExclusionUndo, restoreTarget: CorpseIdentifier) = launch {
        log(TAG, INFO) { "onUndoExclude(${undo.exclusionIds.size}, restore=$restoreTarget)" }
        currentTarget = restoreTarget
        corpseFinder.undoExclude(undo)
    }

    data class State(
        val items: List<Corpse> = emptyList(),
        val target: CorpseIdentifier? = null,
        val progress: Progress.Data? = null,
    )

    sealed interface Event {
        data class TaskResult(val result: CorpseFinderTask.Result) : Event
        data class ExclusionsCreated(
            val count: Int,
            val undo: CorpseFinder.ExclusionUndo,
            val restoreTarget: CorpseIdentifier,
        ) : Event
    }

    companion object {
        private val TAG = logTag("CorpseFinder", "Details", "ViewModel")
    }
}
