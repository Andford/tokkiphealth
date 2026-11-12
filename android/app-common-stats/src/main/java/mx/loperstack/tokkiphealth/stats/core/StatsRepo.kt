package mx.loperstack.tokkiphealth.stats.core

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.error.localized
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.flow.shareLatest
import mx.loperstack.tokkiphealth.common.flow.throttleLatest
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskSubmitter
import mx.loperstack.tokkiphealth.stats.core.db.ReportEntity
import mx.loperstack.tokkiphealth.stats.core.db.ReportsDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepo @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    @ApplicationContext private val context: Context,
    private val reportsDatabase: ReportsDatabase,
    private val statsSettings: StatsSettings,
    private val spaceTracker: SpaceTracker,
) {

    val reports: Flow<Collection<Report>>
        get() = reportsDatabase.reports

    data class State(
        val reportsCount: Int,
        val snapshotsCount: Int,
        val totalSpaceFreed: Long,
        val itemsProcessed: Long,
        val databaseSize: Long,
    ) {
        val isEmpty: Boolean
            get() = reportsCount == 0 && snapshotsCount == 0 && totalSpaceFreed == 0L && itemsProcessed == 0L
    }

    val state = mx.loperstack.tokkiphealth.common.flow.combine(
        reportsDatabase.reportCount,
        reportsDatabase.snapshotsCount,
        statsSettings.totalSpaceFreed.flow,
        statsSettings.totalItemsProcessed.flow,
        reportsDatabase.databaseSize,
    ) { reportsCount, snapshotsCount, spaceFreed, itemsProcessed, databaseSize ->
        State(
            reportsCount = reportsCount,
            snapshotsCount = snapshotsCount,
            totalSpaceFreed = spaceFreed,
            itemsProcessed = itemsProcessed,
            databaseSize = databaseSize,
        )
    }
        .throttleLatest(500)
        .shareLatest(appScope)

    suspend fun report(task: TaskSubmitter.ManagedTask) {
        log(TAG, INFO) { "report(${task.id})...${task.task.javaClass}" }
        if (task.task !is Reportable) {
            log(TAG) { "report(${task.id}): Not reportable" }
            return
        }

        if (Bugs.isDebug) log(TAG) { "report(${task.id})...$task" }

        val reportDetails = (task.result as? ReportDetails)
        val report = ReportEntity(
            startAt = task.startedAt ?: Instant.now(),
            endAt = task.completedAt ?: Instant.now(),
            tool = task.toolType,
            status = when {
                // A task that failed but still handed back a result got part of its work done.
                task.error != null && reportDetails != null -> Report.Status.PARTIAL_SUCCESS
                task.error != null -> Report.Status.FAILURE
                reportDetails != null -> reportDetails.status
                else -> Report.Status.SUCCESS
            },
            primaryMessage = task.result?.primaryInfo?.get(context),
            secondaryMessage = task.result?.secondaryInfo?.get(context),
            errorMessage = task.error?.localized(context)?.asText()?.get(context),
            affectedCount = reportDetails?.affectedCount,
            affectedSpace = reportDetails?.affectedSpace,
            extra = null,
        )
        val reportsRetention = statsSettings.retentionReports.value()
        if (reportsRetention <= Duration.ZERO) {
            log(TAG) { "report(${task.id}): Retention for reports is $reportsRetention, not storing it" }
        } else {
            val pathsRetention = statsSettings.retentionPaths.value()
            reportsDatabase.withTransaction {
                reportsDatabase.addReport(report)

                (reportDetails as? ReportDetails.AffectedPaths)?.let { pathsReport ->
                    if (pathsRetention <= Duration.ZERO) {
                        log(TAG) { "report(${task.id}): Retention for affected files is $pathsRetention, skipping" }
                    } else {
                        log(TAG) { "report(${task.id}): Saving details about ${pathsReport.affectedPaths.size} files" }
                        val affectedPaths = pathsReport.affectedPaths
                            .map { it.toAffectedPath(report.reportId, pathsReport.action) }
                        reportsDatabase.addPaths(affectedPaths)
                    }
                }

                reportDetails?.affectedPkgs?.let { pkgs ->
                    log(TAG) { "report(${task.id}): Saving details about affected pkgs: ${pkgs.size}" }
                    val affectedPkgs = pkgs.toAffectedPkgs(report.reportId)
                    reportsDatabase.addPkgs(affectedPkgs)
                }
            }
        }

        report.affectedSpace?.let { affected ->
            log(TAG) { "report(${task.id}): Saving details about affected space: $affected" }
            statsSettings.totalSpaceFreed.update { it + affected }
        }
        report.affectedCount?.let { affected ->
            log(TAG) { "report(${task.id}): Saving details about affected items: $affected" }
            statsSettings.totalItemsProcessed.update { it + affected }
        }

        reportsDatabase.applyRetention()
    }

    suspend fun recordSnapshot(force: Boolean = false) {
        spaceTracker.recordSnapshot(force = force)
    }

    suspend fun resetAll() = withContext(NonCancellable) {
        log(TAG, INFO) { "resetAll()" }
        statsSettings.totalItemsProcessed.value(0L)
        statsSettings.totalSpaceFreed.value(0L)
        statsSettings.lastSnapshotAt.value(0L)
        reportsDatabase.clear()
    }

    private fun APath.toAffectedPath(
        id: ReportId,
        action: AffectedPath.Action = AffectedPath.Action.DELETED
    ) = object : AffectedPath {
        override val reportId: ReportId = id
        override val action: AffectedPath.Action = action
        override val path: APath = this@toAffectedPath
    }

    private fun Map<Pkg.Id, AffectedPkg.Action>.toAffectedPkgs(
        id: ReportId,
    ) = this.map { (pkgId, action) ->
        object : AffectedPkg {
            override val reportId: ReportId = id
            override val action: AffectedPkg.Action = action
            override val pkgId: Pkg.Id = pkgId
        }
    }

    suspend fun getById(id: ReportId): Report? {
        log(TAG) { "getById($id)" }
        return reportsDatabase.getReport(id)
    }

    /** First report for [tool] at/after [since] — used to deep-link the dashboard freed-hero chips. */
    suspend fun getReportForToolSince(tool: SDMTool.Type, since: Instant): Report? {
        log(TAG) { "getReportForToolSince($tool, $since)" }
        return reportsDatabase.getReportForToolSince(tool, since)
    }

    suspend fun getAffectedPaths(id: ReportId): Collection<AffectedPath> {
        log(TAG) { "getAffectedPaths($id)" }
        return reportsDatabase.getAffectedPaths(id)
    }

    suspend fun getAffectedPkgs(id: ReportId): Collection<AffectedPkg> {
        log(TAG) { "getAffectedPkgs($id)" }
        return reportsDatabase.getAffectedPkgs(id)
    }

    companion object {
        private val TAG = logTag("Stats", "Repo")
    }
}