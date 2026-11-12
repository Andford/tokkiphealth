package mx.loperstack.tokkiphealth.appcleaner.core

import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError
import mx.loperstack.tokkiphealth.setup.SetupModule
import mx.loperstack.tokkiphealth.setup.SetupRoute
import mx.loperstack.tokkiphealth.setup.SetupScreenOptions

class InaccessibleDeletionException(
    override val cause: Throwable
) : IllegalStateException(), HasLocalizedError {

    override fun getLocalizedError(): LocalizedError = LocalizedError(
        throwable = this,
        label = mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_automation_unavailable_title.toCaString(),
        description = caString {
            val sb = StringBuilder()
            sb.append(it.getString(mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_automation_unavailable_body))
            if (cause is HasLocalizedError) {
                sb.append("\n\n")
                sb.append(cause.getLocalizedError().description.get(it))
            }
            sb.toString()
        },
        fixActionLabel = mx.loperstack.tokkiphealth.common.R.string.setup_title.toCaString(),
        fixActionRoute = SetupRoute(
            options = SetupScreenOptions(
                isOnboarding = false,
                showCompleted = true,
                typeFilter = setOf(SetupModule.Type.AUTOMATION),
            ),
        ),
    )

}
