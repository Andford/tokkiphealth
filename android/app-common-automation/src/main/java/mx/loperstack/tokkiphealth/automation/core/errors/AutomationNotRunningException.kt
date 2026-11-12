package mx.loperstack.tokkiphealth.automation.core.errors

import mx.loperstack.tokkiphealth.automation.R
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

open class AutomationNotRunningException(
    override val message: String = "Accessibility service isn't running"
) : AutomationUnavailableException(), HasLocalizedError {
    override fun getLocalizedError(): LocalizedError = LocalizedError(
        throwable = this,
        label = R.string.automation_error_not_running_title.toCaString(),
        description = R.string.automation_error_not_running_body.toCaString(),
    )
}