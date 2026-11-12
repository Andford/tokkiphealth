package mx.loperstack.tokkiphealth.main.ui.dashboard

import mx.loperstack.tokkiphealth.appcleaner.core.AppCleaner
import mx.loperstack.tokkiphealth.appcleaner.core.hasData
import mx.loperstack.tokkiphealth.appcleaner.core.tasks.AppCleanerProcessingTask
import mx.loperstack.tokkiphealth.appcleaner.core.tasks.AppCleanerScanTask
import mx.loperstack.tokkiphealth.appcleaner.ui.AppJunkDetailsRoute
import mx.loperstack.tokkiphealth.analyzer.core.Analyzer
import mx.loperstack.tokkiphealth.appcontrol.core.AppControl
import mx.loperstack.tokkiphealth.common.SdmSeLinks
import mx.loperstack.tokkiphealth.common.flow.intervalFlow
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.navigation.routes.AppControlListRoute
import mx.loperstack.tokkiphealth.common.navigation.routes.DeviceStorageRoute
import mx.loperstack.tokkiphealth.common.navigation.routes.UpgradeRoute
import mx.loperstack.tokkiphealth.common.upgrade.isPro
import mx.loperstack.tokkiphealth.corpsefinder.core.CorpseFinder
import mx.loperstack.tokkiphealth.corpsefinder.core.hasData
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderDeleteTask
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderScanTask
import mx.loperstack.tokkiphealth.corpsefinder.ui.CorpseDetailsRoute
import mx.loperstack.tokkiphealth.deduplicator.core.Deduplicator
import mx.loperstack.tokkiphealth.deduplicator.core.hasData
import mx.loperstack.tokkiphealth.deduplicator.core.tasks.DeduplicatorDeleteTask
import mx.loperstack.tokkiphealth.deduplicator.core.tasks.DeduplicatorScanTask
import mx.loperstack.tokkiphealth.deduplicator.ui.DeduplicatorDetailsRoute
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.main.core.taskmanager.getLatestResult
import mx.loperstack.tokkiphealth.main.core.taskmanager.isBusy
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.AnalyzerDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.AppControlDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.MotdDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.SetupDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.SqueezerDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.StatsDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.TitleDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.ToolDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.UpdateDashboardCardItem
import mx.loperstack.tokkiphealth.setup.SetupRoute
import mx.loperstack.tokkiphealth.squeezer.core.Squeezer
import mx.loperstack.tokkiphealth.squeezer.core.tasks.SqueezerProcessTask
import mx.loperstack.tokkiphealth.squeezer.ui.SqueezerSetupRoute
import mx.loperstack.tokkiphealth.stats.core.LowStorage
import mx.loperstack.tokkiphealth.stats.core.SpaceTracker
import mx.loperstack.tokkiphealth.stats.core.db.SpaceSnapshotEntity
import mx.loperstack.tokkiphealth.stats.core.forecast.StorageForecaster
import mx.loperstack.tokkiphealth.stats.core.forecast.StorageTrendCalculator
import mx.loperstack.tokkiphealth.stats.ui.ReportsRoute
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleaner
import mx.loperstack.tokkiphealth.systemcleaner.core.hasData
import mx.loperstack.tokkiphealth.systemcleaner.core.tasks.SystemCleanerProcessingTask
import mx.loperstack.tokkiphealth.systemcleaner.core.tasks.SystemCleanerScanTask
import mx.loperstack.tokkiphealth.systemcleaner.ui.FilterContentDetailsRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private val TAG = logTag("Dashboard", "ViewModel")

internal fun DashboardViewModel.buildUpdateInfo(): Flow<UpdateDashboardCardItem?> = updateService.availableUpdate
    .map { update ->
        if (update == null) {
            log(TAG, INFO) { "No update available" }
            return@map null
        }

        try {
            return@map UpdateDashboardCardItem(
                update = update,
                onDismiss = {
                    launch {
                        updateService.dismissUpdate(update)
                        updateService.refresh()
                    }
                },
                onViewUpdate = {
                    launch { updateService.viewUpdate(update) }
                },
                onUpdate = {
                    launch { updateService.startUpdate(update) }
                }
            )
        } catch (e: Exception) {
            log(TAG, WARN) { "Update check failed: ${e.asLog()}" }
            return@map null
        }
    }
    .onStart { emit(null) }

