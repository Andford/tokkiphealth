package mx.loperstack.tokkiphealth.automation.core.errors

open class InvalidSystemStateException(
    message: String,
) : PlanAbortException(message)
