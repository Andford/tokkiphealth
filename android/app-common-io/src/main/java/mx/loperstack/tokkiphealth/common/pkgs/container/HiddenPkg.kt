package mx.loperstack.tokkiphealth.common.pkgs.container

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.ca.cache
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.io.R
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallDetails
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallerInfo
import mx.loperstack.tokkiphealth.common.pkgs.getLabel2
import mx.loperstack.tokkiphealth.common.pkgs.loadIconFromArchive
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.user.UserHandle2

data class HiddenPkg(
    override val packageInfo: PackageInfo,
    override val userHandle: UserHandle2,
    override val installerInfo: InstallerInfo = InstallerInfo(),
    val apkPath: APath? = null,
) : Installed, InstallDetails {

    override val id: Pkg.Id = packageInfo.packageName.toPkgId()

    override val isSystemApp: Boolean
        get() {
            val fromFlags = applicationInfo?.run { flags and ApplicationInfo.FLAG_SYSTEM != 0 }
            if (fromFlags == true) return true
            // getPackageArchiveInfo() does not populate FLAG_SYSTEM (same limitation LibraryPkg
            // documents), so for an archive-parsed instance the partition is the only signal.
            apkPath?.let { path -> return SYSTEM_PARTITIONS.any { path.path.startsWith(it) } }
            return fromFlags ?: true
        }

    /**
     * Whether the package manager has this installed for [userHandle] — this container covers both
     * "installed but hidden" and "not installed for this user". An [apkPath] instance was parsed from
     * an APK, whose flags carry a default user state rather than this user's, so it reports false.
     */
    val isInstalledForUser: Boolean
        get() = apkPath == null && applicationInfo?.let { it.flags and ApplicationInfo.FLAG_INSTALLED != 0 } == true

    override val label: CaString = caString { context ->
        context.packageManager.getLabel2(id) ?: id.name
    }.cache()

    override val icon: ((Context) -> Drawable)? = { context ->
        packageInfo.loadIconFromArchive(context.packageManager)
            ?: AppCompatResources.getDrawable(context, R.drawable.ic_default_app_icon_24)!!
    }

    override fun toString(): String = "HiddenPkg(packageName=$packageName, userHandle=$userHandle)"

    companion object {
        private val SYSTEM_PARTITIONS = listOf(
            "/system/",
            "/system_ext/",
            "/product/",
            "/vendor/",
            "/odm/",
            "/apex/",
        )
    }
}