internal fun DashboardViewModel.buildTitleCardItem(): Flow<TitleDashboardCardItem> = combine(
    upgradeInfo,
    taskManager.state,
) { upgradeInfo, taskState ->
    TitleDashboardCardItem(
        upgradeInfo = upgradeInfo,
        webpageTool = webpageTool,
        isWorking = !taskState.isIdle,
        onRibbonClicked = { webpageTool.open(SdmSeLinks.ISSUES) },
        onMascotTriggered = { easterEggTriggered.value = it }
    )
}

/**
 * Resolves the result shown on a tool's dashboard card. The dashboard historically showed the tool's
 * last *task* result — a snapshot frozen at scan completion — so post-scan mutations that only touch
 * live data (applying exclusions, the uninstall watcher pruning corpses) left the card advertising a
 * stale freeable size while the hero summary, which reads live data, disagreed. This prefers a summary
 * rebuilt from live tool [data] whenever a scan is on screen, while preserving frozen "freed X"
 * outcomes from a completed delete/one-click (those are not scan results) — including when that
 * cleanup left residue behind, which is the common case rather than the exception.
 */
internal inline fun <D : Any> resolveScanCardResult(
    isInitializing: Boolean,
    isWorking: Boolean,
    data: D?,
    hasData: Boolean,
    lastResult: SDMTool.Task.Result?,
    lastResultIsScan: Boolean,
    reconstruct: (D) -> SDMTool.Task.Result,
): SDMTool.Task.Result? = when {
    // Tool state not resolved yet, or a task is running (the card shows progress): keep the frozen result.
    isInitializing || isWorking -> lastResult
    // A completed delete/one-click: keep its "freed X" outcome even when residual data survives it. Some
    // junk can never be cleared — a locked system app's cache (com.google.android.gms is the usual one)
    // fails with LockedAppCacheException on every run — so a successful cleanup routinely leaves a small
    // remainder. Rebuilding from that remainder would advertise it as a fresh "can be freed" scan and hide
    // what the cleanup actually did: a run that freed 1 GB and left 5 MB behind reported "5 MB can be freed".
    lastResult != null && !lastResultIsScan -> lastResult
    // Live scan data was invalidated without a new task (e.g. Deduplicator arbiter-config change): drop a
    // now-stale scan result so the card falls back to its description instead of advertising vanished space.
    data == null -> null
    // A scan is on screen — data present, or the last result is a scan even if data is now empty after
    // excluding everything: rebuild the freeable summary from live data (empty data yields Success(0, 0)).
    hasData || lastResultIsScan -> reconstruct(data)
    // No result recorded and nothing in live data: fall back to the tool's description.
    else -> null
}

internal fun DashboardViewModel.buildCorpseFinderItem(): Flow<ToolDashboardCardItem> =
    (corpseFinder.state as Flow<CorpseFinder.State?>).onStart { emit(null) }.map { state ->
    val data = state?.data
    // CorpseFinder records the user-visible result in Data.lastResult and deliberately does NOT let the
    // background uninstall watcher overwrite it. Prefer that over TaskManager, whose newest entry for this
    // tool may be a watcher event — which would otherwise surface on the card as the user's last action.
    val lastResult = data?.lastResult
    // The single value the card renders: a frozen cleanup receipt, or a summary rebuilt from live
    // scan findings. Gating the discard on this rather than on its inputs keeps "there is something on
    // the card" and "there is something to clear" from ever drifting apart.
    val cardResult = resolveScanCardResult(
        isInitializing = state == null,
        isWorking = state?.progress != null,
        data = data,
        hasData = data.hasData,
        lastResult = lastResult,
        lastResultIsScan = lastResult is CorpseFinderScanTask.Success,
    ) { it.toScanSuccess() }
    ToolDashboardCardItem(
        toolType = SDMTool.Type.CORPSEFINDER,
        isInitializing = state == null,
        result = cardResult,
        progress = state?.progress,
        showProRequirement = false,
        onScan = {
            launch { submitTask(CorpseFinderScanTask()) }
        },
        onDelete = {
            events.tryEmit(DashboardEvents.CorpseFinderDeleteConfirmation(CorpseFinderDeleteTask()))
            Unit
        }.takeIf { state?.data?.hasData == true },
        onCancel = {
            launch { taskManager.cancel(SDMTool.Type.CORPSEFINDER) }
        },
        onViewTool = { showCorpseFinder() },
        onViewDetails = {
            navTo(CorpseDetailsRoute())
        },
        onDismissResult = {
            launch {
                // A task can start between the composition that rendered this control and the click
                // landing here (scheduler, widget, the uninstall watcher), and discardScanData()
                // waits on the same tool lock the task holds — without this we would clear the data
                // that task just produced.
                if (taskManager.state.first().isBusy(SDMTool.Type.CORPSEFINDER)) return@launch
                // Data first, then the recorded result: the reverse order lets the card rebuild from
                // the surviving residue for a frame and flash a "can be freed" figure. This card reads
                // Data.lastResult rather than TaskManager (see above), so dropping the tool's data is
                // what actually clears it.
                corpseFinder.discardScanData()
                taskManager.forgetCompleted(SDMTool.Type.CORPSEFINDER)
            }
        }.takeIf { cardResult != null && state?.progress == null },
    )
}

