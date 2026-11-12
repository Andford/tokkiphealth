package mx.loperstack.tokkiphealth.common.pkgs.pkgops.ipc

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import kotlinx.parcelize.Parcelize

@Parcelize
data class RunningPackagesResult(
    val pkgs: Set<InstallId>,
) : Parcelable