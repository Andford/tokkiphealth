package mx.loperstack.tokkiphealth.appcontrol.core

import mx.loperstack.tokkiphealth.appcontrol.core.export.AppExportType
import mx.loperstack.tokkiphealth.appcontrol.core.usage.UsageInfo
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.isNotNullOrEmpty
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallDetails
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.features.SourceAvailable
import mx.loperstack.tokkiphealth.common.pkgs.isLibrary
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.user.UserProfile2
import java.time.Instant

data class AppInfo(
    val pkg: Installed,
    val isActive: Boolean?,
    val sizes: PkgOps.SizeStats?,
    val usage: UsageInfo?,
    val userProfile: UserProfile2?,
    val canBeToggled: Boolean,
    val canBeStopped: Boolean,
    val canBeExported: Boolean,
    val canBeDeleted: Boolean,
    val canBeArchived: Boolean,
    val canBeRestored: Boolean,
) {
    val label: CaString
        get() = pkg.label ?: pkg.packageName.toCaString()

    val id: Pkg.Id
        get() = pkg.id

    val installId: InstallId
        get() = pkg.installId

    val updatedAt: Instant?
        get() = (pkg as? InstallDetails)?.updatedAt

    val installedAt: Instant?
        get() = (pkg as? InstallDetails)?.installedAt

    /**
     * Version string for display, or null when it should be hidden.
     * Dynamic shared libraries have no version code (SharedLibraryInfo.longVersion == -1);
     * the backing package's versionName isn't meaningful for the library, so drop the whole line.
     */
    val versionText: String?
        get() = when {
            pkg.isLibrary && pkg.versionCode == -1L -> null
            else -> "${pkg.versionName ?: "?"} (${pkg.versionCode})"
        }

    val exportType: AppExportType
        get() = when {
            pkg is SourceAvailable && pkg.splitSources.isNotNullOrEmpty() -> AppExportType.BUNDLE
            pkg is SourceAvailable && pkg.sourceDir != null -> AppExportType.APK
            else -> AppExportType.NONE
        }
}
