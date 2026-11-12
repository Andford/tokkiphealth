package mx.loperstack.tokkiphealth.stats.core

import mx.loperstack.tokkiphealth.common.files.APath

interface AffectedPath {
    val reportId: ReportId
    val action: Action
    val path: APath

    enum class Action {
        DELETED,
        COMPRESSED,
        ;
    }
}
