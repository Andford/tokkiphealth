package mx.loperstack.tokkiphealth.automation.core.errors

import mx.loperstack.tokkiphealth.automation.R
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

class AutomationSchedulerException(
    override val cause: Throwable,
) : AutomationException(cause = cause), HasLocalizedError {
    override fun getLocalizedError() = LocalizedError(
        throwable = this,
        label = R.string.automation_error_scheduler_title.toCaString(),
        description = caString {
            val sb = StringBuilder(it.getString(R.string.automation_error_scheduler_body))
            if (cause is HasLocalizedError) {
                sb.append("\n\n")
                sb.append((cause as HasLocalizedError).getLocalizedError().description.get(it))
            }
            sb.toString()
        },
    )
}
