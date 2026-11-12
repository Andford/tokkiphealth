package mx.loperstack.tokkiphealth.automation.ui

import android.content.Context
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import mx.loperstack.tokkiphealth.automation.R as AutomationR
import mx.loperstack.tokkiphealth.common.R as CommonR
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper
import mx.loperstack.tokkiphealth.common.progress.Progress
import org.junit.Test
import testhelpers.compose.BaseComposeRobolectricTest

class AutomationOverlayTest : BaseComposeRobolectricTest() {

    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    private val sampleState = AutomationOverlayState(
        title = "AppCleaner".toCaString(),
        subtitle = "Clearing caches…".toCaString(),
        progress = Progress.Data(
            primary = "com.example.app".toCaString(),
            secondary = "/storage/emulated/0/Android/data/com.example.app/cache".toCaString(),
            count = Progress.Count.Indeterminate(),
        ),
    )

    @Test
    fun `phone shows the touch cancel button`() {
        composeRule.setContent {
            PreviewWrapper {
                AutomationOverlay(state = sampleState, isTv = false, onCancel = {})
            }
        }
        composeRule.onNodeWithText(ctx.getString(CommonR.string.general_cancel_action)).assertExists()
    }

    @Test
    fun `tv hides the cancel button and shows the home-to-cancel hint`() {
        composeRule.setContent {
            PreviewWrapper {
                AutomationOverlay(
                    state = sampleState,
                    isTv = true,
                    showHomeCancelHint = true,
                    onCancel = {},
                )
            }
        }
        composeRule.onNodeWithText(ctx.getString(CommonR.string.general_cancel_action)).assertDoesNotExist()
        composeRule
            .onNodeWithText(ctx.getString(AutomationR.string.automation_screenoverlay_explanation_tv))
            .assertExists()
    }
}
