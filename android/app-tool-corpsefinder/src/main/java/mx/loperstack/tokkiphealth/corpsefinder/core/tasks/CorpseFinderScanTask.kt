package mx.loperstack.tokkiphealth.corpsefinder.core.tasks

import mx.loperstack.tokkiphealth.corpsefinder.R
import mx.loperstack.tokkiphealth.common.ByteFormatter
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.getQuantityString2
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import kotlinx.parcelize.Parcelize

@Parcelize
data class CorpseFinderScanTask(
    val pkgIdFilter: Set<Pkg.Id> = emptySet(),
) : CorpseFinderTask {

    sealed interface Result : CorpseFinderTask.Result

    @Parcelize
    data class Success(
        private val itemCount: Int,
        private val recoverableSpace: Long,
    ) : Result {
        override val primaryInfo
            get() = caString {
                getQuantityString2(R.plurals.corpsefinder_result_x_corpses_found, itemCount)
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