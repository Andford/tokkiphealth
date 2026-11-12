package mx.loperstack.tokkiphealth.appcleaner.core.tasks

import mx.loperstack.tokkiphealth.appcleaner.R
import mx.loperstack.tokkiphealth.common.ByteFormatter
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.getQuantityString2
import mx.loperstack.tokkiphealth.stats.core.ReportDetails
import mx.loperstack.tokkiphealth.stats.core.Reportable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppCleanerOneClickTask(
    val shortcutMode: Boolean = false,
) : AppCleanerTask, Reportable {

    sealed interface Result : AppCleanerTask.Result

    @Parcelize
    data class Success(
        override val affectedSpace: Long,
        override val affectedPaths: Set<APath>,
        override val affectedCount: Int = affectedPaths.size,
        val stoppedEarly: AppCleanerTask.StopReason? = null,
        val skippedCount: Int = 0,
    ) : Result, ReportDetails.AffectedSpace, ReportDetails.AffectedPaths {

        override val primaryInfo
            get() = caString {
                when (stoppedEarly) {
                    AppCleanerTask.StopReason.SCREEN_UNAVAILABLE -> getQuantityString2(
                        R.plurals.appcleaner_result_x_items_deleted_stopped_screen,
                        affectedCount,
                    )

                    AppCleanerTask.StopReason.ERROR -> getQuantityString2(
                        R.plurals.appcleaner_result_x_items_deleted_stopped_error,
                        affectedCount,
                    )

                    AppCleanerTask.StopReason.AUTOMATION_NO_CONSENT -> getQuantityString2(
                        R.plurals.appcleaner_result_x_items_deleted_stopped_automation,
                        skippedCount,
                        getQuantityString2(R.plurals.appcleaner_result_x_items_deleted, affectedCount),
                        skippedCount,
                    )

                    null -> getQuantityString2(R.plurals.appcleaner_result_x_items_deleted, affectedCount)
                }
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