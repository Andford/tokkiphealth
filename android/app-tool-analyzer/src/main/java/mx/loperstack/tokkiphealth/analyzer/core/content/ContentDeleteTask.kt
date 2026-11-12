package mx.loperstack.tokkiphealth.analyzer.core.content

import mx.loperstack.tokkiphealth.analyzer.core.AnalyzerTask
import mx.loperstack.tokkiphealth.common.ByteFormatter
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.getQuantityString2
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.storage.StorageId
import mx.loperstack.tokkiphealth.stats.core.ReportDetails
import mx.loperstack.tokkiphealth.stats.core.Reportable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContentDeleteTask(
    val storageId: StorageId,
    val groupId: ContentGroup.Id,
    val targetPkg: InstallId? = null,
    val targets: Set<APath>,
) : AnalyzerTask, Reportable {

    @Parcelize
    data class Result(
        override val affectedSpace: Long,
        override val affectedPaths: Set<APath>,
    ) : AnalyzerTask.Result, ReportDetails.AffectedSpace, ReportDetails.AffectedPaths {

        override val primaryInfo: CaString
            get() = caString {
                val itemText = getQuantityString2(
                    mx.loperstack.tokkiphealth.common.R.plurals.general_delete_success_deleted_x,
                    affectedPaths.size,
                )
                val spaceText = run {
                    val (spaceFormatted, spaceQuantity) = ByteFormatter.formatSize(this, affectedSpace)
                    getQuantityString2(
                        mx.loperstack.tokkiphealth.common.R.plurals.general_result_x_space_freed,
                        spaceQuantity,
                        spaceFormatted,
                    )
                }
                "$itemText $spaceText"
            }
    }
}