internal fun DashboardViewModel.buildSystemCleanerItem(): Flow<ToolDashboardCardItem> = combine(
    (systemCleaner.state as Flow<SystemCleaner.State?>).onStart { emit(null) },
    taskManager.state.map { it.getLatestResult(SDMTool.Type.SYSTEMCLEANER) },
) { state, lastResult ->
    val data = state?.data
    // The single value the card renders: a frozen cleanup receipt, or a summary rebuilt from live
    // scan findings. Gating the discard on this rather than on its inputs keeps "there is something on
    // the card" and "there is something to clear" from ever drifting apart.
    val cardResult = resolveScanCardResult(
        isInitializing = state == null,
        isWorking = state?.progress != null,
        data = data,
        hasData = data.hasData,
        lastResult = lastResult,
        lastResultIsScan = lastResult is SystemCleanerScanTask.Success,
    ) { it.toScanSuccess() }
    ToolDashboardCardItem(
        toolType = SDMTool.Type.SYSTEMCLEANER,
        isInitializing = state == null,
        result = cardResult,
        progress = state?.progress,
        showProRequirement = false,
        onScan = {
            launch { submitTask(SystemCleanerScanTask()) }
        },
        onDelete = {
            events.tryEmit(DashboardEvents.SystemCleanerDeleteConfirmation(SystemCleanerProcessingTask()))
            Unit
        }.takeIf { state?.data?.hasData == true },
        onCancel = {
            launch { taskManager.cancel(SDMTool.Type.SYSTEMCLEANER) }
        },
        onViewTool = { showSystemCleaner() },
        onViewDetails = {
            navTo(FilterContentDetailsRoute())
        },
        onDismissResult = {
            launch {
                // See buildCorpseFinderItem: bail if a task started since composition, and clear the
                // live data before the recorded result so the card can't flash the leftover figure.
                if (taskManager.state.first().isBusy(SDMTool.Type.SYSTEMCLEANER)) return@launch
                systemCleaner.discardScanData()
                taskManager.forgetCompleted(SDMTool.Type.SYSTEMCLEANER)
            }
        }.takeIf { cardResult != null && state?.progress == null },
    )
}

internal fun DashboardViewModel.buildAppCleanerItem(): Flow<ToolDashboardCardItem> = combine(
    (appCleaner.state as Flow<AppCleaner.State?>).onStart { emit(null) },
    taskManager.state.map { it.getLatestResult(SDMTool.Type.APPCLEANER) },
    upgradeInfo.map { it?.isPro ?: false },
) { state, lastResult, isPro ->
    val data = state?.data
    // The single value the card renders: a frozen cleanup receipt, or a summary rebuilt from live
    // scan findings. Gating the discard on this rather than on its inputs keeps "there is something on
    // the card" and "there is something to clear" from ever drifting apart.
    val cardResult = resolveScanCardResult(
        isInitializing = state == null,
        isWorking = state?.progress != null,
        data = data,
        hasData = data.hasData,
        lastResult = lastResult,
        lastResultIsScan = lastResult is AppCleanerScanTask.Success,
    ) { it.toScanSuccess() }
    ToolDashboardCardItem(
        toolType = SDMTool.Type.APPCLEANER,
        isInitializing = state == null,
        result = cardResult,
        progress = state?.progress,
        showProRequirement = !isPro,
        onScan = {
            launch { submitTask(AppCleanerScanTask()) }
        },
        onDelete = {
            events.tryEmit(DashboardEvents.AppCleanerDeleteConfirmation(AppCleanerProcessingTask()))
            Unit
        }.takeIf { state?.data?.hasData == true },
        onCancel = {
            launch { taskManager.cancel(SDMTool.Type.APPCLEANER) }
        },
        onViewTool = { showAppCleaner() },
        onViewDetails = {
            navTo(AppJunkDetailsRoute())
        },
        onDismissResult = {
            launch {
                // See buildCorpseFinderItem: bail if a task started since composition, and clear the
                // live data before the recorded result so the card can't flash the leftover figure.
                if (taskManager.state.first().isBusy(SDMTool.Type.APPCLEANER)) return@launch
                appCleaner.discardScanData()
                taskManager.forgetCompleted(SDMTool.Type.APPCLEANER)
            }
        }.takeIf { cardResult != null && state?.progress == null },
    )
}

