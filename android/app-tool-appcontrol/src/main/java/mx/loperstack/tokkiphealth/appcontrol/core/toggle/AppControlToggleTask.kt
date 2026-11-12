package mx.loperstack.tokkiphealth.appcontrol.core.toggle

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
data class AppControlToggleTask(
    val targets: Set<InstallId> = emptySet(),
) : AppControlTask, Reportable {

    @Parcelize
    data class Result(
        private val enabled: Set<InstallId>,
        private val disabled: Set<InstallId>,
        private val failed: Set<InstallId>,
        private val skipped: Set<InstallId> = emptySet(),
    ) : AppControlTask.Result, ReportDetails.AffectedPkgs {

        override val affectedPkgs: Map<Pkg.Id, AffectedPkg.Action>
            get() = enabled.associate { it.pkgId to AffectedPkg.Action.ENABLED } + disabled.associate { it.pkgId to AffectedPkg.Action.DISABLED }

        // Composition is pinned by AppControlTest."primaryInfo carries every outcome of a mixed selection"
        override val primaryInfo: CaString
            get() = caString {
                val toggled = enabled.size + disabled.size
                val clauses = listOfNotNull(
                    toggled.takeIf { it > 0 }?.let {
                        getQuantityString2(mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_toggle_result_message_x, it)
                    },
                    failed.size.takeIf { it > 0 }?.let {
                        getQuantityString2(mx.loperstack.tokkiphealth.common.R.plurals.result_x_failed, it)
                    },
                    skipped.size.takeIf { it > 0 }?.let {
                        getQuantityString2(mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_toggle_result_skipped_x, it)
                    },
                )
                when {
                    clauses.isEmpty() -> getQuantityString2(
                        mx.loperstack.tokkiphealth.appcontrol.R.plurals.appcontrol_toggle_result_message_x,
                        toggled,
                    )

                    else -> clauses.joinToString(", ")
                }
            }

        override val secondaryInfo: CaString?
            get() = failed.takeIf { it.isNotEmpty() }?.let {
                caString {
                    getQuantityString2(mx.loperstack.tokkiphealth.common.R.plurals.result_x_failed, failed.size)
                }
            }
    }
}