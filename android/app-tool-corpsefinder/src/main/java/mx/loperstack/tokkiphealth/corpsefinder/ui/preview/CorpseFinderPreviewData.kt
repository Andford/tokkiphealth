package mx.loperstack.tokkiphealth.corpsefinder.ui.preview

import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.FileType
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.files.local.LocalPathLookup
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.Owner
import mx.loperstack.tokkiphealth.common.forensics.OwnerInfo
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.user.UserHandle2
import mx.loperstack.tokkiphealth.corpsefinder.core.Corpse
import mx.loperstack.tokkiphealth.corpsefinder.core.RiskLevel
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.PrivateDataCorpseFilter
import mx.loperstack.tokkiphealth.corpsefinder.ui.list.CorpseFinderListViewModel
import java.time.Instant
import kotlin.reflect.KClass

internal fun previewLocalPathLookup(
    pathSegments: Array<String> = arrayOf("storage", "emulated", "0", "Android", "data", "com.example.app"),
    fileType: FileType = FileType.DIRECTORY,
    size: Long = 12L * 1024 * 1024,
): LocalPathLookup = LocalPathLookup(
    lookedUp = LocalPath.build(*pathSegments),
    fileType = fileType,
    size = size,
    modifiedAt = Instant.parse("2026-04-01T12:00:00Z"),
    target = null,
)

private fun previewAreaInfo(): AreaInfo {
    val root = LocalPath.build("storage", "emulated", "0", "Android", "data")
    return AreaInfo(
        file = root,
        prefix = root,
        dataArea = DataArea(
            path = root,
            type = DataArea.Type.PRIVATE_DATA,
            label = "Private data".toCaString(),
            userHandle = UserHandle2(handleId = 0),
        ),
        isBlackListLocation = false,
    )
}

private fun previewOwnerInfo(
    owners: Set<Owner> = setOf(
        Owner(pkgId = Pkg.Id(name = "com.example.app"), userHandle = UserHandle2(handleId = 0)),
    ),
): OwnerInfo = OwnerInfo(
    areaInfo = previewAreaInfo(),
    owners = owners,
    installedOwners = emptySet(),
    hasUnknownOwner = false,
)

internal fun previewCorpse(
    filterType: KClass<out mx.loperstack.tokkiphealth.corpsefinder.core.filter.CorpseFilter> = PrivateDataCorpseFilter::class,
    lookup: LocalPathLookup = previewLocalPathLookup(),
    content: Collection<APathLookup<*>> = listOf(
        previewLocalPathLookup(
            pathSegments = arrayOf("storage", "emulated", "0", "Android", "data", "com.example.app", "cache.bin"),
            fileType = FileType.FILE,
            size = 8L * 1024 * 1024,
        ),
        previewLocalPathLookup(
            pathSegments = arrayOf("storage", "emulated", "0", "Android", "data", "com.example.app", "old.log"),
            fileType = FileType.FILE,
            size = 4L * 1024 * 1024,
        ),
    ),
    riskLevel: RiskLevel = RiskLevel.NORMAL,
): Corpse = Corpse(
    filterType = filterType,
    ownerInfo = previewOwnerInfo(),
    lookup = lookup,
    content = content,
    riskLevel = riskLevel,
)

internal fun previewCorpseRow(
    corpse: Corpse = previewCorpse(),
): CorpseFinderListViewModel.Row = CorpseFinderListViewModel.Row(corpse = corpse)
