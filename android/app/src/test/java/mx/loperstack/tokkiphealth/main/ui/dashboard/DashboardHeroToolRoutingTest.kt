package mx.loperstack.tokkiphealth.main.ui.dashboard

import mx.loperstack.tokkiphealth.analyzer.core.Analyzer
import mx.loperstack.tokkiphealth.analyzer.core.AnalyzerSettings
import mx.loperstack.tokkiphealth.appcleaner.core.AppCleaner
import mx.loperstack.tokkiphealth.appcontrol.core.AppControl
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.areas.DataAreaManager
import mx.loperstack.tokkiphealth.common.datastore.DataStoreValue
import mx.loperstack.tokkiphealth.common.debug.DebugCardProvider
import mx.loperstack.tokkiphealth.common.debug.recorder.core.DebugLogSessionManager
import mx.loperstack.tokkiphealth.common.navigation.NavEvent
import mx.loperstack.tokkiphealth.common.review.ReviewTool
import mx.loperstack.tokkiphealth.common.updater.UpdateService
import mx.loperstack.tokkiphealth.common.upgrade.UpgradeRepo
import mx.loperstack.tokkiphealth.corpsefinder.core.CorpseFinder
import mx.loperstack.tokkiphealth.corpsefinder.ui.CorpseFinderListRoute
import mx.loperstack.tokkiphealth.deduplicator.core.Deduplicator
import mx.loperstack.tokkiphealth.main.core.CurriculumVitae
import mx.loperstack.tokkiphealth.main.core.GeneralSettings
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.main.core.motd.MotdRepo
import mx.loperstack.tokkiphealth.main.core.release.ReleaseManager
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskManager
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskSubmitter
import mx.loperstack.tokkiphealth.scheduler.core.SchedulerManager
import mx.loperstack.tokkiphealth.setup.SetupManager
import mx.loperstack.tokkiphealth.squeezer.core.Squeezer
import mx.loperstack.tokkiphealth.stats.core.Report
import mx.loperstack.tokkiphealth.stats.core.SpaceHistoryRepo
import mx.loperstack.tokkiphealth.stats.core.SpaceTracker
import mx.loperstack.tokkiphealth.stats.core.StatsRepo
import mx.loperstack.tokkiphealth.stats.core.StatsSettings
import mx.loperstack.tokkiphealth.stats.ui.AffectedFilesRoute
import mx.loperstack.tokkiphealth.stats.ui.ReportsRoute
import mx.loperstack.tokkiphealth.swiper.core.Swiper
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleaner
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.Test
import testhelpers.BaseTest
import testhelpers.coroutine.TestDispatcherProvider
import testhelpers.coroutine.runTest2
import java.time.Instant
import java.util.UUID

/**
 * Routing of the dashboard freed-hero per-tool chips: a "will be freed" chip opens the live list, a
 * "freed" chip opens that tool's report (or the reports list when no report is resolvable).
 */
internal class DashboardHeroToolRoutingTest : BaseTest() {

