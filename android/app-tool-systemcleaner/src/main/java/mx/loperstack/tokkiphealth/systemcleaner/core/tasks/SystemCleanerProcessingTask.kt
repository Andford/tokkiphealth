package mx.loperstack.tokkiphealth.systemcleaner.core.tasks

import mx.loperstack.tokkiphealth.systemcleaner.R
import mx.loperstack.tokkiphealth.common.ByteFormatter
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.getQuantityString2
import mx.loperstack.tokkiphealth.stats.core.ReportDetails
import mx.loperstack.tokkiphealth.stats.core.Reportable
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.FilterIdentifier
import kotlinx.parcelize.Parcelize

@Parcelize
data class SystemCleanerProcessingTask(
    val targetFilters: Set<FilterIdentifier>? = null,
    val targetContent: Set<APath>? = null,
) : SystemCleanerTask, Reportable {

    sealed interface Result : SystemCleanerTask.Result

    @Parcelize
    data class Success(
        override val affectedSpace: Long,
        override val affectedPaths: Set<APath>
    ) : Result, ReportDetails.AffectedSpace, ReportDetails.AffectedPaths {

        override val primaryInfo
            get() = caString {
                getQuantityString2(R.plurals.systemcleaner_result_x_items_deleted, affectedCount)
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