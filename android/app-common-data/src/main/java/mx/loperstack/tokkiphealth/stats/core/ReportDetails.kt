package mx.loperstack.tokkiphealth.stats.core

import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.pkgs.Pkg

interface ReportDetails {
    val status: Report.Status
        get() = Report.Status.SUCCESS

    interface AffectedSpace : ReportDetails {
        val affectedSpace: Long
    }

    interface AffectedCount : ReportDetails {
        val affectedCount: Int
    }

    interface AffectedPaths : ReportDetails, AffectedCount {
        val affectedPaths: Set<APath>

        val action: AffectedPath.Action
            get() = AffectedPath.Action.DELETED

        override val affectedCount: Int
            get() = affectedPaths.size
    }

    interface AffectedPkgs : ReportDetails, AffectedCount {
        val affectedPkgs: Map<Pkg.Id, AffectedPkg.Action>

        override val affectedCount: Int
            get() = affectedPkgs.size
    }
}