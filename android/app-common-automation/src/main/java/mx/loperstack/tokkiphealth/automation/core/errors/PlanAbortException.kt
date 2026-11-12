package mx.loperstack.tokkiphealth.automation.core.errors

open class PlanAbortException(
    message: String,
    val treatAsSuccess: Boolean = false,
) : AutomationException(message)
