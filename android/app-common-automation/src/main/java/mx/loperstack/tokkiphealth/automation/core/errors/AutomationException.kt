package mx.loperstack.tokkiphealth.automation.core.errors

import androidx.annotation.Keep

@Keep
open class AutomationException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {
    constructor(message: String?) : this(message, null)
}