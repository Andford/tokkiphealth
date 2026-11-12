package mx.loperstack.tokkiphealth.automation.core.errors

import mx.loperstack.tokkiphealth.automation.R
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

/**
 * The app's settings page exists but can't be opened while the app is disabled.
 * Unlike [NoSettingsWindowException] this is recoverable: enabling the app, or using
 * root/ADB instead of the accessibility service, makes the cache clearable again.
 */
class DisabledAppException(
    message: String,
) : PlanAbortException(message), HasLocalizedError {
    override fun getLocalizedError() = LocalizedError(
        throwable = this,
        label = R.string.automation_error_disabled_app_title.toCaString(),
        description = R.string.automation_error_disabled_app_body.toCaString()
    )
}
