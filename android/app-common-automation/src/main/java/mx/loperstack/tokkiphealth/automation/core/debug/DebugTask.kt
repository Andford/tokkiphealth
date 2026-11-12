package mx.loperstack.tokkiphealth.automation.core.debug

import mx.loperstack.tokkiphealth.automation.core.AutomationTask
import java.util.UUID

data class DebugTask(
    val runId: UUID = UUID.randomUUID(),
) : AutomationTask {

    data class Result(val task: DebugTask) : AutomationTask.Result
}