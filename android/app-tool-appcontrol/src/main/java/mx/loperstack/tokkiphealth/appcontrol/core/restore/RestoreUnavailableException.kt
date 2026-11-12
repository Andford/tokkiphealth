package mx.loperstack.tokkiphealth.appcontrol.core.restore

import mx.loperstack.tokkiphealth.appcontrol.R
import mx.loperstack.tokkiphealth.automation.core.errors.PlanAbortException
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

/**
 * The system settings show the Restore button for this app, but disabled.
 */
class RestoreUnavailableException(
    message: String,
) : PlanAbortException(message), HasLocalizedError {
    override fun getLocalizedError() = LocalizedError(
        throwable = this,
        label = R.string.appcontrol_restore_unavailable_title.toCaString(),
        description = R.string.appcontrol_restore_unavailable_body.toCaString(),
    )
}