    private fun TestScope.harness(statsRepo: StatsRepo): DashboardViewModel {
        val taskManager = mockk<TaskManager>(relaxed = true).apply {
            every { state } returns MutableStateFlow(TaskSubmitter.State())
        }
        val corpseFinder = mockk<CorpseFinder>(relaxed = true).apply { every { state } returns emptyFlow() }
        val systemCleaner = mockk<SystemCleaner>(relaxed = true).apply { every { state } returns emptyFlow() }
        val appCleaner = mockk<AppCleaner>(relaxed = true).apply { every { state } returns emptyFlow() }
        val deduplicator = mockk<Deduplicator>(relaxed = true).apply { every { state } returns emptyFlow() }
        val squeezer = mockk<Squeezer>(relaxed = true).apply { every { state } returns emptyFlow() }
        val appControl = mockk<AppControl>(relaxed = true).apply { every { state } returns emptyFlow() }
        val analyzer = mockk<Analyzer>(relaxed = true).apply {
            every { data } returns emptyFlow()
            every { progress } returns emptyFlow()
        }
        // A relaxed mock's flow never emits, which would stall the Analyzer card's combine.
        val analyzerSettings = mockk<AnalyzerSettings>(relaxed = true).apply {
            every { lowStorageThresholdBytes } returns mockk(relaxed = true) {
                every { flow } returns flowOf<Long?>(null)
            }
        }
        val schedulerManager = mockk<SchedulerManager>(relaxed = true).apply { every { state } returns emptyFlow() }
        val setupManager = mockk<SetupManager>(relaxed = true).apply { every { state } returns emptyFlow() }
        val areaManager = mockk<DataAreaManager>(relaxed = true).apply { every { latestResult } returns emptyFlow() }
        val sessionManager = mockk<DebugLogSessionManager>(relaxed = true).apply { every { sessions } returns emptyFlow() }
        val motdRepo = mockk<MotdRepo>(relaxed = true).apply { every { motd } returns emptyFlow() }
        val reviewTool = mockk<ReviewTool>(relaxed = true).apply { every { state } returns emptyFlow() }
        val anniversaryProvider = mockk<AnniversaryProvider>(relaxed = true).apply { every { item } returns emptyFlow() }
        val swiper = mockk<Swiper>(relaxed = true).apply {
            every { getSessionsWithStats() } returns emptyFlow()
            every { progress } returns emptyFlow()
        }
        val upgradeRepo = mockk<UpgradeRepo>(relaxed = true).apply {
            every { upgradeInfo } returns emptyFlow()
        }
        val updateService = mockk<UpdateService>(relaxed = true).apply { every { availableUpdate } returns emptyFlow() }
        val debugCardProvider = mockk<DebugCardProvider>(relaxed = true).apply {
            every { create(any(), any(), any(), any()) } returns emptyFlow()
        }
        val statsSettings = mockk<StatsSettings>(relaxed = true).apply {
            every { retentionReports } returns mockDuration()
            every { retentionPaths } returns mockDuration()
            every { retentionSnapshots } returns mockDuration()
        }
        val generalSettings = mockk<GeneralSettings>(relaxed = true).apply {
            every { dashboardCardConfig } returns mockk(relaxed = true) {
                every { flow } returns emptyFlow()
            }
            // Only CorpseFinder participates in mainAction so cleanup stamping can run without
            // the Pro-gated branches needing live upgrade state.
            every { oneClickCorpseFinderEnabled } returns mockBool(true)
            every { oneClickSystemCleanerEnabled } returns mockBool(false)
            every { oneClickAppCleanerEnabled } returns mockBool(false)
            every { oneClickDeduplicatorEnabled } returns mockBool(false)
        }
        val spaceHistoryRepo = mockk<SpaceHistoryRepo>(relaxed = true).apply {
            every { getAllHistory(any()) } returns emptyFlow()
        }
        val curriculumVitae = mockk<CurriculumVitae>(relaxed = true).apply {
            every { installedAt } returns emptyFlow()
        }

        return DashboardViewModel(
            context = mockk(relaxed = true),
            dispatcherProvider = TestDispatcherProvider(),
            areaManager = areaManager,
            taskManager = taskManager,
            setupManager = setupManager,
            corpseFinder = corpseFinder,
            systemCleaner = systemCleaner,
            appCleaner = appCleaner,
            appControl = appControl,
            analyzer = analyzer,
            analyzerSettings = analyzerSettings,
            debugCardProvider = debugCardProvider,
            deduplicator = deduplicator,
            squeezer = squeezer,
            swiper = swiper,
            upgradeRepo = upgradeRepo,
            generalSettings = generalSettings,
            webpageTool = mockk<WebpageTool>(relaxed = true),
            schedulerManager = schedulerManager,
            updateService = updateService,
            sessionManager = sessionManager,
            motdRepo = motdRepo,
            releaseManager = mockk<ReleaseManager>(relaxed = true),
            reviewTool = reviewTool,
            anniversaryProvider = anniversaryProvider,
            statsRepo = statsRepo,
            statsSettings = statsSettings,
            curriculumVitae = curriculumVitae,
            spaceHistoryRepo = spaceHistoryRepo,
            spaceTracker = mockk<SpaceTracker>(relaxed = true),
            deviceDetective = mockk(relaxed = true),
        )
    }

    private fun mockDuration(): DataStoreValue<java.time.Duration> = mockk(relaxed = true) {
        every { flow } returns MutableStateFlow(java.time.Duration.ZERO)
    }

