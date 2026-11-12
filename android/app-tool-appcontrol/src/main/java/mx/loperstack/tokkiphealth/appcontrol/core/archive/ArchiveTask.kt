package mx.loperstack.tokkiphealth.appcontrol.core.archive

import mx.loperstack.tokkiphealth.appcontrol.core.AppControlTask
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.getQuantityString2
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.stats.core.AffectedPkg
import mx.loperstack.tokkiphealth.stats.core.ReportDetails
import mx.loperstack.tokkiphealth.stats.core.Reportable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ArchiveTask(
    val targets: Set<InstallId> = emptySet(),
) : AppControlTask, Reportable {

    @Parcelize
    data class Result(
        val success: Set<InstallId>,
        val failed: Set<InstallId>,
        val unavailable: Set<InstallId> = emptySet(),
    ) : AppControlTask.Result, ReportDetails.AffectedPkgs {

        override val affectedPkgs: Map<Pkg.Id, AffectedPkg.Action>
            get() = success.associate { it.pkgId to AffectedPkg.Action.ARCHIVED }

        override val primaryInfo: CaString
            get() = caString {
                val clauses = listOfNotNull(
                    success.size.takeIf { it > 0 }?.let {
                        getQuantityString2(mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_archive_result_x, it)
                    },
                    failed.size.takeIf { it > 0 }?.let {
                        getQuantityString2(mx.loperstack.tokkiphealth.common.R.plurals.result_x_failed, it)
                    },
                )
                when {
                    clauses.isEmpty() -> getQuantityString2(
                        mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_archive_result_x,
                        success.size,
                    )

                    else -> clauses.joinToString(", ")
                }
            }
    }
}
