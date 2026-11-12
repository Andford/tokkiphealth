package mx.loperstack.tokkiphealth.appcleaner.core.automation.errors

import mx.loperstack.tokkiphealth.appcleaner.R
import mx.loperstack.tokkiphealth.automation.core.errors.PlanAbortException
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

class LockedAppCacheException(
    message: String,
) : PlanAbortException(message), HasLocalizedError {
    override fun getLocalizedError() = LocalizedError(
        throwable = this,
        label = R.string.appcleaner_automation_error_locked_appcache_title.toCaString(),
        description = R.string.appcleaner_automation_error_locked_appcache_body.toCaString()
    )
}
