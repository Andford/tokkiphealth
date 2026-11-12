package mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.hyperos

import android.content.Intent
import android.provider.Settings
import mx.loperstack.tokkiphealth.appcleaner.R
import mx.loperstack.tokkiphealth.automation.core.errors.InvalidSystemStateException
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

class SecurityCenterMissingPermissionException(
    message: String = "App `com.miui.securitycenter` is missing the GET_USAGE_STATS permission."
) : InvalidSystemStateException(message), HasLocalizedError {
    override fun getLocalizedError() = LocalizedError(
        throwable = this,
        label = R.string.appcleaner_automation_error_securitycenter_permission_title.toCaString(),
        description = R.string.appcleaner_automation_error_securitycenter_permission_body.toCaString(),
        fixActionLabel = mx.loperstack.tokkiphealth.common.R.string.general_grant_access_action.toCaString(),
        fixAction = {
            try {
                it.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (e: Exception) {
                log(TAG, ERROR) { "Failed to open usage access settings: ${e.asLog()}" }
            }
        },
        infoActionLabel = mx.loperstack.tokkiphealth.common.R.string.general_help_action.toCaString(),
        infoAction = { WebpageTool.open(it, "https://sdmse.darken.eu/appcleaner#commiuisecuritycenter") }
    )

    companion object {
        private val TAG = logTag("AppCleaner", "Automation", "HyperOS", "SecurityCenter")
    }
}
