package mx.loperstack.tokkiphealth.stats.core

import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.pkgs.Pkg

val ReportDetails.affectedCount: Int?
    get() = (this as? ReportDetails.AffectedCount)?.affectedCount

val ReportDetails.affectedSpace: Long?
    get() = (this as? ReportDetails.AffectedSpace)?.affectedSpace

val ReportDetails.affectedPaths: Collection<APath>?
    get() = (this as? ReportDetails.AffectedPaths)?.affectedPaths

val ReportDetails.affectedPkgs: Map<Pkg.Id, AffectedPkg.Action>?
    get() = (this as? ReportDetails.AffectedPkgs)?.affectedPkgs