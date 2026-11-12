package mx.loperstack.tokkiphealth.setup

import android.content.Intent
import mx.loperstack.tokkiphealth.common.permissions.Permission
import mx.loperstack.tokkiphealth.setup.automation.AutomationSetupModule
import mx.loperstack.tokkiphealth.setup.saf.SAFSetupModule

sealed interface SetupEvents {
    data class SafRequestAccess(
        val item: SAFSetupModule.Result.PathAccess,
    ) : SetupEvents

    data class SafWrongPathError(
        val exception: Exception
    ) : SetupEvents

    data class RuntimePermissionRequests(
        val item: Permission,
    ) : SetupEvents

    data class SpecialPermissionRequest(
        val item: Permission,
        val intent: Intent,
        val fallbackIntent: Intent?,
    ) : SetupEvents

    data class ConfigureAccessibilityService(
        val item: AutomationSetupModule.Result,
    ) : SetupEvents

    data class ShowOurDetailsPage(
        val intent: Intent,
    ) : SetupEvents
}
