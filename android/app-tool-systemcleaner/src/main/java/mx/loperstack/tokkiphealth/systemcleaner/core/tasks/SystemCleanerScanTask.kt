package mx.loperstack.tokkiphealth.systemcleaner.core.tasks

import mx.loperstack.tokkiphealth.systemcleaner.R
import mx.loperstack.tokkiphealth.common.ByteFormatter
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.getQuantityString2
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.main.core.SDMTool
import kotlinx.parcelize.Parcelize

@Parcelize
data class SystemCleanerScanTask(
    val pkgIdFilter: Set<Pkg.Id> = emptySet(),
    val isWatcherTask: Boolean = false,
) : SystemCleanerTask {

    sealed interface Result : SystemCleanerTask.Result {
        override val type: SDMTool.Type get() = SDMTool.Type.SYSTEMCLEANER
    }

    @Parcelize
    data class Success(
        private val itemCount: Int,
        private val recoverableSpace: Long,
    ) : Result {
        override val primaryInfo
            get() = caString {
                getQuantityString2(R.plurals.systemcleaner_result_x_items_found, itemCount)
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