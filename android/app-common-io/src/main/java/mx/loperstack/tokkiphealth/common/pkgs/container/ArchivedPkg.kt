package mx.loperstack.tokkiphealth.common.pkgs.container

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ArchivedPackageInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.ca.cache
import mx.loperstack.tokkiphealth.common.io.R
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallDetails
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallerInfo
import mx.loperstack.tokkiphealth.common.pkgs.getIcon2
import mx.loperstack.tokkiphealth.common.pkgs.getLabel2
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.user.UserHandle2

@SuppressLint("NewApi")
data class ArchivedPkg(
    override val packageInfo: PackageInfo,
    override val userHandle: UserHandle2,
    override val installerInfo: InstallerInfo,
    private val archivedPackageInfo: ArchivedPackageInfo? = null,
) : Installed, InstallDetails {

    override val id: Pkg.Id
        get() = packageInfo.packageName.toPkgId()

    override val label: CaString = caString { context ->
        // Try to get label from ArchivedPackageInfo first (works for archived apps)
        archivedPackageInfo?.launcherActivities?.firstOrNull()?.label?.toString()
            ?: context.packageManager.getLabel2(id) // Fall back to standard PM API (may not work for archived apps)
            ?: id.name
    }.cache()

    override val icon: ((Context) -> Drawable)? = { context ->
        context.packageManager.getIcon2(id) ?: AppCompatResources.getDrawable(context, R.drawable.ic_archive_24)!!
    }

    override val isEnabled: Boolean
        get() = false

    override fun toString(): String = "ArchivedPkg(packageName=$packageName)"
}