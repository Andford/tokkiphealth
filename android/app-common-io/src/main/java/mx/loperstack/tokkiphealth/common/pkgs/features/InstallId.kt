package mx.loperstack.tokkiphealth.common.pkgs.features

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.user.UserHandle2
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class InstallId(
    val pkgId: Pkg.Id,
    val userHandle: UserHandle2,
) : Parcelable