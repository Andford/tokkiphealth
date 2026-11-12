package mx.loperstack.tokkiphealth.automation.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import mx.loperstack.tokkiphealth.automation.R
import mx.loperstack.tokkiphealth.automation.core.common.ACSNodeInfo
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.progress.Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface AutomationHost : Progress.Client {

    val service: AccessibilityService

    val scope: CoroutineScope

    suspend fun windowRoot(): ACSNodeInfo?

    suspend fun changeOptions(action: (Options) -> Options)

    val events: Flow<AutomationEvent>

    data class State(
        val hasOverlay: Boolean = false,
        val passthrough: Boolean = false,
    )

    val state: Flow<State>

    data class Options(
        val showOverlay: Boolean = false,
        val passthrough: Boolean = true,
        val accessibilityServiceInfo: AccessibilityServiceInfo = AccessibilityServiceInfo(),
        val controlPanelTitle: CaString = R.string.automation_active_title.toCaString(),
        val controlPanelSubtitle: CaString = mx.loperstack.tokkiphealth.common.R.string.general_progress_loading.toCaString(),
    ) {

        override fun toString(): String {
            val acsInfo = try {
                //    java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String android.content.ComponentName.flattenToShortString()' on a null object reference
                //    at android.accessibilityservice.AccessibilityServiceInfo.getId(AccessibilityServiceInfo.java:759)
                //    at android.accessibilityservice.AccessibilityServiceInfo.toString(AccessibilityServiceInfo.java:1105)
                accessibilityServiceInfo.toString()
            } catch (_: NullPointerException) {
                "NPE"
            }
            return "AutomationHost.Options(showOverlay=$showOverlay, passthrough=$passthrough, acsInfo=$acsInfo)"
        }
    }
}