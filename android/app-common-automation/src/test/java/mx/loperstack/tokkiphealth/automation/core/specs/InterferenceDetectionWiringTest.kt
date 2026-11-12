package mx.loperstack.tokkiphealth.automation.core.specs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import mx.loperstack.tokkiphealth.automation.core.AutomationEvent
import mx.loperstack.tokkiphealth.automation.core.AutomationHost
import mx.loperstack.tokkiphealth.automation.core.common.ACSNodeInfo
import mx.loperstack.tokkiphealth.automation.core.common.pkgId
import mx.loperstack.tokkiphealth.automation.core.common.stepper.StepContext
import mx.loperstack.tokkiphealth.automation.core.errors.AutomationInterferenceException
import mx.loperstack.tokkiphealth.common.funnel.IPCFunnel
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.progress.Progress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import testhelpers.BaseTest
import testhelpers.TestApplication
import testhelpers.coroutine.TestDispatcherProvider

/**
 * Exercises the real wiring that the pure detector test skips:
 * windowCheckDefaultSettings / interferenceAware -> windowCheck flow -> SettingsInterferenceDetector,
 * driven by a fake [AutomationHost]. Pure-logic / timing cases live in SettingsInterferenceDetectorTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class InterferenceDetectionWiringTest : BaseTest() {

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val ipcFunnel = IPCFunnel(appContext, TestDispatcherProvider())

    private val settingsPkg = "com.android.settings".toPkgId()
    private val targetPkg = "com.example.target".toPkgId()
    private val avastPkg = "com.avast.android.mobilesecurity".toPkgId()

    private val specGen = object : SpecGenerator {
        override val tag = "Test"
        override suspend fun isResponsible(pkg: Installed) = false
    }

    private fun rootOf(pkg: String): ACSNodeInfo = mockk(relaxed = true) {
        every { packageName } returns pkg
    }

    private fun hostWith(root: ACSNodeInfo, eventFlow: Flow<AutomationEvent> = emptyFlow()): AutomationHost =
        mockk(relaxed = true) {
            every { events } returns eventFlow
            coEvery { windowRoot() } returns root
        }

    private fun stepContextWith(host: AutomationHost): StepContext {
        val context = object : AutomationExplorer.Context {
            override val host: AutomationHost = host
            override val androidContext: Context = appContext
            override val progress: Flow<Progress.Data?> = emptyFlow()
            override fun updateProgress(update: (Progress.Data?) -> Progress.Data?) {}
        }
        return StepContext(hostContext = context, tag = "Test", stepAttempts = 0)
    }

    private fun pkgInfo(): Installed = mockk(relaxed = true) {
        every { id } returns targetPkg
        every { packageName } returns targetPkg.name
        every { hasNoSettings } returns false
    }

    @Test
    fun `windowCheckDefaultSettings aborts when a known locker holds the settings window`() = runTest {
        val host = hostWith(rootOf(avastPkg.name))
        val stepContext = stepContextWith(host)
        val check = with(specGen) { windowCheckDefaultSettings(settingsPkg, ipcFunnel, pkgInfo()) }

        shouldThrow<AutomationInterferenceException> {
            check(stepContext)
        }.blockerPkg shouldBe avastPkg
    }

    @Test
    fun `interferenceAware passes the expected settings window through unharmed`() = runTest {
        val root = rootOf(settingsPkg.name)
        val stepContext = stepContextWith(hostWith(root))
        val condition = with(specGen) {
            interferenceAware(setOf(settingsPkg), targetPkg, ipcFunnel) { _, r -> r.pkgId == settingsPkg }
        }
        val check = with(specGen) { windowCheck(condition) }

        check(stepContext) shouldBe root
    }
}
