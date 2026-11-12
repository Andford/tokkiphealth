package mx.loperstack.tokkiphealth.automation.core.common.stepper

import mx.loperstack.tokkiphealth.automation.core.common.ACSNodeInfo
import mx.loperstack.tokkiphealth.common.ca.CaString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class AutomationStep(
    val source: String,
    val descriptionInternal: String,
    val label: CaString,
    val windowLaunch: (suspend StepContext.() -> Unit)? = null,
    val windowCheck: (suspend StepContext.() -> ACSNodeInfo)? = null,
    val nodeRecovery: (suspend StepContext.(ACSNodeInfo) -> Boolean)? = null,
    val nodeAction: (suspend StepContext.() -> Boolean)? = null,
    val timeout: Duration = 40.seconds, // Inner attempt timeout is 15s, executionTimeout is 30s
) {
    override fun toString(): String = "Step(source=$source, description=$descriptionInternal)"
}