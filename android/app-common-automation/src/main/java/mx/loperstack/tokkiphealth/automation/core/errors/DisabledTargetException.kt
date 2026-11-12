package mx.loperstack.tokkiphealth.automation.core.errors

open class DisabledTargetException(message: String?, cause: Throwable?) : AutomationException(message, cause) {
    constructor(message: String?) : this(message, null)
}