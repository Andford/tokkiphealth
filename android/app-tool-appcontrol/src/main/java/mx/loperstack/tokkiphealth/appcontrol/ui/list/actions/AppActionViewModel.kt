package mx.loperstack.tokkiphealth.appcontrol.ui.list.actions

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.appcontrol.core.AppControl
import mx.loperstack.tokkiphealth.appcontrol.core.AppInfo
import mx.loperstack.tokkiphealth.appcontrol.core.archive.ArchiveException
import mx.loperstack.tokkiphealth.appcontrol.core.archive.ArchiveTask
import mx.loperstack.tokkiphealth.appcontrol.core.archive.ArchiveUnavailableException
import mx.loperstack.tokkiphealth.appcontrol.core.createGooglePlayIntent
import mx.loperstack.tokkiphealth.appcontrol.core.createSystemSettingsIntent
import mx.loperstack.tokkiphealth.appcontrol.core.export.AppExportTask
import mx.loperstack.tokkiphealth.appcontrol.core.forcestop.ForceStopTask
import mx.loperstack.tokkiphealth.appcontrol.core.restore.RestoreException
import mx.loperstack.tokkiphealth.appcontrol.core.restore.RestoreTask
import mx.loperstack.tokkiphealth.appcontrol.core.restore.RestoreUnavailableException
import mx.loperstack.tokkiphealth.appcontrol.core.toggle.AppControlToggleTask
import mx.loperstack.tokkiphealth.appcontrol.core.uninstall.UninstallException
import mx.loperstack.tokkiphealth.appcontrol.core.uninstall.UninstallTask
import mx.loperstack.tokkiphealth.appcontrol.ui.list.actions.items.AppActionItem
import mx.loperstack.tokkiphealth.appcontrol.ui.list.actions.items.AppActionItemContext
import mx.loperstack.tokkiphealth.appcontrol.ui.list.actions.items.buildAppActionItems
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.SingleEventFlow
import mx.loperstack.tokkiphealth.common.hasApiLevel
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallDetails
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.pkgs.getLaunchIntent
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.common.user.UserManager2
import mx.loperstack.tokkiphealth.exclusion.core.ExclusionManager
import mx.loperstack.tokkiphealth.exclusion.core.current
import mx.loperstack.tokkiphealth.exclusion.core.save
import mx.loperstack.tokkiphealth.exclusion.core.types.Exclusion
import mx.loperstack.tokkiphealth.exclusion.core.types.PkgExclusion
import mx.loperstack.tokkiphealth.exclusion.ui.PkgExclusionEditorRoute
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskSubmitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AppActionViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val appControl: AppControl,
    private val taskManager: TaskSubmitter,
    private val exclusionManager: ExclusionManager,
    private val userManager2: UserManager2,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    // Bound by the host via [setInstallId] (Picker-style entry-arg binding).
    // SavedStateHandle.toRoute<>() is unreliable for non-nullable serializable args under Nav3 —
    // see project memory `feedback_nav3_route_binding`.
    private val installIdFlow = MutableStateFlow<InstallId?>(null)

    fun setInstallId(installId: InstallId) {
        if (installIdFlow.value == installId) return
        installIdFlow.value = installId
    }

    init {
        installIdFlow.filterNotNull()
            .flatMapLatest { id ->
                appControl.state
                    // Wait for data to be loaded before deciding the app is gone — otherwise the
                    // transient null state during a scan refresh would dismiss the sheet.
                    .mapNotNull { it.data }
                    .distinctUntilChanged()
                    .map { data -> data.apps.firstOrNull { app -> app.installId == id } to id }
            }
            .filter { (app, _) -> app == null }
            .take(1)
            .onEach { (_, id) ->
                log(TAG) { "App data for $id is no longer available, dismissing sheet" }
                navUp()
            }
            .launchIn(vmScope)
    }

    val events = SingleEventFlow<Event>()

    // Item production excludes progress so high-frequency progress ticks (which re-emit the tool
    // state with the same data instance) don't re-scan the app list and rebuild the action items
    // while the sheet is open. Progress is merged in last (below) as a cheap field swap.
    private val baseState = combine(
        installIdFlow,
        appControl.state.distinctUntilChangedBy { it.copy(progress = null) },
        exclusionManager.exclusions,
    ) { installId, acState, _ ->
        if (installId == null) return@combine State()
        val appInfo = acState.data?.apps?.firstOrNull { it.installId == installId }
            ?: return@combine State()

        val isCurrentUser = appInfo.installId.userHandle == userManager2.currentUser().handle
        val launchAvailable = appInfo.pkg.id.getLaunchIntent(context) != null
        val appStoreAvailable = (appInfo.pkg as? InstallDetails)?.installerInfo?.installer != null
        val existingExclusion = exclusionManager
            .current()
            .filterIsInstance<Exclusion.Pkg>()
            .firstOrNull { it.match(appInfo.id) }

        val ctx = AppActionItemContext(
            isCurrentUser = isCurrentUser,
            launchAvailable = launchAvailable,
            appStoreAvailable = appStoreAvailable,
            canForceStop = acState.canForceStop,
            canArchive = acState.canArchive,
            canRestore = acState.canRestore,
            canToggle = acState.canToggle,
            existingExclusionId = existingExclusion?.id,
        )
        val items = buildAppActionItems(appInfo, ctx)
        State(appInfo = appInfo, items = items)
    }

    val state: StateFlow<State> = combine(
        baseState,
        appControl.progress,
    ) { base, progress ->
        base.copy(progress = progress)
    }.safeStateIn(
        initialValue = State(),
        onError = { State() },
    )

    private suspend fun currentAppInfoOrNull(): AppInfo? {
        val id = installIdFlow.value ?: return null
        return appControl.state.first().data?.apps?.firstOrNull { it.installId == id }
    }

    fun onActionTapped(item: AppActionItem) = launch {
        log(TAG, INFO) { "onActionTapped($item)" }
        val id = installIdFlow.value ?: return@launch
        when (item) {
            is AppActionItem.Info.Size -> openStorageSettings()
            is AppActionItem.Info.Usage -> openUsageScreen()
            is AppActionItem.Action.Launch -> launchApp()
            is AppActionItem.Action.SystemSettings -> openSystemSettings()
            is AppActionItem.Action.AppStore -> openAppStore()
            is AppActionItem.Action.ForceStop -> submitForceStop()
            is AppActionItem.Action.Toggle -> submitToggle()
            is AppActionItem.Action.Exclude -> handleExclude(item)
            is AppActionItem.Action.Uninstall -> events.emit(Event.ConfirmUninstall(id))
            is AppActionItem.Action.Archive -> events.emit(Event.ConfirmArchive(id))
            is AppActionItem.Action.Restore -> events.emit(Event.ConfirmRestore(id))
            is AppActionItem.Action.Export -> requestExportPath()
        }
    }

    fun onUninstallConfirmed() = launch {
        val appInfo = currentAppInfoOrNull() ?: return@launch
        log(TAG, INFO) { "onUninstallConfirmed(${appInfo.installId})" }
        val task = UninstallTask(setOf(appInfo.installId))
        val result = taskManager.submit(task) as UninstallTask.Result
        if (result.failed.isNotEmpty()) {
            throw UninstallException(installId = result.failed.first())
        }
        events.emit(Event.ShowResult(result))
    }

    fun onArchiveConfirmed() = launch {
        val appInfo = currentAppInfoOrNull() ?: return@launch
        log(TAG, INFO) { "onArchiveConfirmed(${appInfo.installId})" }
        val task = ArchiveTask(setOf(appInfo.installId))
        val result = taskManager.submit(task) as ArchiveTask.Result
        if (result.failed.isNotEmpty()) {
            val id = result.failed.first()
            if (id in result.unavailable) {
                throw ArchiveUnavailableException("Archive button is disabled for $id")
            }
            throw ArchiveException(installId = id)
        }
        events.emit(Event.ShowResult(result))
    }

    fun onRestoreConfirmed() = launch {
        val appInfo = currentAppInfoOrNull() ?: return@launch
        log(TAG, INFO) { "onRestoreConfirmed(${appInfo.installId})" }
        val task = RestoreTask(setOf(appInfo.installId))
        val result = taskManager.submit(task) as RestoreTask.Result
        if (result.failed.isNotEmpty()) {
            val id = result.failed.first()
            if (id in result.unavailable) {
                throw RestoreUnavailableException("Restore button is disabled for $id")
            }
            throw RestoreException(installId = id)
        }
        events.emit(Event.ShowResult(result))
    }

    fun onExportPathPicked(uri: Uri?) = launch {
        if (uri == null) {
            log(TAG, WARN) { "Export failed, no path picked" }
            return@launch
        }
        val appInfo = currentAppInfoOrNull() ?: return@launch
        log(TAG, INFO) { "onExportPathPicked($uri)" }
        val task = AppExportTask(targets = setOf(appInfo.installId), savePath = uri)
        val result = taskManager.submit(task) as AppExportTask.Result
        events.emit(Event.ShowResult(result))
    }

    private suspend fun submitToggle() {
        val appInfo = currentAppInfoOrNull() ?: return
        val task = AppControlToggleTask(setOf(appInfo.installId))
        val result = taskManager.submit(task) as AppControlToggleTask.Result
        events.emit(Event.ShowResult(result))
    }

    private suspend fun submitForceStop() {
        val appInfo = currentAppInfoOrNull() ?: return
        val task = ForceStopTask(setOf(appInfo.installId), allowOffLimits = true)
        val result = taskManager.submit(task) as ForceStopTask.Result
        events.emit(Event.ShowResult(result))
    }

    private suspend fun handleExclude(item: AppActionItem.Action.Exclude) {
        if (item.existingExclusionId != null) {
            navTo(PkgExclusionEditorRoute(exclusionId = item.existingExclusionId))
            return
        }
        val appInfo = currentAppInfoOrNull() ?: return
        val newExcl = PkgExclusion(pkgId = appInfo.id)
        exclusionManager.save(newExcl)
    }

    private suspend fun launchApp() {
        val appInfo = currentAppInfoOrNull() ?: return
        val intent = appInfo.pkg.id.getLaunchIntent(context)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ?: return
        runCatching { context.startActivity(intent) }
            .onFailure { log(TAG, WARN) { "Launch failed: ${it.asLog()}" }; errorEvents.emit(it) }
    }

    private suspend fun openSystemSettings() {
        val appInfo = currentAppInfoOrNull() ?: return
        val intent = appInfo.createSystemSettingsIntent(context)
        runCatching { context.startActivity(intent) }
            .onFailure { log(TAG, ERROR) { "System settings intent failed: ${it.asLog()}" }; errorEvents.emit(it) }
    }

    private suspend fun openAppStore() {
        val appInfo = currentAppInfoOrNull() ?: return
        val intent = appInfo.createGooglePlayIntent(context)
        runCatching { context.startActivity(intent) }
            .onFailure { log(TAG, WARN) { "AppStore intent failed: ${it.asLog()}" }; errorEvents.emit(it) }
    }

    private fun openStorageSettings() {
        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        runCatching { context.startActivity(intent) }
            .onFailure { errorEvents.emitBlocking(it) }
    }

    private fun openUsageScreen() = launch {
        val appInfo = currentAppInfoOrNull() ?: return@launch
        val usageIntent = if (hasApiLevel(29)) {
            Intent(Settings.ACTION_APP_USAGE_SETTINGS).apply {
                putExtra(Intent.EXTRA_PACKAGE_NAME, appInfo.pkg.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            null
        }
        try {
            if (usageIntent == null) throw ActivityNotFoundException()
            context.startActivity(usageIntent)
        } catch (_: ActivityNotFoundException) {
            // No per-app usage screen available; fall back to per-app system settings.
            val fallback = appInfo.createSystemSettingsIntent(context)
            runCatching { context.startActivity(fallback) }
                .onFailure { errorEvents.emit(it) }
        }
    }

    private fun requestExportPath() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        events.tryEmit(Event.SelectExportPath(intent))
    }

    data class State(
        val appInfo: AppInfo? = null,
        val progress: Progress.Data? = null,
        val items: List<AppActionItem>? = null,
    )

    sealed interface Event {
        data class ShowResult(val result: mx.loperstack.tokkiphealth.appcontrol.core.AppControlTask.Result) : Event
        data class SelectExportPath(val intent: Intent) : Event
        data class ConfirmUninstall(val installId: InstallId) : Event
        data class ConfirmArchive(val installId: InstallId) : Event
        data class ConfirmRestore(val installId: InstallId) : Event
    }

    companion object {
        private val TAG = logTag("AppControl", "Action", "ViewModel")
    }
}
