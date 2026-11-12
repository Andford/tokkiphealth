package mx.loperstack.tokkiphealth.systemcleaner.core.filter

import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError
import mx.loperstack.tokkiphealth.common.error.localized

class SystemCleanerFilterException(
    val filter: SystemCleanerFilter,
    override val cause: Throwable
) : Exception(), HasLocalizedError {

    override val message: String
        get() = "SystemCleaner filter failed: $filter"

    override fun getLocalizedError(): LocalizedError = LocalizedError(
        throwable = this,
        label = mx.loperstack.tokkiphealth.common.R.string.general_error_label.toCaString(),
        description = caString {
            val subReason = cause.localized(it).asText().get(it)
            "SystemCleaner filter failed: $filter.\n\n$subReason"
        }
    )
}