package mx.loperstack.tokkiphealth.deduplicator.core.tasks

import mx.loperstack.tokkiphealth.deduplicator.R
import mx.loperstack.tokkiphealth.common.ByteFormatter
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.getQuantityString2
import kotlinx.parcelize.Parcelize

@Parcelize
data class DeduplicatorScanTask(
    val paths: Set<APath>? = null,
) : DeduplicatorTask {

    sealed interface Result : DeduplicatorTask.Result

    @Parcelize
    data class Success(
        private val itemCount: Int,
        private val recoverableSpace: Long,
    ) : Result {
        override val primaryInfo
            get() = caString {
                getQuantityString2(R.plurals.deduplicator_result_x_clusters_found, itemCount)
            }

        override val secondaryInfo
            get() = caString {
                val (text, quantity) = ByteFormatter.formatSize(this, recoverableSpace)
                getQuantityString2(
                    R.plurals.deduplicator_x_space_occupied_by_duplicates_msg,
                    quantity,
                    text,
                )
            }
    }
}