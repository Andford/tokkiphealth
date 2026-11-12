package mx.loperstack.tokkiphealth.appcleaner.core.automation

import mx.loperstack.tokkiphealth.automation.core.AutomationTask
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId

class ClearCacheTask(
    val targets: List<InstallId>,
    val returnToApp: Boolean,
    val isWholeToolClean: Boolean,
    val onSuccess: (InstallId) -> Unit,
    val onError: (InstallId, Exception) -> Unit,
) : AutomationTask {

    data class Result(
        val successful: Collection<InstallId>,
        val failed: Map<InstallId, Exception>,
    ) : AutomationTask.Result
}