package mx.loperstack.tokkiphealth.systemcleaner.ui.settings

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.access.AccessState
import mx.loperstack.tokkiphealth.common.compose.settings.FeatureGateState
import mx.loperstack.tokkiphealth.common.compose.settings.privilegedGateState
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.combine
import mx.loperstack.tokkiphealth.common.navigation.routes.CustomFilterListRoute
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.common.upgrade.UpgradeRepo
import mx.loperstack.tokkiphealth.setup.SetupModule
import mx.loperstack.tokkiphealth.setup.SetupRoute
import mx.loperstack.tokkiphealth.setup.SetupScreenOptions
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleaner
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleanerSettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.time.Duration
import javax.inject.Inject

@HiltViewModel
class SystemCleanerSettingsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    upgradeRepo: UpgradeRepo,
    systemCleaner: SystemCleaner,
    rootManager: RootManager,
    private val settings: SystemCleanerSettings,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    private data class GenericFilters(
        val logFiles: Boolean,
        val advertisements: Boolean,
        val emptyDirectories: Boolean,
        val superfluosApks: Boolean,
        val superfluosApksIncludeSameVersion: Boolean,
        val trashed: Boolean,
        val screenshots: Boolean,
        val lostDir: Boolean,
        val linuxFiles: Boolean,
        val macFiles: Boolean,
        val windowsFiles: Boolean,
        val tempFiles: Boolean,
        val thumbnails: Boolean,
        val analytics: Boolean,
    )

    private data class SpecificFilters(
        val anr: Boolean,
        val localTmp: Boolean,
        val downloadCache: Boolean,
        val dataLogger: Boolean,
        val logDropbox: Boolean,
        val recentTasks: Boolean,
        val tombstones: Boolean,
        val usageStats: Boolean,
        val packageCache: Boolean,
    )

    private val genericFilterFlow = combine(
        settings.filterLogFilesEnabled.flow,
        settings.filterAdvertisementsEnabled.flow,
        settings.filterEmptyDirectoriesEnabled.flow,
        settings.filterSuperfluosApksEnabled.flow,
        settings.filterSuperfluosApksIncludeSameVersion.flow,
        settings.filterTrashedEnabled.flow,
        settings.filterScreenshotsEnabled.flow,
        settings.filterLostDirEnabled.flow,
        settings.filterLinuxFilesEnabled.flow,
        settings.filterMacFilesEnabled.flow,
        settings.filterWindowsFilesEnabled.flow,
        settings.filterTempFilesEnabled.flow,
        settings.filterThumbnailsEnabled.flow,
        settings.filterAnalyticsEnabled.flow,
    ) { logs, ads, empty, superApks, superApksSame, trashed, screenshots, lostDir, linux, mac, windows, temp, thumbnails, analytics ->
        GenericFilters(logs, ads, empty, superApks, superApksSame, trashed, screenshots, lostDir, linux, mac, windows, temp, thumbnails, analytics)
    }

    private val specificFilterFlow = combine(
        settings.filterAnrEnabled.flow,
        settings.filterLocalTmpEnabled.flow,
        settings.filterDownloadCacheEnabled.flow,
        settings.filterDataLoggerEnabled.flow,
        settings.filterLogDropboxEnabled.flow,
        settings.filterRecentTasksEnabled.flow,
        settings.filterTombstonesEnabled.flow,
        settings.filterUsageStatsEnabled.flow,
        settings.filterPackageCacheEnabled.flow,
    ) { anr, localTmp, downloadCache, dataLogger, logDropbox, recentTasks, tombstones, usageStats, packageCache ->
        SpecificFilters(anr, localTmp, downloadCache, dataLogger, logDropbox, recentTasks, tombstones, usageStats, packageCache)
    }

    val state: StateFlow<State> = combine(
        systemCleaner.state,
        upgradeRepo.upgradeInfo.map { it.isPro },
        settings.filterScreenshotsAge.flow,
        genericFilterFlow,
        specificFilterFlow,
        rootManager.accessState,
    ) { cleanerState, isPro, screenshotsAge, generic, specific, rootAccess ->
        State(
            isPro = isPro,
            areSystemFilterAvailable = cleanerState.areSystemFilterAvailable,
            rootAccess = rootAccess,
            systemFilterGate = privilegedGateState(cleanerState.areSystemFilterAvailable, listOf(rootAccess)),
            screenshotsAge = screenshotsAge,
            filterLogFiles = generic.logFiles,
            filterAdvertisements = generic.advertisements,
            filterEmptyDirectories = generic.emptyDirectories,
            filterSuperfluosApks = generic.superfluosApks,
            filterSuperfluosApksIncludeSameVersion = generic.superfluosApksIncludeSameVersion,
            filterTrashed = generic.trashed,
            filterScreenshots = generic.screenshots,
            filterLostDir = generic.lostDir,
            filterLinuxFiles = generic.linuxFiles,
            filterMacFiles = generic.macFiles,
            filterWindowsFiles = generic.windowsFiles,
            filterTempFiles = generic.tempFiles,
            filterThumbnails = generic.thumbnails,
            filterAnalytics = generic.analytics,
            filterAnr = specific.anr,
            filterLocalTmp = specific.localTmp,
            filterDownloadCache = specific.downloadCache,
            filterDataLogger = specific.dataLogger,
            filterLogDropbox = specific.logDropbox,
            filterRecentTasks = specific.recentTasks,
            filterTombstones = specific.tombstones,
            filterUsageStats = specific.usageStats,
            filterPackageCache = specific.packageCache,
        )
    }.safeStateIn(
        initialValue = State(),
        onError = { State() },
    )

    fun setFilterLogFiles(value: Boolean) = launch { settings.filterLogFilesEnabled.value(value) }
    fun setFilterAdvertisements(value: Boolean) = launch { settings.filterAdvertisementsEnabled.value(value) }
    fun setFilterEmptyDirectories(value: Boolean) = launch { settings.filterEmptyDirectoriesEnabled.value(value) }
    fun setFilterSuperfluosApks(value: Boolean) = launch { settings.filterSuperfluosApksEnabled.value(value) }
    fun setFilterSuperfluosApksIncludeSameVersion(value: Boolean) = launch {
        settings.filterSuperfluosApksIncludeSameVersion.value(value)
    }
    fun setFilterTrashed(value: Boolean) = launch { settings.filterTrashedEnabled.value(value) }
    fun setFilterScreenshots(value: Boolean) = launch { settings.filterScreenshotsEnabled.value(value) }
    fun setFilterScreenshotsAge(value: Duration) = launch { settings.filterScreenshotsAge.value(value) }
    fun resetFilterScreenshotsAge() = launch {
        settings.filterScreenshotsAge.value(SystemCleanerSettings.SCREENSHOTS_AGE_DEFAULT)
    }
    fun setFilterLostDir(value: Boolean) = launch { settings.filterLostDirEnabled.value(value) }
    fun setFilterLinuxFiles(value: Boolean) = launch { settings.filterLinuxFilesEnabled.value(value) }
    fun setFilterMacFiles(value: Boolean) = launch { settings.filterMacFilesEnabled.value(value) }
    fun setFilterWindowsFiles(value: Boolean) = launch { settings.filterWindowsFilesEnabled.value(value) }
    fun setFilterTempFiles(value: Boolean) = launch { settings.filterTempFilesEnabled.value(value) }
    fun setFilterThumbnails(value: Boolean) = launch { settings.filterThumbnailsEnabled.value(value) }
    fun setFilterAnalytics(value: Boolean) = launch { settings.filterAnalyticsEnabled.value(value) }

    fun setFilterAnr(value: Boolean) = launch { settings.filterAnrEnabled.value(value) }
    fun setFilterLocalTmp(value: Boolean) = launch { settings.filterLocalTmpEnabled.value(value) }
    fun setFilterDownloadCache(value: Boolean) = launch { settings.filterDownloadCacheEnabled.value(value) }
    fun setFilterDataLogger(value: Boolean) = launch { settings.filterDataLoggerEnabled.value(value) }
    fun setFilterLogDropbox(value: Boolean) = launch { settings.filterLogDropboxEnabled.value(value) }
    fun setFilterRecentTasks(value: Boolean) = launch { settings.filterRecentTasksEnabled.value(value) }
    fun setFilterTombstones(value: Boolean) = launch { settings.filterTombstonesEnabled.value(value) }
    fun setFilterUsageStats(value: Boolean) = launch { settings.filterUsageStatsEnabled.value(value) }
    fun setFilterPackageCache(value: Boolean) = launch { settings.filterPackageCacheEnabled.value(value) }

    fun onCustomFiltersClick() {
        navTo(CustomFilterListRoute)
    }

    fun onRootFilterBadgeClick() = navTo(
        SetupRoute(
            options = SetupScreenOptions(
                showCompleted = true,
                typeFilter = setOf(SetupModule.Type.ROOT),
            ),
        ),
    )

    fun onHelpClick() {
        webpageTool.open(HELP_URL)
    }

    data class State(
        val isPro: Boolean = false,
        val areSystemFilterAvailable: Boolean = false,
        val rootAccess: AccessState = AccessState.Undecided,
        val systemFilterGate: FeatureGateState = FeatureGateState.SETUP,
        val screenshotsAge: Duration = SystemCleanerSettings.SCREENSHOTS_AGE_DEFAULT,
        val filterLogFiles: Boolean = true,
        val filterAdvertisements: Boolean = true,
        val filterEmptyDirectories: Boolean = true,
        val filterSuperfluosApks: Boolean = false,
        val filterSuperfluosApksIncludeSameVersion: Boolean = true,
        val filterTrashed: Boolean = false,
        val filterScreenshots: Boolean = false,
        val filterLostDir: Boolean = true,
        val filterLinuxFiles: Boolean = true,
        val filterMacFiles: Boolean = true,
        val filterWindowsFiles: Boolean = true,
        val filterTempFiles: Boolean = true,
        val filterThumbnails: Boolean = false,
        val filterAnalytics: Boolean = true,
        val filterAnr: Boolean = true,
        val filterLocalTmp: Boolean = false,
        val filterDownloadCache: Boolean = true,
        val filterDataLogger: Boolean = true,
        val filterLogDropbox: Boolean = true,
        val filterRecentTasks: Boolean = false,
        val filterTombstones: Boolean = false,
        val filterUsageStats: Boolean = false,
        val filterPackageCache: Boolean = false,
    )

    companion object {
        private val TAG = logTag("Settings", "SystemCleaner", "ViewModel")
        private const val HELP_URL = "https://sdmse.darken.eu/systemcleaner#settings"
    }
}
