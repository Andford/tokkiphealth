package mx.loperstack.tokkiphealth.common.forensics

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.clutter.Marker
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.user.UserHandle2
import kotlinx.parcelize.Parcelize

@Parcelize
data class Owner(
    val pkgId: Pkg.Id,
    val userHandle: UserHandle2,
    val flags: Set<Marker.Flag> = emptySet(),
) : Parcelable {

    val installId: InstallId
        get() = InstallId(pkgId, userHandle)

    fun hasFlag(flag: Marker.Flag): Boolean = flags.contains(flag)

}