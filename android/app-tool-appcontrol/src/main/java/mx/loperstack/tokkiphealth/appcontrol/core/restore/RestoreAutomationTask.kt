package mx.loperstack.tokkiphealth.appcontrol.core.restore

import mx.loperstack.tokkiphealth.automation.core.AutomationTask
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId

class RestoreAutomationTask(
    val targets: List<InstallId>,
) : AutomationTask {

    data class Result(
        val successful: Collection<InstallId>,
        val failed: Map<InstallId, Exception>,
    ) : AutomationTask.Result
}
