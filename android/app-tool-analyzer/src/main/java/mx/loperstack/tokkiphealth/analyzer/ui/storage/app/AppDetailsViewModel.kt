package mx.loperstack.tokkiphealth.analyzer.ui.storage.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.analyzer.core.Analyzer
import mx.loperstack.tokkiphealth.analyzer.core.content.ContentGroup
import mx.loperstack.tokkiphealth.analyzer.core.device.DeviceStorage
import mx.loperstack.tokkiphealth.analyzer.core.storage.AppDeepScanTask
import mx.loperstack.tokkiphealth.analyzer.core.storage.categories.AppCategory
import mx.loperstack.tokkiphealth.analyzer.ui.AppDetailsRoute
import mx.loperstack.tokkiphealth.analyzer.ui.ContentRoute
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.pkgs.getSettingsIntent
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AppDetailsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val analyzer: Analyzer,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    private val routeFlow = MutableStateFlow<AppDetailsRoute?>(null)

    fun bindRoute(route: AppDetailsRoute) {
        if (routeFlow.value != null) return
        log(TAG, INFO) { "bindRoute(${route.installId} on ${route.storageId})" }
        routeFlow.value = route
    }

    init {
        // Process-death handler.
        routeFlow
            .filterNotNull()
            .flatMapLatest { route ->
                analyzer.data
                    .filter { it.findPkg(route) == null }
                    .take(1)
                    .onEach {
                        log(TAG, WARN) { "Can't find ${route.installId} on ${route.storageId}" }
                        navUp()
                    }
            }
            .launchInViewModel()

        // Auto deep scan when stats are shallow.
        routeFlow
            .filterNotNull()
            .flatMapLatest { route ->
                analyzer.data
                    .mapNotNull { it.findPkg(route) }
                    .take(1)
                    .filter { it.isShallow }
                    .onEach {
                        log(TAG) { "Current stats are shallow, initiating deep scan" }
                        analyzer.submit(AppDeepScanTask(route.storageId, route.installId))
                    }
            }
            .launchInViewModel()
    }

    private fun Analyzer.Data.findPkg(route: AppDetailsRoute): AppCategory.PkgStat? {
        val appContent = categories[route.storageId]?.filterIsInstance<AppCategory>()?.singleOrNull()
        return appContent?.pkgStats?.get(route.installId)
    }

    fun refresh() = launch {
        val route = routeFlow.value ?: return@launch
        log(TAG) { "refresh()" }
        analyzer.submit(AppDeepScanTask(route.storageId, route.installId))
    }

    val state: StateFlow<State> = routeFlow
        .filterNotNull()
        .flatMapLatest { route ->
            // Detail production excludes progress so high-frequency progress ticks during a (deep)
            // scan don't rebuild the whole Ready state + size-group rows. Progress is merged in the
            // outer combine (below) as a cheap field swap.
            val content = analyzer.data.map { data ->
                val pkgStat = data.findPkg(route)
                val storage = data.storages.firstOrNull { it.id == route.storageId }
                if (pkgStat == null || storage == null) {
                    State.NotFound
                } else {
                    State.Ready(
                        storage = storage,
                        pkgStat = pkgStat,
                        appCode = pkgStat.appCode,
                        appData = pkgStat.appData,
                        appMedia = pkgStat.appMedia,
                        extraData = pkgStat.extraData,
                        progress = null,
                    )
                }
            }

            combine(content, analyzer.progress) { base, progress ->
                if (base is State.Ready) base.copy(progress = progress) else base
            }
        }
        .safeStateIn(initialValue = State.Loading, onError = { State.NotFound })

    fun onSettingsClick(pkgStat: AppCategory.PkgStat) = launch {
        val intent = pkgStat.pkg.getSettingsIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            log(TAG, ERROR) { "Launching system settings intent failed: ${e.asLog()}" }
            errorEvents.emit(e)
        }
    }

    fun onGroupClick(group: ContentGroup, pkgStat: AppCategory.PkgStat) {
        val route = routeFlow.value ?: return
        navTo(
            ContentRoute(
                storageId = route.storageId,
                groupId = group.id,
                installId = pkgStat.id,
            ),
        )
    }

    sealed interface State {
        data object Loading : State
        data class Ready(
            val storage: DeviceStorage,
            val pkgStat: AppCategory.PkgStat,
            val appCode: ContentGroup?,
            val appData: ContentGroup?,
            val appMedia: ContentGroup?,
            val extraData: ContentGroup?,
            val progress: Progress.Data?,
        ) : State
        data object NotFound : State
    }

    companion object {
        private val TAG = logTag("Analyzer", "App", "Details", "ViewModel")
    }
}
