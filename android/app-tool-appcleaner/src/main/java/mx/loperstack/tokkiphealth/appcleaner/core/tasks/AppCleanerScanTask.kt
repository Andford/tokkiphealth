package mx.loperstack.tokkiphealth.appcleaner.core.tasks

import mx.loperstack.tokkiphealth.appcleaner.R
import mx.loperstack.tokkiphealth.common.ByteFormatter
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.getQuantityString2
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppCleanerScanTask(
    val pkgIdFilter: Set<Pkg.Id> = emptySet(),
) : AppCleanerTask {

    sealed interface Result : AppCleanerTask.Result

    @Parcelize
    data class Success(
        private val itemCount: Int,
        private val recoverableSpace: Long,
    ) : Result {
        override val primaryInfo
            get() = caString {
                getQuantityString2(R.plurals.appcleaner_result_x_items_found, itemCount)
            }

        override val secondaryInfo
            get() = caString {
                val (formatted, quantity) = ByteFormatter.formatSize(this, recoverableSpace)
                getQuantityString2(
                    mx.loperstack.tokkiphealth.common.R.plurals.x_space_can_be_freed,
                    quantity,
                    formatted,
                )
            }
    }
}