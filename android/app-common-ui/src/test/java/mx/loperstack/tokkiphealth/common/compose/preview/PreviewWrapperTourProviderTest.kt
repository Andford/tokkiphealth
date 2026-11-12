package mx.loperstack.tokkiphealth.common.compose.preview

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.onNodeWithText
import mx.loperstack.tokkiphealth.common.compose.tour.GuidedTourAccess
import mx.loperstack.tokkiphealth.common.compose.tour.LocalGuidedTourController
import mx.loperstack.tokkiphealth.common.compose.tour.NoOpGuidedTourAccess
import org.junit.Assert.assertSame
import org.junit.Test
import testhelpers.compose.BaseComposeRobolectricTest

/**
 * Verifies that [PreviewWrapper] itself fixes tour-enabled previews: under inspection it provides
 * [NoOpGuidedTourAccess] so a composable reading [LocalGuidedTourController] renders instead of hitting the
 * local's `error()` default. This is the central mechanism that keeps every screen's `@Preview` from crashing.
 */
class PreviewWrapperTourProviderTest : BaseComposeRobolectricTest() {

    @Test
    fun `provides no-op tour controller under inspection`() {
        var seen: GuidedTourAccess? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewWrapper {
                    seen = LocalGuidedTourController.current
                    Text("shown")
                }
            }
        }

        composeRule.onNodeWithText("shown").assertExists()
        assertSame(NoOpGuidedTourAccess, seen)
    }
}