internal fun DashboardViewModel.buildDeduplicatorItem(): Flow<ToolDashboardCardItem?> = combine(
    (deduplicator.state as Flow<Deduplicator.State?>).onStart { emit(null) },
    taskManager.state.map { it.getLatestResult(SDMTool.Type.DEDUPLICATOR) },
    upgradeInfo.map { it?.isPro ?: false },
) { state, lastResult, isPro ->
    val data = state?.data
    // The single value the card renders: a frozen cleanup receipt, or a summary rebuilt from live
    // scan findings. Gating the discard on this rather than on its inputs keeps "there is something on
    // the card" and "there is something to clear" from ever drifting apart.
    val cardResult = resolveScanCardResult(
        isInitializing = state == null,
        isWorking = state?.progress != null,
        data = data,
        hasData = data.hasData,
        lastResult = lastResult,
        lastResultIsScan = lastResult is DeduplicatorScanTask.Success,
    ) { it.toScanSuccess() }
    ToolDashboardCardItem(
        toolType = SDMTool.Type.DEDUPLICATOR,
        isInitializing = state == null,
        result = cardResult,
        progress = state?.progress,
        showProRequirement = !isPro,
        onScan = {
            launch { submitTask(DeduplicatorScanTask()) }
        },
        onDelete = {
            launch {
                val event = DashboardEvents.DeduplicatorDeleteConfirmation(
                    task = DeduplicatorDeleteTask(),
                    clusters = deduplicator.state.first().data?.clusters?.sortedByDescending { it.averageSize }
                )
                events.tryEmit(event)
            }
        }.takeIf { state?.data?.hasData == true },
        onCancel = {
            launch { taskManager.cancel(SDMTool.Type.DEDUPLICATOR) }
        },
        onViewTool = { showDeduplicator() },
        onViewDetails = {
            navTo(DeduplicatorDetailsRoute())
        },
        onDismissResult = {
            launch {
                // See buildCorpseFinderItem: bail if a task started since composition, and clear the
                // live data before the recorded result so the card can't flash the leftover figure.
                if (taskManager.state.first().isBusy(SDMTool.Type.DEDUPLICATOR)) return@launch
                deduplicator.discardScanData()
                taskManager.forgetCompleted(SDMTool.Type.DEDUPLICATOR)
            }
        }.takeIf { cardResult != null && state?.progress == null },
    )
}

internal fun DashboardViewModel.buildSqueezerItem(): Flow<SqueezerDashboardCardItem?> =
    (squeezer.state as Flow<Squeezer.State?>)
        .onStart { emit(null) }
        .mapLatest { state ->
            SqueezerDashboardCardItem(
                isInitializing = state == null,
                data = state?.data,
                progress = state?.progress,
                // Process results only: performProcess prunes the data before submit() stores its
                // result, so an unfiltered lastResult would show the preceding scan summary where
                // a compression receipt belongs.
                result = state?.lastResult as? SqueezerProcessTask.Result,
                onViewDetails = {
                    navTo(SqueezerSetupRoute)
                },
                onCancel = {
                    launch { taskManager.cancel(SDMTool.Type.SQUEEZER) }
                },
            )
        }

internal fun DashboardViewModel.buildAppControlItem(): Flow<AppControlDashboardCardItem?> =
    (appControl.state as Flow<AppControl.State?>)
        .onStart { emit(null) }
        .mapLatest { state ->
            AppControlDashboardCardItem(
                isInitializing = state == null,
                data = state?.data,
                progress = state?.progress,
                onViewDetails = {
                    navTo(AppControlListRoute)
                }
            )
        }

