package mx.loperstack.tokkiphealth.main.ui.dashboard

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.analyzer.core.Analyzer
import mx.loperstack.tokkiphealth.analyzer.core.AnalyzerSettings
import mx.loperstack.tokkiphealth.common.navigation.routes.DeviceStorageRoute
import mx.loperstack.tokkiphealth.appcleaner.core.AppCleaner
import mx.loperstack.tokkiphealth.appcleaner.core.hasData
import mx.loperstack.tokkiphealth.appcleaner.ui.AppCleanerListRoute
import mx.loperstack.tokkiphealth.appcleaner.ui.AppJunkDetailsRoute
import mx.loperstack.tokkiphealth.common.navigation.routes.AppControlListRoute
import mx.loperstack.tokkiphealth.appcleaner.core.tasks.AppCleanerOneClickTask
import mx.loperstack.tokkiphealth.appcleaner.core.tasks.AppCleanerProcessingTask
import mx.loperstack.tokkiphealth.appcleaner.core.tasks.AppCleanerScanTask
import mx.loperstack.tokkiphealth.appcleaner.core.tasks.AppCleanerSchedulerTask
import mx.loperstack.tokkiphealth.appcleaner.core.tasks.AppCleanerTask
import mx.loperstack.tokkiphealth.appcontrol.core.AppControl
import mx.loperstack.tokkiphealth.common.SdmSeLinks
import mx.loperstack.tokkiphealth.common.navigation.routes.UpgradeRoute
import mx.loperstack.tokkiphealth.corpsefinder.ui.CorpseDetailsRoute
import mx.loperstack.tokkiphealth.corpsefinder.ui.CorpseFinderListRoute
import mx.loperstack.tokkiphealth.common.flow.SingleEventFlow
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.areas.DataAreaManager
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.datastore.valueBlocking
import mx.loperstack.tokkiphealth.common.debug.DebugCardProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.debug.recorder.core.DebugLogSession
import mx.loperstack.tokkiphealth.common.debug.recorder.core.DebugLogSessionManager
import mx.loperstack.tokkiphealth.common.debug.recorder.core.RecorderModule
import mx.loperstack.tokkiphealth.common.debug.recorder.core.SessionId
import mx.loperstack.tokkiphealth.common.device.DeviceDetective
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.DebugRecorderDashboardCardItem
import mx.loperstack.tokkiphealth.common.debug.recorder.ui.RecorderActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.flow.intervalFlow
import mx.loperstack.tokkiphealth.common.flow.replayingShare
import mx.loperstack.tokkiphealth.common.flow.setupCommonEventHandlers
import mx.loperstack.tokkiphealth.common.flow.throttleLatest
import mx.loperstack.tokkiphealth.common.review.ReviewTool
import mx.loperstack.tokkiphealth.common.rngString
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.common.updater.UpdateService
import mx.loperstack.tokkiphealth.common.upgrade.UpgradeRepo
import mx.loperstack.tokkiphealth.common.upgrade.isProForUi
import mx.loperstack.tokkiphealth.corpsefinder.core.CorpseFinder
import mx.loperstack.tokkiphealth.corpsefinder.core.hasData
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderDeleteTask
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderOneClickTask
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderScanTask
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderSchedulerTask
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderTask
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.UninstallWatcherTask
import mx.loperstack.tokkiphealth.deduplicator.core.Deduplicator
import mx.loperstack.tokkiphealth.deduplicator.ui.DeduplicatorDetailsRoute
import mx.loperstack.tokkiphealth.deduplicator.ui.DeduplicatorListRoute
import mx.loperstack.tokkiphealth.deduplicator.core.hasData
import mx.loperstack.tokkiphealth.deduplicator.core.tasks.DeduplicatorDeleteTask
import mx.loperstack.tokkiphealth.deduplicator.core.tasks.DeduplicatorOneClickTask
import mx.loperstack.tokkiphealth.deduplicator.core.tasks.DeduplicatorScanTask
import mx.loperstack.tokkiphealth.deduplicator.core.tasks.DeduplicatorTask
import mx.loperstack.tokkiphealth.main.core.CurriculumVitae
import mx.loperstack.tokkiphealth.main.core.DashboardCardConfig
import mx.loperstack.tokkiphealth.main.core.DashboardCardType
import mx.loperstack.tokkiphealth.main.core.GeneralSettings
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.main.core.motd.MotdRepo
import mx.loperstack.tokkiphealth.main.core.release.ReleaseManager
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskManager
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskSubmitter
import mx.loperstack.tokkiphealth.main.core.taskmanager.getLatestResult
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.AnalyzerDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.DashboardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.AnniversaryDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.AppControlDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.DebugDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.DryRunWarningDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.ErrorDataAreaDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.MotdDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.ReviewDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.SchedulerDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.SetupDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.SqueezerDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.StatsDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.SwiperDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.TitleDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.ToolDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.UpdateDashboardCardItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.UpgradeDashboardCardItem
import mx.loperstack.tokkiphealth.scheduler.core.SchedulerManager
import mx.loperstack.tokkiphealth.scheduler.ui.SchedulerManagerRoute
import mx.loperstack.tokkiphealth.setup.SetupManager
import mx.loperstack.tokkiphealth.setup.SetupRoute
import mx.loperstack.tokkiphealth.squeezer.core.Squeezer
import mx.loperstack.tokkiphealth.squeezer.ui.SqueezerSetupRoute
import mx.loperstack.tokkiphealth.squeezer.core.tasks.SqueezerProcessTask
import mx.loperstack.tokkiphealth.squeezer.core.tasks.SqueezerScanTask
import mx.loperstack.tokkiphealth.squeezer.core.tasks.SqueezerTask
import mx.loperstack.tokkiphealth.stats.core.Report
import mx.loperstack.tokkiphealth.stats.core.SpaceHistoryRepo
import mx.loperstack.tokkiphealth.stats.core.SpaceTracker
import mx.loperstack.tokkiphealth.stats.core.StatsRepo
import mx.loperstack.tokkiphealth.stats.core.StatsSettings
import mx.loperstack.tokkiphealth.stats.ui.AffectedFilesRoute
import mx.loperstack.tokkiphealth.stats.ui.ReportsRoute
import mx.loperstack.tokkiphealth.common.navigation.routes.SwiperSessionsRoute
import mx.loperstack.tokkiphealth.systemcleaner.ui.FilterContentDetailsRoute
import mx.loperstack.tokkiphealth.systemcleaner.ui.SystemCleanerListRoute
import mx.loperstack.tokkiphealth.swiper.core.Swiper
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleaner
import mx.loperstack.tokkiphealth.systemcleaner.core.hasData
import mx.loperstack.tokkiphealth.systemcleaner.core.tasks.SystemCleanerOneClickTask
import mx.loperstack.tokkiphealth.systemcleaner.core.tasks.SystemCleanerProcessingTask
import mx.loperstack.tokkiphealth.systemcleaner.core.tasks.SystemCleanerScanTask
import mx.loperstack.tokkiphealth.systemcleaner.core.tasks.SystemCleanerSchedulerTask
import mx.loperstack.tokkiphealth.systemcleaner.core.tasks.SystemCleanerTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    dispatcherProvider: DispatcherProvider,
    private val areaManager: DataAreaManager,
    internal val taskManager: TaskManager,
    internal val setupManager: SetupManager,
    internal val corpseFinder: CorpseFinder,
    internal val systemCleaner: SystemCleaner,
    internal val appCleaner: AppCleaner,
    internal val appControl: AppControl,
    internal val analyzer: Analyzer,
    internal val analyzerSettings: AnalyzerSettings,
    debugCardProvider: DebugCardProvider,
    internal val deduplicator: Deduplicator,
    internal val squeezer: Squeezer,
    swiper: Swiper,
    private val upgradeRepo: UpgradeRepo,
    private val generalSettings: GeneralSettings,
    internal val webpageTool: WebpageTool,
    schedulerManager: SchedulerManager,
    internal val updateService: UpdateService,
    private val sessionManager: DebugLogSessionManager,
    internal val motdRepo: MotdRepo,
    private val releaseManager: ReleaseManager,
    private val reviewTool: ReviewTool,
    anniversaryProvider: AnniversaryProvider,
    internal val statsRepo: StatsRepo,
    internal val statsSettings: StatsSettings,
    internal val curriculumVitae: CurriculumVitae,
    internal val spaceHistoryRepo: SpaceHistoryRepo,
    internal val spaceTracker: SpaceTracker,
    deviceDetective: DeviceDetective,
) : ViewModel4(dispatcherProvider, TAG) {

    // TV-style devices navigate via D-pad focus, which scrolls the grid — auto-hiding dock on
    // scroll would hide controls the user is about to focus. Static per process, no flow needed.
    val isTvDevice: Boolean by lazy { deviceDetective.isTvLikeDevice() }

    init {
        launch {
            releaseManager.checkEarlyAdopter()
        }
    }

    private val refreshTrigger = MutableStateFlow(rngString)

    val events = SingleEventFlow<DashboardEvents>()

    // The dashboard list is an all-or-nothing combine of many sources: it stays on the loading
    // spinner until every source has emitted once. Several sources re-run async work (Room, DataStore,
    // a filesystem scan) on every ViewModel recreation because they're shared with stopTimeout=0 /
    // replayExpiration=0, so a single slow source could wedge the whole screen permanently on a warm
    // Activity recreation. These helpers guarantee each combine input has an immediate first value and
    // tag it for diagnostics so a stalling upstream is identifiable from VERBOSE logs.
    private fun descLite(value: Any?): String = when (value) {
        null -> "null"
        is Collection<*> -> "size=${value.size}"
        else -> value::class.simpleName ?: "?"
    }

    /** Inject [fallback] as the immediate first value (outer onStart) while the inner handlers log the
     *  real upstream only — a missing "upstream emit" on reproduction pinpoints the staller. */
    private fun <T> Flow<T>.listStateSource(name: String, fallback: T): Flow<T> = this
        .onEach { log(TAG, VERBOSE) { "listState.$name upstream emit: ${descLite(it)}" } }
        .onStart { log(TAG, VERBOSE) { "listState.$name upstream start" } }
        .onStart { emit(fallback) }

    /** Diagnostic-only tag for sources that already have a guaranteed-immediate first value. */
    private fun <T> Flow<T>.listStateDiag(name: String): Flow<T> = this
        .onEach { log(TAG, VERBOSE) { "listState.$name upstream emit: ${descLite(it)}" } }
        .onStart { log(TAG, VERBOSE) { "listState.$name upstream start" } }

    private val updateInfo: Flow<UpdateDashboardCardItem?> = buildUpdateInfo()

    internal val upgradeInfo: Flow<UpgradeRepo.Info?> = upgradeRepo.upgradeInfo
        .map {
            @Suppress("USELESS_CAST")
            it as UpgradeRepo.Info?
        }
        // Emit null immediately so the dashboard's all-or-nothing combine (and every upgrade-gated
        // card + the bottom bar) never blocks waiting for upgrade state to resolve on a warm restart.
        // Consumers already null-guard (it?.isPro ?: false, nullable Info? in items).
        .onStart { emit(null) }
        .setupCommonEventHandlers(TAG) { "upgradeInfo" }
        .replayingShare(vmScope)

    internal val easterEggTriggered = MutableStateFlow(false)

    private val titleCardItem = buildTitleCardItem()

    private val corpseFinderItem: Flow<ToolDashboardCardItem> = buildCorpseFinderItem()

    private val systemCleanerItem: Flow<ToolDashboardCardItem> = buildSystemCleanerItem()

    private val appCleanerItem: Flow<ToolDashboardCardItem> = buildAppCleanerItem()

    private val deduplicatorItem: Flow<ToolDashboardCardItem?> = buildDeduplicatorItem()

    private val squeezerItem: Flow<SqueezerDashboardCardItem?> = buildSqueezerItem()

    private val appControlItem: Flow<AppControlDashboardCardItem?> = buildAppControlItem()

    private val analyzerItem: Flow<AnalyzerDashboardCardItem?> = buildAnalyzerItem()

    private val schedulerItem: Flow<SchedulerDashboardCardItem?> = combine(
        // Self-seed null so the card is present (as an initializing placeholder) in the very first
        // skeleton frame instead of popping in after schedulerManager.state (DynamicStateFlow on IO)
        // resolves. null renders a neutral loading card, not a misleading "no active schedules".
        (schedulerManager.state as Flow<SchedulerManager.State?>).onStart { emit(null) },
        taskManager.state,
    ) { schedulerState, taskState ->
        SchedulerDashboardCardItem(
            isInitializing = schedulerState == null,
            schedulerState = schedulerState ?: SchedulerManager.State(schedules = emptySet()),
            taskState = taskState,
            onManageClicked = {
                navTo(SchedulerManagerRoute)
            }
        )
    }

    private val dataAreaItem: Flow<ErrorDataAreaDashboardCardItem?> = areaManager.latestResult
        .map { result -> dataAreaCardFor(result) { launch { areaManager.reload() } } }

    private val motdItem: Flow<MotdDashboardCardItem?> = buildMotdItem()

    private val setupCardItem: Flow<SetupDashboardCardItem?> = buildSetupCardItem()

    private val reviewItem: Flow<ReviewDashboardCardItem?> = reviewTool.state.map { state ->
        if (!state.shouldAskForReview) return@map null

        ReviewDashboardCardItem(
            onReview = {
                launch { reviewTool.reviewNow(it) }
            },
            onDismiss = {
                launch { reviewTool.dismiss() }
            }
        )
    }

    private val statsItem: Flow<StatsDashboardCardItem?> = buildStatsItem()

    private val anniversaryItem: Flow<AnniversaryDashboardCardItem?> = anniversaryProvider.item

    private val swiperItem: Flow<SwiperDashboardCardItem?> = combine(
        // Self-seed null so the card is present (as an initializing placeholder) in the very first
        // skeleton frame instead of popping in after the Room-backed sessions query resolves.
        (swiper.getSessionsWithStats() as Flow<List<Swiper.SessionWithStats>?>).onStart { emit(null) },
        swiper.progress.onStart { emit(null) },
    ) { sessionsWithStats, progress ->
        SwiperDashboardCardItem(
            isInitializing = sessionsWithStats == null,
            sessionsWithStats = sessionsWithStats ?: emptyList(),
            progress = progress,
            onViewDetails = { showSwiper() }
        )
    }

    private val dashboardCardConfig: Flow<DashboardCardConfig> = generalSettings.dashboardCardConfig.flow
        .replayingShare(vmScope)

    // Combine refresh trigger with card config to stay within combine's argument limit
    private val cardConfigWithRefresh: Flow<DashboardCardConfig> = combine(
        refreshTrigger,
        dashboardCardConfig,
    ) { _, config -> config }

    val listState: StateFlow<ListState?> = mx.loperstack.tokkiphealth.common.flow.combine(
        sessionManager.sessions.listStateSource("sessions", emptyList()),
        debugCardProvider.create(
            vm = this,
            onNavigate = { navTo(it as mx.loperstack.tokkiphealth.common.navigation.NavigationDestination) },
            onError = { errorEvents.tryEmit(it) },
            onShowEvent = { events.tryEmit(it) },
        ).listStateSource("debug", null),
        titleCardItem.listStateDiag("title"),
        upgradeInfo.listStateDiag("upgrade"),
        updateInfo.listStateSource("update", null),
        setupCardItem.listStateSource("setup", null),
        dataAreaItem.listStateSource("dataArea", null),
        corpseFinderItem.listStateDiag("corpse"),
        systemCleanerItem.listStateDiag("system"),
        appCleanerItem.listStateDiag("app"),
        deduplicatorItem.listStateDiag("dedup"),
        squeezerItem.listStateDiag("squeezer"),
        appControlItem.listStateDiag("appControl"),
        analyzerItem.listStateDiag("analyzer"),
        schedulerItem.listStateDiag("scheduler"),
        motdItem.listStateSource("motd", null),
        reviewItem.listStateSource("review", null),
        anniversaryItem.listStateSource("anniversary", null),
        statsItem.listStateSource("stats", null),
        swiperItem.listStateDiag("swiper"),
        cardConfigWithRefresh.listStateDiag("cardConfig"),
    ) { sessions: List<DebugLogSession>,
        debugItem: DebugDashboardCardItem?,
        titleInfo: TitleDashboardCardItem,
        upgradeInfo: UpgradeRepo.Info?,
        updateInfo: UpdateDashboardCardItem?,
        setupItem: SetupDashboardCardItem?,
        dataAreaError: ErrorDataAreaDashboardCardItem?,
        corpseFinderItem: ToolDashboardCardItem?,
        systemCleanerItem: ToolDashboardCardItem?,
        appCleanerItem: ToolDashboardCardItem?,
        deduplicatorItem: ToolDashboardCardItem?,
        squeezerItem: SqueezerDashboardCardItem?,
        appControlItem: AppControlDashboardCardItem?,
        analyzerItem: AnalyzerDashboardCardItem?,
        schedulerItem: SchedulerDashboardCardItem?,
        motdItem: MotdDashboardCardItem?,
        reviewItem: ReviewDashboardCardItem?,
        anniversaryItem: AnniversaryDashboardCardItem?,
        statsItem: StatsDashboardCardItem?,
        swiperItem: SwiperDashboardCardItem?,
        cardConfig: DashboardCardConfig ->
        val items = mutableListOf<DashboardItem>(titleInfo)

        val noError = dataAreaError == null

        val anyInitializing = setOfNotNull(
            corpseFinderItem?.isInitializing,
            systemCleanerItem?.isInitializing,
            appCleanerItem?.isInitializing,
            deduplicatorItem?.isInitializing,
            squeezerItem?.isInitializing,
            appControlItem?.isInitializing,
        ).any { it }

        if (motdItem == null && updateInfo == null && setupItem == null && noError && reviewItem != null && !anyInitializing) {
            log(TAG, INFO) { "Showing review item" }
            items.add(reviewItem)
        } else if (reviewItem != null) {
            log(TAG) { "Could show review item but other high priority items are currently being shown" }
        }

        motdItem?.let { items.add(it) }
        updateInfo?.let { items.add(it) }
        setupItem?.let { items.add(it) }
        dataAreaError?.let { items.add(it) }

        // Dry-run makes every cleanup a no-op while still reporting freed space. The toggle lives at the
        // bottom of the dashboard, so warn up top for as long as it's on. debugItem is null unless debug
        // mode is on, which matches how App.kt gates Bugs.isDryRun.
        debugItem
            ?.takeIf { it.isDryRunEnabled }
            ?.let { debug -> items.add(DryRunWarningDashboardCardItem(onDisable = { debug.onDryRunEnabled(false) })) }

        anniversaryItem?.let { items.add(it) }

        // Add tool cards based on user configuration
        for (entry in cardConfig.cards) {
            if (!entry.isVisible) continue
            when (entry.type) {
                DashboardCardType.CORPSEFINDER -> corpseFinderItem?.let { items.add(it) }
                DashboardCardType.SYSTEMCLEANER -> systemCleanerItem?.let { items.add(it) }
                DashboardCardType.APPCLEANER -> appCleanerItem?.let { items.add(it) }
                DashboardCardType.DEDUPLICATOR -> deduplicatorItem?.let { items.add(it) }
                DashboardCardType.APPCONTROL -> appControlItem?.let { items.add(it) }
                DashboardCardType.SWIPER -> swiperItem?.let { items.add(it) }
                DashboardCardType.SQUEEZER -> squeezerItem?.let { items.add(it) }
                DashboardCardType.ANALYZER -> analyzerItem?.let { items.add(it) }
                DashboardCardType.SCHEDULER -> schedulerItem?.let { items.add(it) }
                DashboardCardType.STATS -> statsItem?.let { items.add(it) }
            }
        }

        upgradeInfo
            ?.takeIf { !it.isPro }
            ?.let {
                UpgradeDashboardCardItem(
                    onUpgrade = { navTo(UpgradeRoute()) }
                )
            }
            ?.run { items.add(this) }

        val recordingSession = sessions.filterIsInstance<DebugLogSession.Recording>().firstOrNull()
        val isRecording = recordingSession != null
        if (isRecording || debugItem != null) {
            val item = DebugRecorderDashboardCardItem(
                webpageTool = webpageTool,
                isRecording = isRecording,
                currentLogDir = recordingSession?.logDir,
                onToggleRecording = {
                    if (isRecording) {
                        launch {
                            when (val result = sessionManager.requestStopRecording()) {
                                is RecorderModule.StopResult.TooShort -> {
                                    events.tryEmit(DashboardEvents.ShowShortRecordingWarning)
                                }
                                is RecorderModule.StopResult.Stopped -> {
                                    launchRecorderActivity(result.sessionId)
                                }
                                is RecorderModule.StopResult.NotRecording -> {}
                            }
                        }
                    } else {
                        launch { sessionManager.startRecording() }
                    }
                }
            )
            items.add(item)
        }

        debugItem?.let { items.add(it) }

        ListState(items = items)
    }
        .throttleLatest(500)
        .safeStateIn(
            initialValue = null,
            onError = { null },
        )

    data class ListState(
        val items: List<DashboardItem>,
    )

    /** The main-action/hero state machine; this VM delegates to it and keeps error/navigation routing. */
    private val mainActionEngine = DashboardMainActionEngine(
        scope = vmScope,
        taskManager = taskManager,
        corpseFinder = corpseFinder,
        systemCleaner = systemCleaner,
        appCleaner = appCleaner,
        deduplicator = deduplicator,
        generalSettings = generalSettings,
        upgradeRepo = upgradeRepo,
        upgradeInfo = upgradeInfo,
        submitTask = ::submitTask,
        onUpgradeRequired = { navTo(UpgradeRoute()) },
        onStateError = { errorEvents.tryEmit(it) },
    )

    val bottomBarState: StateFlow<BottomBarState?> = mainActionEngine.bottomBarState(
        listIsReady = listState.map { state -> state?.items?.any { it is MainActionItem } == true },
    ).safeStateIn(
        initialValue = null,
        onError = { null },
    )

    /**
     * Whether the hero card is expanded. Auto-expands only for a one-tap main action that produced
     * a result; anything started elsewhere leaves it collapsed and reachable via the bar's chip.
     */
    val isHeroExpanded: StateFlow<Boolean> = mainActionEngine.isHeroExpanded

    fun dismissHero() = mainActionEngine.dismissHero()

    /** Expands a collapsed hero (via the compact summary chip in the bar). */
    fun expandHero() = mainActionEngine.expandHero()

    /**
     * Drops all pending scan results, returning the dashboard to its pristine SCAN state. Unlike
     * [dismissHero] (which only hides the card), this clears the tools' data, so the main action
     * no longer threatens deletion. Recoverable by simply rescanning, hence no confirmation step.
     */
    fun discardResults() = mainActionEngine.discardResults()

    fun mainAction(actionState: BottomBarState.Action) = mainActionEngine.mainAction(actionState)

    fun confirmCorpseDeletion() = launch {
        log(TAG, INFO) { "confirmCorpseDeletion()" }
        submitTask(CorpseFinderDeleteTask())
    }

    fun showCorpseFinder() {
        log(TAG, INFO) { "showCorpseFinderDetails()" }
        navTo(CorpseFinderListRoute)
    }

    fun confirmFilterContentDeletion() = launch {
        log(TAG, INFO) { "confirmFilterContentDeletion()" }
        submitTask(SystemCleanerProcessingTask())
    }

    fun showSystemCleaner() {
        log(TAG, INFO) { "showSystemCleanerDetails()" }
        navTo(SystemCleanerListRoute)
    }

    fun confirmAppJunkDeletion() = launch {
        log(TAG, INFO) { "confirmAppJunkDeletion()" }

        if (!upgradeRepo.isProForUi()) {
            navTo(UpgradeRoute())
            return@launch
        }
        submitTask(AppCleanerProcessingTask())
    }

    fun showAppCleaner() {
        log(TAG, INFO) { "showAppCleanerDetails()" }
        navTo(AppCleanerListRoute)
    }

    fun showDeduplicator() {
        log(TAG, INFO) { "showDeduplicatorDetails()" }
        navTo(DeduplicatorListRoute)
    }

    /**
     * Hero-chip tap. Routes by the *rendered* card mode (passed up from the card, not re-read from
     * state): a "will be freed" chip opens the live findings list to review; a "freed" chip opens
     * what that tool actually removed (its report), since the live list is now empty.
     */
    fun onHeroToolClick(mode: HeroSummary.Mode, type: SDMTool.Type) {
        log(TAG, INFO) { "onHeroToolClick($mode, $type)" }
        when (mode) {
            HeroSummary.Mode.FREED -> showToolReport(type)
            HeroSummary.Mode.FREEABLE -> showTool(type)
            // Renders no chips, so there is nothing here to tap.
            HeroSummary.Mode.NOTHING_FREED -> {}
            // Renders no *free* chips; its locked chips route through onLockedToolClick instead.
            HeroSummary.Mode.LOCKED_ONLY -> {}
        }
    }

    /** Opens a tool's findings list — used by the hero card's per-tool chips (pre-deletion). */
    fun showTool(type: SDMTool.Type) {
        log(TAG, INFO) { "showTool($type)" }
        when (type) {
            SDMTool.Type.CORPSEFINDER -> showCorpseFinder()
            SDMTool.Type.SYSTEMCLEANER -> showSystemCleaner()
            SDMTool.Type.APPCLEANER -> showAppCleaner()
            SDMTool.Type.DEDUPLICATOR -> showDeduplicator()
            else -> log(TAG, WARN) { "showTool() ignoring unsupported type: $type" }
        }
    }

    /**
     * Opens the report for what [type] just removed (post-deletion freed-hero chip). Resolves *this*
     * cleanup's report via [DashboardMainActionEngine.freedResultSince]; falls back to the reports
     * list when the report isn't persisted yet (sub-second tap) or the deletion failed.
     */
    fun showToolReport(type: SDMTool.Type) = launch {
        log(TAG, INFO) { "showToolReport($type)" }
        val report = statsRepo.getReportForToolSince(type, mainActionEngine.freedResultSince)
        when (report?.status) {
            Report.Status.SUCCESS, Report.Status.PARTIAL_SUCCESS -> navTo(AffectedFilesRoute(report.reportId))
            else -> navTo(ReportsRoute)
        }
    }

    fun showSwiper() {
        log(TAG, INFO) { "showSwiper()" }
        navTo(SwiperSessionsRoute)
    }

    fun confirmDeduplicatorDeletion() = launch {
        log(TAG, INFO) { "confirmDeduplicatorDeletion()" }

        if (!upgradeRepo.isProForUi()) {
            navTo(UpgradeRoute())
            return@launch
        }
        submitTask(DeduplicatorDeleteTask())
    }

    fun confirmStopRecording() = launch {
        val result = sessionManager.forceStopRecording() ?: return@launch
        launchRecorderActivity(result.sessionId)
    }

    private fun launchRecorderActivity(sessionId: SessionId) {
        val intent = RecorderActivity.getLaunchIntent(context, sessionId).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        events.tryEmit(DashboardEvents.OpenIntent(intent))
    }

    fun undoSetupHide() = launch {
        log(TAG) { "undoSetupHide()" }
        setupManager.setDismissed(false)
    }

    internal suspend fun submitTask(task: SDMTool.Task): SDMTool.Task.Result {
        log(TAG, VERBOSE) { "Submitting $task" }
        val result = taskManager.submit(task)
        log(TAG, VERBOSE) { "Task result for $task was $result" }
        when (result) {
            is CorpseFinderTask.Result -> when (result) {
                is CorpseFinderScanTask.Success -> {}
                is UninstallWatcherTask.Success -> {}
                is CorpseFinderSchedulerTask.Success -> {}
                is CorpseFinderDeleteTask.Success -> events.tryEmit(DashboardEvents.TaskResult(result))
                is CorpseFinderOneClickTask.Success -> events.tryEmit(DashboardEvents.TaskResult(result))
            }

            is SystemCleanerTask.Result -> when (result) {
                is SystemCleanerScanTask.Success -> {}
                is SystemCleanerSchedulerTask.Success -> {}
                is SystemCleanerProcessingTask.Success -> events.tryEmit(DashboardEvents.TaskResult(result))
                is SystemCleanerOneClickTask.Success -> events.tryEmit(DashboardEvents.TaskResult(result))
            }

            is AppCleanerTask.Result -> when (result) {
                is AppCleanerScanTask.Success -> {}
                is AppCleanerSchedulerTask.Success -> {}
                is AppCleanerProcessingTask.Success -> events.tryEmit(DashboardEvents.TaskResult(result))
                is AppCleanerOneClickTask.Success -> events.tryEmit(DashboardEvents.TaskResult(result))
            }

            is DeduplicatorTask.Result -> when (result) {
                is DeduplicatorScanTask.Success -> {}
                is DeduplicatorDeleteTask.Success -> events.tryEmit(DashboardEvents.TaskResult(result))
                is DeduplicatorOneClickTask.Success -> events.tryEmit(DashboardEvents.TaskResult(result))
            }

            is SqueezerTask.Result -> when (result) {
                is SqueezerScanTask.Success -> {}
                is SqueezerProcessTask.Success -> events.tryEmit(DashboardEvents.TaskResult(result))
            }
        }
        return result
    }

    companion object {
        private val TAG = logTag("Dashboard", "ViewModel")

        internal fun dataAreaCardFor(
            result: Result<DataAreaManager.State>?,
            onReload: () -> Unit,
        ): ErrorDataAreaDashboardCardItem? = when {
            result == null -> null
            result.getOrNull()?.areas?.isNotEmpty() == true -> null
            else -> ErrorDataAreaDashboardCardItem(buildFailed = result.isFailure, onReload = onReload)
        }
    }
}
