package mx.loperstack.tokkiphealth.swiper.core.tasks

import mx.loperstack.tokkiphealth.common.ByteFormatter
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.getQuantityString2
import mx.loperstack.tokkiphealth.stats.core.ReportDetails
import mx.loperstack.tokkiphealth.stats.core.Reportable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SwiperDeleteTask(
    val sessionId: String,
) : SwiperTask, Reportable {

    sealed interface Result : SwiperTask.Result

    @Parcelize
    data class Success(
        override val affectedSpace: Long,
        override val affectedPaths: Set<APath>,
    ) : Result, ReportDetails.AffectedSpace, ReportDetails.AffectedPaths {

        override val primaryInfo
            get() = caString {
                getQuantityString2(
                    mx.loperstack.tokkiphealth.swiper.R.plurals.swiper_result_x_items_deleted,
                    affectedCount,
                )
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

    @Parcelize
    data class PartialSuccess(
        override val affectedSpace: Long,
        override val affectedPaths: Set<APath>,
        val failedCount: Int,
    ) : Result, ReportDetails.AffectedSpace, ReportDetails.AffectedPaths {

        override val primaryInfo
            get() = caString {
                getQuantityString2(
                    mx.loperstack.tokkiphealth.swiper.R.plurals.swiper_result_x_items_deleted,
                    affectedCount,
                )
            }

        override val secondaryInfo
            get() = caString {
                getQuantityString2(
                    mx.loperstack.tokkiphealth.swiper.R.plurals.swiper_result_x_items_failed,
                    failedCount,
                    failedCount,
                )
            }
    }
}
