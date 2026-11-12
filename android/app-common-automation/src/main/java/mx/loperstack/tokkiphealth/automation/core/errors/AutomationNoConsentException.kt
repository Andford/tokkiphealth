package mx.loperstack.tokkiphealth.automation.core.errors

import mx.loperstack.tokkiphealth.automation.R
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError
import mx.loperstack.tokkiphealth.setup.SetupModule
import mx.loperstack.tokkiphealth.setup.SetupRoute
import mx.loperstack.tokkiphealth.setup.SetupScreenOptions

open class AutomationNoConsentException(
    override val message: String = "User has not consented to accessibility service being used",
) : AutomationUnavailableException(), HasLocalizedError {

    override fun getLocalizedError(): LocalizedError = LocalizedError(
        throwable = this,
        label = R.string.automation_error_no_consent_title.toCaString(),
        description = R.string.automation_error_no_consent_body.toCaString(),
        fixActionLabel = mx.loperstack.tokkiphealth.common.R.string.setup_title.toCaString(),
        fixActionRoute = SetupRoute(
            options = SetupScreenOptions(
                showCompleted = true,
                typeFilter = setOf(SetupModule.Type.AUTOMATION),
            ),
        ),
    )
}
