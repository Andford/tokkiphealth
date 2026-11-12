package mx.loperstack.tokkiphealth.squeezer.core.tasks

import android.os.Parcelable
import mx.loperstack.tokkiphealth.squeezer.R
import mx.loperstack.tokkiphealth.common.ByteFormatter
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.getQuantityString2
import mx.loperstack.tokkiphealth.squeezer.core.CompressibleMedia
import mx.loperstack.tokkiphealth.squeezer.core.FailureReason
import mx.loperstack.tokkiphealth.stats.core.AffectedPath
import mx.loperstack.tokkiphealth.stats.core.ReportDetails
import mx.loperstack.tokkiphealth.stats.core.Reportable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class SqueezerProcessTask(
    val mode: TargetMode = TargetMode.All(),
    val qualityOverride: Int? = null,
) : SqueezerTask, Reportable {

    sealed interface TargetMode : Parcelable {

        @Parcelize
        data class All(
            val id: UUID = UUID.randomUUID(),
        ) : TargetMode

        @Parcelize
        data class Selected(
            val targets: Set<CompressibleMedia.Id>,
        ) : TargetMode
    }

    sealed interface Result : SqueezerTask.Result

    @Parcelize
    data class Success(
        override val affectedSpace: Long,
        override val affectedPaths: Set<APath>,
        val processedCount: Int,
        val failedCount: Int = 0,
        val failureReasons: Map<FailureReason, Int> = emptyMap(),
        /**
         * Photos skipped at process time because a protection opt-in (HDR/depth, Motion Photo,
         * oversized) is off; the guard re-checks the settings per item. Without this the items
         * just vanish from the pending list, counted neither as processed nor failed.
         */
        val guardSkippedCount: Int = 0,
        /** Files re-encoded without a smaller result, so the original was kept. Not in [processedCount]. */
        val unchangedCount: Int = 0,
    ) : Result, ReportDetails.AffectedSpace, ReportDetails.AffectedPaths {

        /** Files aborted to preserve metadata (part of [failedCount], surfaced as their own clause). */
        val metadataUnpreservableCount: Int
            get() = failureReasons[FailureReason.METADATA_UNPRESERVABLE] ?: 0

        /** [failedCount] minus [metadataUnpreservableCount] so a metadata abort isn't counted twice. */
        val genericFailedCount: Int
            get() = (failedCount - metadataUnpreservableCount).coerceAtLeast(0)

        override val action: AffectedPath.Action
            get() = AffectedPath.Action.COMPRESSED

        override val primaryInfo
            get() = caString {
                getQuantityString2(R.plurals.squeezer_result_x_images_compressed, processedCount)
            }

        override val secondaryInfo
            get() = caString {
                val (formatted, quantity) = ByteFormatter.formatSize(this, affectedSpace)
                val fragments = mutableListOf(
                    getQuantityString2(
                        mx.loperstack.tokkiphealth.common.R.plurals.general_result_x_space_freed,
                        quantity,
                        formatted,
                    ),
                )
                // Metadata-preservation aborts are part of failedCount; surface them as their own,
                // honest clause and exclude them from the generic "failed" count (see the count
                // properties above) so a single such file isn't shown twice.
                if (genericFailedCount > 0) {
                    fragments += getQuantityString2(R.plurals.squeezer_result_x_items_failed, genericFailedCount)
                }
                if (metadataUnpreservableCount > 0) {
                    fragments += getQuantityString2(
                        R.plurals.squeezer_result_x_metadata_unpreservable,
                        metadataUnpreservableCount,
                    )
                }
                if (unchangedCount > 0) {
                    fragments += getQuantityString2(
                        R.plurals.squeezer_result_x_unchanged_not_smaller,
                        unchangedCount,
                    )
                }
                if (guardSkippedCount > 0) {
                    fragments += getQuantityString2(
                        R.plurals.squeezer_result_x_skipped_protected,
                        guardSkippedCount,
                    )
                }
                fragments.joinToString(" • ")
            }
    }
}
