package mx.loperstack.tokkiphealth.systemcleaner.ui.details

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.flow.SingleEventFlow
import mx.loperstack.tokkiphealth.common.previews.PreviewOptions
import mx.loperstack.tokkiphealth.common.previews.PreviewRoute
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.uix.PagedDetailsViewModel
import mx.loperstack.tokkiphealth.common.uix.resolveTarget
import mx.loperstack.tokkiphealth.exclusion.ui.ExclusionsListRoute
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskSubmitter
import mx.loperstack.tokkiphealth.main.core.taskmanager.uniqueTaskResults
import mx.loperstack.tokkiphealth.systemcleaner.core.FilterContent
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleaner
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.FilterIdentifier
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.filterIdentifier
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock.ScreenshotsFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock.TrashedFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.hasData
import mx.loperstack.tokkiphealth.systemcleaner.core.tasks.SystemCleanerProcessingTask
import mx.loperstack.tokkiphealth.systemcleaner.core.tasks.SystemCleanerTask
import mx.loperstack.tokkiphealth.systemcleaner.ui.FilterContentDetailsRoute
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
class FilterContentDetailsViewModel @Inject constructor(
    handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val systemCleaner: SystemCleaner,
    private val taskSubmitter: TaskSubmitter,
) : PagedDetailsViewModel<FilterContentDetailsRoute, FilterIdentifier>(handle, dispatcherProvider, tag = TAG) {

    val events = SingleEventFlow<Event>()

    init {
        // `mapNotNull { it.data }` skips the null transitions that performScan publishes at
        // the start of a refresh, so navUp fires only on real "drain to empty", not loading.
        autoNavUpOnEmpty(systemCleaner.state.mapNotNull { it.data }.map { it.hasData })

        taskSubmitter.uniqueTaskResults<SystemCleanerTask.Result>(SDMTool.Type.SYSTEMCLEANER)
            .onEach { events.tryEmit(Event.TaskResult(it)) }
            .launchInViewModel()
    }

    val state: StateFlow<State> = routeFlow.filterNotNull().flatMapLatest { route ->
        // Item production excludes progress so high-frequency progress ticks during a scan don't
        // re-sort the filter-content list and re-run resolveTarget. Progress is merged in last (below)
        // as a cheap field swap that preserves the items List instance, letting keyed pager pages skip.
        val itemsState = systemCleaner.state
            .map { it.data }
            .filterNotNull()
            .distinctUntilChanged()
            .map { data ->
                val sortedContents = data.filterContents.sortedByDescending { it.size }

                val availableTarget = resolveTarget(
                    items = sortedContents,
                    requestedTarget = currentTarget ?: route.filterIdentifier,
                    lastPosition = lastPosition,
                    identifierOf = { it.identifier },
                    onPositionTracked = { lastPosition = it },
                )

                State(
                    items = sortedContents,
                    target = availableTarget,
                )
            }

        combine(itemsState, systemCleaner.progress) { base, progress ->
            base.copy(progress = progress)
        }
    }.safeStateIn(
        initialValue = State(),
        onError = { State() },
    )

    fun onPageChanged(identifier: FilterIdentifier) {
        log(TAG) { "onPageChanged($identifier)" }
        currentTarget = identifier
    }

    fun onConfirmDeleteFilter(id: FilterIdentifier) = launch {
        log(TAG, INFO) { "onConfirmDeleteFilter($id)" }
        val data = systemCleaner.state.first().data ?: return@launch
        if (data.filterContents.none { it.identifier == id }) return@launch
        taskSubmitter.submit(SystemCleanerProcessingTask(targetFilters = setOf(id)))
    }

    fun onConfirmDeleteFiles(id: FilterIdentifier, paths: Set<APath>) = launch {
        log(TAG, INFO) { "onConfirmDeleteFiles($id, ${paths.size})" }
        if (paths.isEmpty()) return@launch
        val data = systemCleaner.state.first().data ?: return@launch
        val fc = data.filterContents.firstOrNull { it.identifier == id } ?: return@launch
        val livePaths = fc.items.map { it.path }.toSet()
        val validPaths = paths intersect livePaths
        if (validPaths.isEmpty()) return@launch
        taskSubmitter.submit(
            SystemCleanerProcessingTask(
                targetFilters = setOf(id),
                targetContent = validPaths,
            ),
        )
    }

    fun onExcludeFilter(id: FilterIdentifier) = launch {
        log(TAG, INFO) { "onExcludeFilter($id)" }
        val data = systemCleaner.state.first().data ?: return@launch
        val fc = data.filterContents.firstOrNull { it.identifier == id } ?: return@launch
        val paths = fc.items.map { it.path }.toSet()
        if (paths.isEmpty()) return@launch
        val undo = systemCleaner.exclude(id, paths)
        // Skip the "0 exclusions created" snackbar when every path was already excluded (save dedups
        // to empty), matching the list VM's guard. (was BUG-FIXME-4)
        if (undo.exclusionIds.isEmpty()) return@launch
        events.tryEmit(
            Event.ExclusionsCreated(
                count = undo.exclusionIds.size,
                undo = undo,
                restoreTarget = id,
            ),
        )
    }

    fun onExcludeFiles(id: FilterIdentifier, paths: Set<APath>) = launch {
        log(TAG, INFO) { "onExcludeFiles($id, ${paths.size})" }
        if (paths.isEmpty()) return@launch
        val data = systemCleaner.state.first().data ?: return@launch
        val fc = data.filterContents.firstOrNull { it.identifier == id } ?: return@launch
        val livePaths = fc.items.map { it.path }.toSet()
        val validPaths = paths intersect livePaths
        if (validPaths.isEmpty()) return@launch
        systemCleaner.exclude(id, validPaths)
        events.tryEmit(Event.SelectionExclusionsCreated(validPaths.size))
    }

    fun onShowExclusions() {
        navTo(ExclusionsListRoute)
    }

    fun onUndoExclude(undo: SystemCleaner.ExclusionUndo, restoreTarget: FilterIdentifier) = launch {
        log(TAG, INFO) { "onUndoExclude(${undo.exclusionIds.size}, restore=$restoreTarget)" }
        currentTarget = restoreTarget
        systemCleaner.undoExclude(undo)
    }

    fun onPreviewFile(filterId: FilterIdentifier, path: APath) {
        log(TAG, INFO) { "onPreviewFile($filterId, $path)" }
        val supportsPreview = filterId == TrashedFilter::class.filterIdentifier ||
            filterId == ScreenshotsFilter::class.filterIdentifier
        if (!supportsPreview) return
        navTo(PreviewRoute(options = PreviewOptions(path)))
    }

    data class State(
        val items: List<FilterContent> = emptyList(),
        val target: FilterIdentifier? = null,
        val progress: Progress.Data? = null,
    )

    sealed interface Event {
        data class TaskResult(val result: SystemCleanerTask.Result) : Event
        data class ExclusionsCreated(
            val count: Int,
            val undo: SystemCleaner.ExclusionUndo,
            val restoreTarget: FilterIdentifier,
        ) : Event

        data class SelectionExclusionsCreated(val count: Int) : Event
    }

    companion object {
        private val TAG = logTag("SystemCleaner", "Details", "ViewModel")
    }
}
