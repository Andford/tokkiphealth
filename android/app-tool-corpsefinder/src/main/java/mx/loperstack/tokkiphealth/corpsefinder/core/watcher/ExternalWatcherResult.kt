package mx.loperstack.tokkiphealth.corpsefinder.core.watcher

import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.pkgs.Pkg

interface ExternalWatcherResult {
    val pkgId: Pkg.Id

    data class Scan(
        override val pkgId: Pkg.Id,
        val foundItems: Int,
    ) : ExternalWatcherResult

    data class Deletion(
        val appName: CaString?,
        override val pkgId: Pkg.Id,
        val deletedItems: Int,
        val freedSpace: Long,
    ) : ExternalWatcherResult
}