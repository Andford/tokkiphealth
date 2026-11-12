package mx.loperstack.tokkiphealth.appcleaner.ui.preview

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import mx.loperstack.tokkiphealth.appcleaner.core.AppJunk
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.ExpendablesFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter.DefaultCachesPublicFilter
import mx.loperstack.tokkiphealth.appcleaner.core.scanner.InaccessibleCache
import mx.loperstack.tokkiphealth.appcleaner.ui.list.AppCleanerListViewModel
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.files.FileType
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.files.local.LocalPathLookup
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.user.UserHandle2
import java.time.Instant

private class PreviewInstalled(
    private val pkgName: String,
    private val displayLabel: String,
    override val userHandle: UserHandle2 = UserHandle2(handleId = 0),
) : Installed {
    override val packageInfo: PackageInfo = PackageInfo().apply {
        packageName = pkgName
        versionName = "1.0.0"
    }
    override val label: CaString = displayLabel.toCaString()
    override val icon: ((Context) -> Drawable)? = null
}

internal fun previewInstalled(
    pkgName: String = "com.example.app",
    label: String = "Example App",
): Installed = PreviewInstalled(pkgName = pkgName, displayLabel = label)

private fun previewLookup(
    pathSegments: Array<String>,
    fileType: FileType = FileType.FILE,
    size: Long = 1024L * 1024,
): LocalPathLookup = LocalPathLookup(
    lookedUp = LocalPath.build(*pathSegments),
    fileType = fileType,
    size = size,
    modifiedAt = Instant.parse("2026-04-01T12:00:00Z"),
    target = null,
)

internal fun previewExpendables(): Map<kotlin.reflect.KClass<out ExpendablesFilter>, Collection<ExpendablesFilter.Match>> =
    mapOf(
        DefaultCachesPublicFilter::class to listOf(
            ExpendablesFilter.Match.Deletion(
                identifier = DefaultCachesPublicFilter::class,
                lookup = previewLookup(
                    pathSegments = arrayOf("storage", "emulated", "0", "Android", "data", "com.example.app", "cache", "blob.bin"),
                    size = 6L * 1024 * 1024,
                ),
            ),
            ExpendablesFilter.Match.Deletion(
                identifier = DefaultCachesPublicFilter::class,
                lookup = previewLookup(
                    pathSegments = arrayOf("storage", "emulated", "0", "Android", "data", "com.example.app", "cache", "old.tmp"),
                    size = 2L * 1024 * 1024,
                ),
            ),
        ),
    )

internal fun previewInaccessibleCache(
    pkgName: String = "com.example.app",
    itemCount: Int = 12,
    totalSize: Long = 24L * 1024 * 1024,
): InaccessibleCache = InaccessibleCache(
    identifier = mx.loperstack.tokkiphealth.common.pkgs.features.InstallId(
        pkgId = pkgName.toPkgId(),
        userHandle = UserHandle2(handleId = 0),
    ),
    isSystemApp = false,
    itemCount = itemCount,
    totalSize = totalSize,
    publicSize = null,
    theoreticalPaths = emptySet(),
)

internal fun previewAppJunk(
    pkg: Installed = previewInstalled(),
    expendables: Map<kotlin.reflect.KClass<out ExpendablesFilter>, Collection<ExpendablesFilter.Match>>? = previewExpendables(),
    inaccessibleCache: InaccessibleCache? = previewInaccessibleCache(),
    isExclusionLimited: Boolean = false,
): AppJunk = AppJunk(
    pkg = pkg,
    userProfile = null,
    expendables = expendables,
    inaccessibleCache = inaccessibleCache,
    isExclusionLimited = isExclusionLimited,
)

internal fun previewAppCleanerRow(
    junk: AppJunk = previewAppJunk(),
): AppCleanerListViewModel.Row = AppCleanerListViewModel.Row(junk = junk)
