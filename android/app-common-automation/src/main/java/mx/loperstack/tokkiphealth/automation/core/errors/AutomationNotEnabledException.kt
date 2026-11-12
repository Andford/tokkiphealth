package mx.loperstack.tokkiphealth.automation.core.errors

import mx.loperstack.tokkiphealth.automation.R
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

open class AutomationNotEnabledException(
    override val message: String = "Accessibility service is not enabled"
) : AutomationUnavailableException(), HasLocalizedError {

    override fun getLocalizedError(): LocalizedError = LocalizedError(
        throwable = this,
        label = R.string.automation_error_not_enabled_title.toCaString(),
        description = R.string.automation_error_not_enabled_body.toCaString(),
    )

}