package mx.loperstack.tokkiphealth.stats.ui.settings

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.combine
import mx.loperstack.tokkiphealth.common.navigation.routes.UpgradeRoute
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.common.upgrade.UpgradeRepo
import mx.loperstack.tokkiphealth.stats.core.StatsRepo
import mx.loperstack.tokkiphealth.stats.core.StatsSettings
import mx.loperstack.tokkiphealth.stats.ui.ReportsRoute
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.time.Duration
import javax.inject.Inject

@HiltViewModel
class StatsSettingsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val statsRepo: StatsRepo,
    private val settings: StatsSettings,
    upgradeRepo: UpgradeRepo,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    val state: StateFlow<State> = kotlinx.coroutines.flow.combine(
        statsRepo.state,
        upgradeRepo.upgradeInfo.map { it.isPro },
        settings.retentionReports.flow,
        settings.retentionPaths.flow,
        settings.retentionSnapshots.flow,
    ) { repoState, isPro, retentionReports, retentionPaths, retentionSnapshots ->
        State(
            reportsCount = repoState.reportsCount,
            totalSpaceFreed = repoState.totalSpaceFreed,
            itemsProcessed = repoState.itemsProcessed,
            retentionReports = retentionReports,
            retentionPaths = retentionPaths,
            retentionSnapshots = retentionSnapshots,
            isPro = isPro,
        )
    }.safeStateIn(
        initialValue = State(),
        onError = { State() },
    )

    fun onViewStatsClick() {
        if (state.value.isPro) {
            navTo(ReportsRoute)
        } else {
            navTo(UpgradeRoute(forced = true))
        }
    }

    fun setRetentionReports(age: Duration) = launch { settings.retentionReports.value(age) }
    fun resetRetentionReports() = launch { settings.retentionReports.value(StatsSettings.DEFAULT_RETENTION_REPORTS) }

    fun setRetentionPaths(age: Duration) = launch { settings.retentionPaths.value(age) }
    fun resetRetentionPaths() = launch { settings.retentionPaths.value(StatsSettings.DEFAULT_RETENTION_PATHS) }

    fun setRetentionSnapshots(age: Duration) = launch { settings.retentionSnapshots.value(age) }
    fun resetRetentionSnapshots() = launch {
        settings.retentionSnapshots.value(StatsSettings.DEFAULT_RETENTION_SNAPSHOTS)
    }

    fun resetAll() = launch {
        log(TAG) { "resetAll()" }
        statsRepo.resetAll()
    }

    data class State(
        val reportsCount: Int = 0,
        val totalSpaceFreed: Long = 0L,
        val itemsProcessed: Long = 0L,
        val retentionReports: Duration = StatsSettings.DEFAULT_RETENTION_REPORTS,
        val retentionPaths: Duration = StatsSettings.DEFAULT_RETENTION_PATHS,
        val retentionSnapshots: Duration = StatsSettings.DEFAULT_RETENTION_SNAPSHOTS,
        val isPro: Boolean = false,
    )

    companion object {
        private val TAG = logTag("Settings", "Stats", "ViewModel")
    }
}