internal fun DashboardViewModel.buildAnalyzerItem(): Flow<AnalyzerDashboardCardItem?> = combine(
    // Each input emits an immediate fallback so the card shell renders in the first grid frame;
    // the trend slot shimmers (isLoadingTrend) until the Room history query resolves.
    (analyzer.data as Flow<Analyzer.Data?>).onStart { emit(null) },
    analyzer.progress.onStart { emit(null) },
    intervalFlow(1.hours)
        .flatMapLatest { _ ->
            // The forecast needs a LIVE reading: sampling is six-hourly, so the newest persisted
            // row can be hours stale and a download that already crossed the floor would still
            // read as filling. Re-read per history emission, otherwise a cleanup (which forces a
            // snapshot, and so a re-emission) would be forecast against pre-cleanup free space.
            spaceHistoryRepo.getAllHistory(Instant.now() - Duration.ofDays(7))
                .mapLatest { AnalyzerTrendInput(snapshots = it, primaryStorage = spaceTracker.readPrimaryStorage()) }
        }
        .onStart<AnalyzerTrendInput?> { emit(null) },
    analyzerSettings.lowStorageThresholdBytes.flow,
) { data, progress, trend, customThreshold ->
    val primary = trend?.primaryStorage
    AnalyzerDashboardCardItem(
        data = data,
        progress = progress,
        combinedDelta = trend?.let { calculateCombinedDelta(it.snapshots) },
        forecast = if (trend != null && primary != null) {
            StorageForecaster.forecast(
                history = trend.snapshots.filter { it.storageId == primary.storageId },
                current = primary,
                lowStorageThresholdBytes = LowStorage.resolveThreshold(primary.spaceCapacity, customThreshold),
            )
        } else {
            null
        },
        isLoadingTrend = trend == null,
        onViewDetails = {
            navTo(DeviceStorageRoute)
        },
    )
}

internal data class AnalyzerTrendInput(
    val snapshots: List<SpaceSnapshotEntity>,
    val primaryStorage: SpaceTracker.StorageSnapshot?,
)

internal fun calculateCombinedDelta(snapshots: List<SpaceSnapshotEntity>): Long? {
    val totals = snapshots
        .groupBy { it.storageId }
        .values
        .mapNotNull { StorageTrendCalculator.windowTotal(it) }
    if (totals.isEmpty()) return null
    return totals.sum()
}

internal fun DashboardViewModel.buildSetupCardItem(): Flow<SetupDashboardCardItem?> = setupManager.state
    .flatMapLatest { setupState ->
        if (setupState.isDone || setupState.isDismissed) return@flatMapLatest flowOf(null)

        val item = SetupDashboardCardItem(
            setupState = setupState,
            onDismiss = {
                setupManager.setDismissed(true)
                events.tryEmit(DashboardEvents.SetupDismissHint)
            },
            onContinue = { navTo(SetupRoute()) }
        )

        if (setupState.isIncompleteSettled) return@flatMapLatest flowOf(item)

        if (!setupState.isLoading) return@flatMapLatest flowOf(null)

        intervalFlow(1.seconds).map {
            val now = Instant.now()
            val loadingStart = setupState.startedLoadingAt ?: now
            if (Duration.between(loadingStart, now) >= Duration.ofSeconds(5)) {
                item
            } else {
                null
            }
        }
    }

internal fun DashboardViewModel.buildMotdItem(): Flow<MotdDashboardCardItem?> = motdRepo.motd
    .map {
        if (it == null) return@map null
        MotdDashboardCardItem(
            state = it,
            onPrimary = {
                launch {
                    it.motd.primaryLink?.let { webpageTool.open(it) }
                }
            },
            onTranslate = {
                launch {
                    webpageTool.open(it.translationUrl)
                }
            },
            onDismiss = {
                launch { motdRepo.dismiss(it.id) }
            }
        )
    }

internal fun DashboardViewModel.buildStatsItem(): Flow<StatsDashboardCardItem?> = mx.loperstack.tokkiphealth.common.flow.combine(
    statsRepo.state,
    upgradeInfo.map { it?.isPro ?: false },
    statsSettings.retentionReports.flow,
    statsSettings.retentionPaths.flow,
    statsSettings.retentionSnapshots.flow,
    // CurriculumVitae.installedAt is filterNotNull() and stays silent until updateAppLaunch()
    // has written the install date, which on a first-ever launch waits on upgrade state (5s
    // timeout). As a bare combine source that would hide the whole Stats card for that window,
    // so prime it with null and treat null as "no caption yet".
    (curriculumVitae.installedAt as Flow<Instant?>).onStart { emit(null) },
) { state, isPro, retentionReports, retentionPaths, retentionSnapshots, installedAt ->
    // Hide card if all retention settings are disabled
    if (retentionReports == Duration.ZERO && retentionPaths == Duration.ZERO && retentionSnapshots == Duration.ZERO) {
        return@combine null
    }
    // Also hide if there's no data
    if (state.isEmpty) return@combine null
    StatsDashboardCardItem(
        state = state,
        showProRequirement = !isPro,
        installedAt = installedAt,
        onViewAction = {
            if (isPro) {
                navTo(ReportsRoute)
            } else {
                navTo(UpgradeRoute())
            }
        }
    )
}
