package mx.loperstack.tokkiphealth.appcontrol.core

import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppControlScanTask(
    val pkgIdFilter: Set<Pkg.Id> = emptySet(),
    val refreshPkgCache: Boolean,
    val loadInfoScreenTime: Boolean,
    val loadInfoSize: Boolean,
    val loadInfoActive: Boolean,
    val includeMultiUser: Boolean,
) : AppControlTask {

    @Parcelize
    data class Result(
        private val itemCount: Int,
    ) : AppControlTask.Result {
        override val primaryInfo: CaString
            get() = mx.loperstack.tokkiphealth.common.R.string.general_result_success_message.toCaString()
    }
}