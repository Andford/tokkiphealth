package mx.loperstack.tokkiphealth.deduplicator.core.tasks

import android.os.Parcelable
import mx.loperstack.tokkiphealth.deduplicator.R
import mx.loperstack.tokkiphealth.common.ByteFormatter
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.getQuantityString2
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.stats.core.ReportDetails
import mx.loperstack.tokkiphealth.stats.core.Reportable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class DeduplicatorDeleteTask(
    val mode: TargetMode = TargetMode.All(),
) : DeduplicatorTask, Reportable {

    sealed interface TargetMode : Parcelable {

        @Parcelize
        data class All(
            val id: UUID = UUID.randomUUID(),
        ) : TargetMode

        @Parcelize
        data class Clusters(
            val deleteAll: Boolean = false,
            val targets: Set<Duplicate.Cluster.Id>,
        ) : TargetMode

        @Parcelize
        data class Groups(
            val deleteAll: Boolean = false,
            val targets: Set<Duplicate.Group.Id>,
        ) : TargetMode

        @Parcelize
        data class Duplicates(
            val targets: Set<Duplicate.Id>
        ) : TargetMode

        @Parcelize
        data class Directory(
            val clusterId: Duplicate.Cluster.Id,
            val targets: Set<Duplicate.Id>,
            val deleteAll: Boolean = false,
        ) : TargetMode
    }

    sealed interface Result : DeduplicatorTask.Result

    @Parcelize
    data class Success(
        override val affectedSpace: Long,
        override val affectedPaths: Set<APath>,
    ) : Result, ReportDetails.AffectedSpace, ReportDetails.AffectedPaths {

        override val primaryInfo
            get() = caString {
                getQuantityString2(R.plurals.deduplicator_result_x_clusters_processed, affectedCount)
            }

        override val secondaryInfo
            get() = caString {
                val (formatted, quantity) = ByteFormatter.formatSize(this, affectedSpace)
                getQuantityString2(
                    mx.loperstack.tokkiphealth.common.R.plurals.general_result_x_space_freed,
                    quantity,
                    formatted,
                )
            }
    }
}

val SDMTool.Task.isSingleDuplicateDelete: Boolean
    get() {
        if (this !is DeduplicatorDeleteTask) return false
        val mode = this.mode
        return mode is DeduplicatorDeleteTask.TargetMode.Duplicates && mode.targets.size == 1
    }