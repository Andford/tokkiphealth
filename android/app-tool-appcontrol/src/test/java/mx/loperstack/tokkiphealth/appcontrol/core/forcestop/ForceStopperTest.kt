package mx.loperstack.tokkiphealth.appcontrol.core.forcestop

import mx.loperstack.tokkiphealth.appcontrol.core.AppInfo
import mx.loperstack.tokkiphealth.automation.core.AutomationSubmitter
import mx.loperstack.tokkiphealth.automation.core.ForceStopAutomationTask
import mx.loperstack.tokkiphealth.common.adb.AdbManager
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallDetails
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.sharedresource.SharedResource
import mx.loperstack.tokkiphealth.common.user.UserHandle2
import mx.loperstack.tokkiphealth.setup.SetupModule
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import testhelpers.BaseTest
import testhelpers.coroutine.TestDispatcherProvider
import testhelpers.coroutine.runTest2

class ForceStopperTest : BaseTest() {

    // adoptChildResource(pkgOps) calls addChild(...) + get() on the child resource, which a plain
    // MockK mock can't answer. Mirrors the pattern in AppControlTest.
    private val keepAliveScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @AfterEach
    fun stopKeepAliveScope() {
        keepAliveScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    private val systemUserHandle = UserHandle2(handleId = 0)

    private fun appInfo(pkgName: String): AppInfo {
        val pkgId = Pkg.Id(pkgName)
        val pkg = mockk<Installed>(relaxed = true, moreInterfaces = arrayOf(InstallDetails::class)).apply {
            every { id } returns pkgId
            every { packageName } returns pkgName
            every { label } returns null
            every { userHandle } returns systemUserHandle
            every { installId } returns InstallId(pkgId, systemUserHandle)
        }
        return AppInfo(
            pkg = pkg,
            isActive = null,
            sizes = null,
            usage = null,
            userProfile = null,
            canBeToggled = false,
            canBeStopped = true,
            canBeExported = false,
            canBeDeleted = false,
            canBeArchived = false,
            canBeRestored = false,
        )
    }

    private class Setup(
        val forceStopper: ForceStopper,
        val pkgOps: PkgOps,
        val automation: AutomationSubmitter,
    )

    private fun setupForceStopper(
        useRoot: Boolean = false,
        useAdb: Boolean = false,
        automationComplete: Boolean = false,
        automationResult: ForceStopAutomationTask.Result = ForceStopAutomationTask.Result(
            successful = emptySet(),
            failed = emptySet(),
        ),
    ): Setup {
        val pkgOps = mockk<PkgOps>(relaxed = true).apply {
            every { sharedResource } returns SharedResource.createKeepAlive("pkgOps", keepAliveScope)
        }
        val automation = mockk<AutomationSubmitter>().apply {
            coEvery { submit(any()) } returns automationResult
        }
        val rootManager = mockk<RootManager>().apply {
            every { this@apply.useRoot } returns flowOf(useRoot)
        }
        val adbManager = mockk<AdbManager>().apply {
            every { this@apply.useAdb } returns flowOf(useAdb)
        }
        val automationSetupModule = mockk<SetupModule>().apply {
            every { state } returns flowOf(
                object : SetupModule.State.Current {
                    override val type = SetupModule.Type.AUTOMATION
                    override val isComplete: Boolean = automationComplete
                },
            )
            coJustRun { refresh() }
        }

        val forceStopper = ForceStopper(
            appScope = keepAliveScope,
            dispatcherProvider = TestDispatcherProvider(),
            pkgOps = pkgOps,
            automation = automation,
            adbManager = adbManager,
            rootManager = rootManager,
            automationSetupModule = automationSetupModule,
        )
        return Setup(forceStopper = forceStopper, pkgOps = pkgOps, automation = automation)
    }

    @Test
    fun `no available method reports every target as failed`() = runTest2 {
        val target = appInfo("eu.thlab.target")
        val setup = setupForceStopper(useRoot = false, useAdb = false, automationComplete = false)

        val result = setup.forceStopper.forceStop(listOf(target))

        result.success shouldBe emptySet()
        result.failed shouldBe setOf(target.installId)
        coVerify(exactly = 0) { setup.pkgOps.forceStop(any()) }
        coVerify(exactly = 0) { setup.automation.submit(any()) }
    }

    @Test
    fun `a complete automation setup dispatches the automation task`() = runTest2 {
        val target = appInfo("eu.thlab.target")
        val setup = setupForceStopper(
            automationComplete = true,
            automationResult = ForceStopAutomationTask.Result(
                successful = setOf(target.installId),
                failed = emptySet(),
            ),
        )

        val result = setup.forceStopper.forceStop(listOf(target))

        result.success shouldBe setOf(target.installId)
        result.failed shouldBe emptySet()
        coVerify(exactly = 1) { setup.automation.submit(any()) }
    }

    @Test
    fun `root uses pkgOps force stop`() = runTest2 {
        val target = appInfo("eu.thlab.target")
        val targetId = target.installId
        val setup = setupForceStopper(useRoot = true)

        val result = setup.forceStopper.forceStop(listOf(target))

        result.success shouldBe setOf(targetId)
        result.failed shouldBe emptySet()
        coVerify(exactly = 1) { setup.pkgOps.forceStop(targetId) }
        coVerify(exactly = 0) { setup.automation.submit(any()) }
    }

    @Test
    fun `the dead end branch keeps the off limit entries found before it`() = runTest2 {
        val offLimit = appInfo("com.android.systemui")
        val target = appInfo("eu.thlab.target")
        val setup = setupForceStopper(useRoot = false, useAdb = false, automationComplete = false)

        val result = setup.forceStopper.forceStop(listOf(offLimit, target))

        result.success shouldBe emptySet()
        result.failed shouldBe setOf(offLimit.installId, target.installId)
    }
}
