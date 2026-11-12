package mx.loperstack.tokkiphealth.main.core.taskmanager

import mx.loperstack.tokkiphealth.appcleaner.core.tasks.AppCleanerSchedulerTask
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderSchedulerTask
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.scheduler.core.SchedulerSettings
import mx.loperstack.tokkiphealth.systemcleaner.core.tasks.SystemCleanerSchedulerTask
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchedulerTaskFactoryImpl @Inject constructor(
    private val schedulerSettings: SchedulerSettings,
) : SchedulerTaskFactory {

    override suspend fun createTasks(
        scheduleId: String,
        enabledTools: Set<SDMTool.Type>,
    ): List<SDMTool.Task> = enabledTools.mapNotNull { type ->
        when (type) {
            SDMTool.Type.CORPSEFINDER -> CorpseFinderSchedulerTask(scheduleId)
            SDMTool.Type.SYSTEMCLEANER -> SystemCleanerSchedulerTask(scheduleId)
            SDMTool.Type.APPCLEANER -> {
                val useAutomation = schedulerSettings.useAutomation.value()
                AppCleanerSchedulerTask(scheduleId, useAutomation = useAutomation)
            }
            else -> null
        }
    }
}