    private fun mockBool(value: Boolean): DataStoreValue<Boolean> = mockk(relaxed = true) {
        every { flow } returns MutableStateFlow(value)
    }

    private class CollectedNav(val list: MutableList<NavEvent>, private val job: Job) {
        fun cancel() = job.cancel()
    }

    private fun CoroutineScope.collectNav(vm: DashboardViewModel): CollectedNav {
        val list = mutableListOf<NavEvent>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { vm.navEvents.collect { list.add(it) } }
        return CollectedNav(list, job)
    }

    @Test
    fun `freeable chip opens the live tool list`() = runTest2 {
        val statsRepo = mockk<StatsRepo>(relaxed = true).apply { every { state } returns emptyFlow() }
        val vm = harness(statsRepo)
        val nav = collectNav(vm)

        vm.onHeroToolClick(HeroSummary.Mode.FREEABLE, SDMTool.Type.CORPSEFINDER)
        advanceUntilIdle()

        nav.list shouldBe listOf(NavEvent.GoTo(CorpseFinderListRoute))
        nav.cancel()
    }

    @Test
    fun `freed chip opens that tool's report when one exists`() = runTest2 {
        val id = UUID.randomUUID()
        val report = mockk<Report>(relaxed = true)
        every { report.status } returns Report.Status.SUCCESS
        every { report.reportId } returns id
        val statsRepo = mockk<StatsRepo>(relaxed = true).apply {
            every { state } returns emptyFlow()
            coEvery { getReportForToolSince(SDMTool.Type.CORPSEFINDER, any()) } returns report
        }
        val vm = harness(statsRepo)
        val nav = collectNav(vm)

        vm.onHeroToolClick(HeroSummary.Mode.FREED, SDMTool.Type.CORPSEFINDER)
        advanceUntilIdle()

        nav.list shouldBe listOf<NavEvent>(NavEvent.GoTo(AffectedFilesRoute(id)))
        nav.cancel()
    }

    @Test
    fun `freed chip falls back to the reports list when no report is resolved`() = runTest2 {
        val statsRepo = mockk<StatsRepo>(relaxed = true).apply {
            every { state } returns emptyFlow()
            coEvery { getReportForToolSince(any(), any()) } returns null
        }
        val vm = harness(statsRepo)
        val nav = collectNav(vm)

        vm.onHeroToolClick(HeroSummary.Mode.FREED, SDMTool.Type.SYSTEMCLEANER)
        advanceUntilIdle()

        nav.list shouldBe listOf<NavEvent>(NavEvent.GoTo(ReportsRoute))
        nav.cancel()
    }

    @Test
    fun `freed chip resolves the report against this cleanup's start time`() = runTest2 {
        val sinceSlot = slot<Instant>()
        val statsRepo = mockk<StatsRepo>(relaxed = true).apply {
            every { state } returns emptyFlow()
            coEvery { getReportForToolSince(any(), capture(sinceSlot)) } returns null
        }
        val vm = harness(statsRepo)
        val nav = collectNav(vm)

        val beforeCleanup = Instant.now()
        vm.mainAction(BottomBarState.Action.ONECLICK)
        advanceUntilIdle()

        vm.onHeroToolClick(HeroSummary.Mode.FREED, SDMTool.Type.CORPSEFINDER)
        advanceUntilIdle()

        // The lookup must use this cleanup's batch start, not EPOCH or a stale earlier stamp.
        (sinceSlot.captured >= beforeCleanup) shouldBe true
        nav.list shouldBe listOf<NavEvent>(NavEvent.GoTo(ReportsRoute))
        nav.cancel()
    }

    @Test
    fun `freed chip falls back to the reports list when the latest report failed`() = runTest2 {
        val report = mockk<Report>(relaxed = true)
        every { report.status } returns Report.Status.FAILURE
        val statsRepo = mockk<StatsRepo>(relaxed = true).apply {
            every { state } returns emptyFlow()
            coEvery { getReportForToolSince(any(), any()) } returns report
        }
        val vm = harness(statsRepo)
        val nav = collectNav(vm)

        vm.onHeroToolClick(HeroSummary.Mode.FREED, SDMTool.Type.CORPSEFINDER)
        advanceUntilIdle()

        nav.list shouldBe listOf<NavEvent>(NavEvent.GoTo(ReportsRoute))
        nav.cancel()
    }
}
