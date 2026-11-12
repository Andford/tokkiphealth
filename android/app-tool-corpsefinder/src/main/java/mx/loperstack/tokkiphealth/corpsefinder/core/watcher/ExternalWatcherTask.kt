package mx.loperstack.tokkiphealth.corpsefinder.core.watcher

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import kotlinx.parcelize.Parcelize

sealed interface ExternalWatcherTask : Parcelable {
    @Parcelize
    data class Delete(
        val target: Pkg.Id
    ) : ExternalWatcherTask
}