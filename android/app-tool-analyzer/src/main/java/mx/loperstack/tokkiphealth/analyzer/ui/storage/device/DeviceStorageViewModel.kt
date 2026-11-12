package mx.loperstack.tokkiphealth.analyzer.ui.storage.device

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.analyzer.core.Analyzer
import mx.loperstack.tokkiphealth.analyzer.core.AnalyzerSettings
import mx.loperstack.tokkiphealth.analyzer.core.device.DeviceStorage
import mx.loperstack.tokkiphealth.analyzer.core.device.DeviceStorageScanTask
import mx.loperstack.tokkiphealth.analyzer.ui.StorageContentRoute
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.intervalFlow
import mx.loperstack.tokkiphealth.common.navigation.routes.UpgradeRoute
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.common.upgrade.UpgradeRepo
import mx.loperstack.tokkiphealth.setup.SetupRoute
import mx.loperstack.tokkiphealth.stats.core.SpaceHistoryRepo
import mx.loperstack.tokkiphealth.stats.core.db.SpaceSnapshotEntity
import mx.loperstack.tokkiphealth.stats.ui.SpaceHistoryRoute
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

@HiltViewModel
class DeviceStorageViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val analyzer: Analyzer,
    private val analyzerSettings: AnalyzerSettings,
    spaceHistoryRepo: SpaceHistoryRepo,
    upgradeRepo: UpgradeRepo,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    init {
        analyzer.data
            .take(1)
            .filter { it.storages.isEmpty() }
            .onEach { analyzer.submit(DeviceStorageScanTask()) }
            .launchInViewModel()
    }

    // Storage-row production excludes progress so high-frequency progress ticks during a scan don't
    // rebuild every storage Row. Progress is merged in the outer combine (below) as a cheap field
    // swap that preserves the storages List instance.
    private val storagesState = combine(
        analyzer.data,
        intervalFlow(1.hours).flatMapLatest {
            spaceHistoryRepo.getAllHistory(Instant.now() - Duration.ofDays(7))
        },
        upgradeRepo.upgradeInfo.map { it.isPro },
        analyzerSettings.hintLowSpaceDismissed.flow,
    ) { data, snapshots, isPro, hintDismissed ->
        val snapshotsByStorage = snapshots.groupBy { it.storageId }
        State(
            storages = data.storages.map { storage ->
                Row(
                    storage = storage,
                    snapshots = snapshotsByStorage[storage.id.externalId.toString()]
                        .orEmpty()
                        .sortedBy { it.recordedAt },
                    isPro = isPro,
                )
            },
            showLowSpaceHint = !isPro && !hintDismissed,
        )
    }

    val state: StateFlow<State> = combine(
        storagesState,
        analyzer.progress,
    ) { base, progress ->
        base.copy(progress = progress)
    }.safeStateIn(initialValue = State(), onError = { State() })

    fun refresh() = launch {
        log(TAG) { "refresh()" }
        analyzer.submit(DeviceStorageScanTask())
    }

    fun onStorageClick(row: Row) {
        log(TAG) { "onStorageClick(${row.storage.id})" }
        if (row.storage.setupIncomplete) {
            navTo(SetupRoute())
        } else {
            navTo(StorageContentRoute(storageId = row.storage.id))
        }
    }

    fun onTrendClick(row: Row) {
        log(TAG) { "onTrendClick(${row.storage.id}, isPro=${row.isPro})" }
        if (row.isPro) {
            navTo(SpaceHistoryRoute(storageId = row.storage.id.externalId.toString()))
        } else {
            navTo(UpgradeRoute())
        }
    }

    data class Row(
        val storage: DeviceStorage,
        val snapshots: List<SpaceSnapshotEntity> = emptyList(),
        val isPro: Boolean = false,
    )

    // No re-arm: unlike the scheduler's battery hint there is no condition that can clear and
    // legitimately raise this pitch again.
    fun dismissLowSpaceHint() = launch {
        log(TAG) { "dismissLowSpaceHint()" }
        analyzerSettings.hintLowSpaceDismissed.value(true)
    }

    fun openUpgrade() {
        log(TAG) { "openUpgrade()" }
        navTo(UpgradeRoute())
    }

    fun onHelpClick() {
        webpageTool.open(HELP_URL)
    }

    data class State(
        val storages: List<Row> = emptyList(),
        val progress: Progress.Data? = null,
        val showLowSpaceHint: Boolean = false,
    )

    companion object {
        private val TAG = logTag("Analyzer", "Storage", "ViewModel")
        private const val HELP_URL = "https://sdmse.darken.eu/storageanalyzer"
    }
}
