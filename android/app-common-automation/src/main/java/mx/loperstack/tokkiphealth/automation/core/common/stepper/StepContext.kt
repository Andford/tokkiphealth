package mx.loperstack.tokkiphealth.automation.core.common.stepper

import mx.loperstack.tokkiphealth.automation.core.specs.AutomationExplorer
import kotlin.time.ComparableTimeMark

data class StepContext(
    val hostContext: AutomationExplorer.Context,
    val tag: String,
    val stepAttempts: Int,
    val attemptDeadline: ComparableTimeMark? = null,
) : AutomationExplorer.Context by hostContext
