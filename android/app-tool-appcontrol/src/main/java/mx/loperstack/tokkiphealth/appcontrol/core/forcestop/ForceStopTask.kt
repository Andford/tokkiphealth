package mx.loperstack.tokkiphealth.appcontrol.core.forcestop

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
data class ForceStopTask(
    val targets: Set<InstallId> = emptySet(),
    // An explicit user decision for a specific app may target packages that automated
    // (multi-app) force-stops must skip, e.g. System UI.
    val allowOffLimits: Boolean = false,
) : AppControlTask, Reportable {

    @Parcelize
    data class Result(
        val success: Set<InstallId>,
        val failed: Set<InstallId>,
    ) : AppControlTask.Result, ReportDetails.AffectedPkgs {

        override val affectedPkgs: Map<Pkg.Id, AffectedPkg.Action>
            get() = success.associate { it.pkgId to AffectedPkg.Action.STOPPED }

        val isSuccess: Boolean
            get() = success.isNotEmpty() && failed.isEmpty()

        override val primaryInfo: CaString
            get() = caString {
                val clauses = listOfNotNull(
                    success.size.takeIf { it > 0 }?.let {
                        getQuantityString2(mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_force_stop_result_message_x, it)
                    },
                    failed.size.takeIf { it > 0 }?.let {
                        getQuantityString2(mx.loperstack.tokkiphealth.common.R.plurals.result_x_failed, it)
                    },
                )
                when {
                    clauses.isEmpty() -> getQuantityString2(
                        mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_force_stop_result_message_x,
                        success.size,
                    )

                    else -> clauses.joinToString(", ")
                }
            }
    }
}