package mx.loperstack.tokkiphealth.stats.core

import mx.loperstack.tokkiphealth.common.pkgs.Pkg

interface AffectedPkg {
    val reportId: ReportId
    val action: Action
    val pkgId: Pkg.Id

    enum class Action {
        EXPORTED,
        STOPPED,
        ENABLED,
        DISABLED,
        DELETED,
        ARCHIVED,
        RESTORED,
        ;
    }
}
