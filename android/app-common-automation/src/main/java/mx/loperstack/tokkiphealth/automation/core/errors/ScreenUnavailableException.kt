package mx.loperstack.tokkiphealth.automation.core.errors

import mx.loperstack.tokkiphealth.automation.R
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

class ScreenUnavailableException(
    message: String,
) : InvalidSystemStateException(message), HasLocalizedError {
    override fun getLocalizedError() = LocalizedError(
        throwable = this,
        label = R.string.automation_error_screen_unavailable_title.toCaString(),
        description = R.string.automation_error_screen_unavailable_body.toCaString()
    )
}
