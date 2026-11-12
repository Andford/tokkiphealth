package mx.loperstack.tokkiphealth.squeezer.ui.list

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.compose.snackbar.ToolListEvent
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.SingleEventFlow
import mx.loperstack.tokkiphealth.common.navigation.routes.UpgradeRoute
import mx.loperstack.tokkiphealth.common.previews.PreviewOptions
import mx.loperstack.tokkiphealth.common.previews.PreviewRoute
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.ui.LayoutMode
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.common.upgrade.UpgradeRepo
import mx.loperstack.tokkiphealth.common.upgrade.isProForUi
import mx.loperstack.tokkiphealth.exclusion.ui.ExclusionsListRoute
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskSubmitter
import mx.loperstack.tokkiphealth.squeezer.core.CompressibleMedia
import mx.loperstack.tokkiphealth.squeezer.core.Squeezer
import mx.loperstack.tokkiphealth.squeezer.core.SqueezerSettings
import mx.loperstack.tokkiphealth.squeezer.core.hasData
import mx.loperstack.tokkiphealth.squeezer.core.tasks.SqueezerProcessTask
import mx.loperstack.tokkiphealth.squeezer.core.tasks.SqueezerTask
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@HiltViewModel
class SqueezerListViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val squeezer: Squeezer,
    private val settings: SqueezerSettings,
    private val taskSubmitter: TaskSubmitter,
    private val upgradeRepo: UpgradeRepo,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    init {
        squeezer.state
            .map { it.data }
            .drop(1)
            .filterNotNull()
            .filter { !it.hasData }
            .take(1)
            .onEach {
                log(TAG, INFO) { "Data drained, popping back stack" }
                navUp()
            }
            .launchIn(vmScope)
    }

    val events = SingleEventFlow<Event>()

    // Media production excludes progress/layout/quality so high-frequency progress ticks during
    // compression don't re-sort the whole media list. Those fields are merged in last (below) as a
    // cheap field swap that preserves the media List instance, letting keyed lazy rows skip.
    private val mediaState = squeezer.state
        .map { it.data }
        .distinctUntilChanged()
        .map { data -> State(media = data?.media?.sortedByDescending { it.size }) }

    val state: StateFlow<State> = combine(
        mediaState,
        squeezer.progress,
        settings.layoutMode.flow,
        settings.compressionQuality.flow,
    ) { base, progress, layoutMode, quality ->
        base.copy(progress = progress, layoutMode = layoutMode, quality = quality)
    }.safeStateIn(
        initialValue = State(),
        onError = { State() },
    )

    data class State(
        val media: List<CompressibleMedia>? = null,
        val progress: Progress.Data? = null,
        val layoutMode: LayoutMode = LayoutMode.LINEAR,
        val quality: Int = SqueezerSettings.DEFAULT_QUALITY,
    )

    sealed interface Event {
        data class ConfirmCompression(
            val items: List<CompressibleMedia>,
            val quality: Int,
        ) : Event

        data class ExclusionsCreated(override val count: Int) : Event, ToolListEvent.ShowExclusionsCreated

        data class TaskResult(override val result: SqueezerTask.Result) : Event, ToolListEvent.ShowTaskResult
    }

    fun compressAll() = launch {
        log(TAG, INFO) { "compressAll()" }
        val media = state.value.media ?: return@launch
        if (media.isEmpty()) return@launch
        compress(media.map { it.identifier }.toSet())
    }

    fun compress(
        ids: Set<CompressibleMedia.Id>,
        confirmed: Boolean = false,
        qualityOverride: Int? = null,
    ) = launch {
        log(TAG, INFO) { "compress(): ${ids.size} confirmed=$confirmed" }

        val current = state.value.media ?: return@launch
        val items = current.filter { it.identifier in ids }
        if (items.isEmpty()) return@launch

        val quality = qualityOverride ?: settings.compressionQuality.value()

        if (!confirmed) {
            events.tryEmit(Event.ConfirmCompression(items, quality))
            return@launch
        }

        if (!upgradeRepo.isProForUi()) {
            navTo(UpgradeRoute())
            return@launch
        }

        val mode: SqueezerProcessTask.TargetMode = SqueezerProcessTask.TargetMode.Selected(
            targets = ids,
        )

        val result = taskSubmitter.submit(
            SqueezerProcessTask(mode = mode, qualityOverride = qualityOverride),
        ) as SqueezerTask.Result
        events.tryEmit(Event.TaskResult(result))
    }

    fun exclude(ids: Set<CompressibleMedia.Id>) = launch {
        log(TAG, INFO) { "exclude(): ${ids.size}" }
        // Use the saved-exclusion count rather than the requested-ids count so the snackbar
        // matches what was actually persisted. ExclusionManager.save() drops duplicates that
        // are already excluded, and `Squeezer.exclude` itself drops ids no longer in the
        // current data snapshot.
        val savedIds = squeezer.exclude(ids)
        events.tryEmit(Event.ExclusionsCreated(savedIds.size))
    }

    fun toggleLayoutMode() = launch {
        log(TAG) { "toggleLayoutMode()" }
        when (settings.layoutMode.value()) {
            LayoutMode.LINEAR -> settings.layoutMode.value(LayoutMode.GRID)
            LayoutMode.GRID -> settings.layoutMode.value(LayoutMode.LINEAR)
        }
    }

    fun openPreview(media: CompressibleMedia) {
        log(TAG) { "openPreview(${media.identifier})" }
        navTo(PreviewRoute(options = PreviewOptions(paths = listOf(media.path))))
    }

    fun openExclusionsList() {
        log(TAG) { "openExclusionsList()" }
        navTo(ExclusionsListRoute)
    }

    companion object {
        private val TAG = logTag("Squeezer", "List", "ViewModel")
    }
}
