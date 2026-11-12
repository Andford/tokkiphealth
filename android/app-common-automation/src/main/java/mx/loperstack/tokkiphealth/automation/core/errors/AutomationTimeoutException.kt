package mx.loperstack.tokkiphealth.automation.core.errors

import mx.loperstack.tokkiphealth.automation.R
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError
import kotlinx.coroutines.TimeoutCancellationException

open class AutomationTimeoutException(
    cause: TimeoutCancellationException,
) : AutomationException(
    "Tokkip Health couldn't complete the necessary steps in time. The app may need adjusting for this device.",
    cause,
), HasLocalizedError {

    override fun getLocalizedError(): LocalizedError = LocalizedError(
        throwable = this,
        label = R.string.automation_error_timeout_title.toCaString(),
        description = R.string.automation_error_timeout_body.toCaString(),
        infoActionLabel = mx.loperstack.tokkiphealth.common.R.string.general_error_report_bug_action.toCaString(),
        infoAction = { WebpageTool.open(it, "https://sdmse.darken.eu/bugs") }
    )

}
