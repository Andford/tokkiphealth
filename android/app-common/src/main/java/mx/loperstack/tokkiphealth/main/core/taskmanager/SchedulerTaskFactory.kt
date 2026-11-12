package mx.loperstack.tokkiphealth.main.core.taskmanager

import mx.loperstack.tokkiphealth.main.core.SDMTool

fun interface SchedulerTaskFactory {
    suspend fun createTasks(
        scheduleId: String,
        enabledTools: Set<SDMTool.Type>,
    ): List<SDMTool.Task>
}
