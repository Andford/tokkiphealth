package mx.loperstack.tokkiphealth.main.core.taskmanager

import mx.loperstack.tokkiphealth.R
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError
import mx.loperstack.tokkiphealth.main.core.SDMTool
import kotlin.time.Duration

class TaskTimeoutException(
    val toolType: SDMTool.Type,
    val timeout: Duration,
) : Exception("Task for $toolType timed out after $timeout"), HasLocalizedError {

    override fun getLocalizedError() = LocalizedError(
        throwable = this,
        label = R.string.tasks_timeout_error_label.toCaString(),
        description = R.string.tasks_timeout_error_desc.toCaString(),
    )
}
