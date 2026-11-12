package mx.loperstack.tokkiphealth.main.ui.dashboard

import android.content.Intent
import mx.loperstack.tokkiphealth.appcleaner.core.tasks.AppCleanerProcessingTask
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderDeleteTask
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import mx.loperstack.tokkiphealth.deduplicator.core.tasks.DeduplicatorDeleteTask
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.systemcleaner.core.tasks.SystemCleanerProcessingTask

sealed interface DashboardEvents {

    data object TodoHint : DashboardEvents
    data object SetupDismissHint : DashboardEvents

    data class CorpseFinderDeleteConfirmation(
        val task: CorpseFinderDeleteTask,
    ) : DashboardEvents

    data class SystemCleanerDeleteConfirmation(
        val task: SystemCleanerProcessingTask,
    ) : DashboardEvents

    data class AppCleanerDeleteConfirmation(
        val task: AppCleanerProcessingTask,
    ) : DashboardEvents

    data class DeduplicatorDeleteConfirmation(
        val task: DeduplicatorDeleteTask,
        val clusters: List<Duplicate.Cluster>? = null,
    ) : DashboardEvents

    data class TaskResult(
        val result: SDMTool.Task.Result
    ) : DashboardEvents

    data class OpenIntent(val intent: Intent) : DashboardEvents

    data object SqueezerSetup : DashboardEvents

    data object ShowShortRecordingWarning : DashboardEvents

    data class ShowUnknownFolders(
        val unknownPaths: List<String>,
        val scannedCount: Int,
        val skippedCount: Int,
    ) : DashboardEvents

}
