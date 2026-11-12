package mx.loperstack.tokkiphealth.common.forensics.csi.pub

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.DataAreaManager
import mx.loperstack.tokkiphealth.common.areas.currentAreas
import mx.loperstack.tokkiphealth.common.clutter.ClutterRepo
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.isAncestorOf
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.CSIProcessor
import mx.loperstack.tokkiphealth.common.forensics.Owner
import mx.loperstack.tokkiphealth.common.forensics.csi.LocalCSIProcessor
import mx.loperstack.tokkiphealth.common.forensics.csi.toOwners
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.isInstalled
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import javax.inject.Inject

@Reusable
class PublicMediaCSI @Inject constructor(
    private val clutterRepo: ClutterRepo,
    private val pkgRepo: PkgRepo,
    private val areaManager: DataAreaManager,
) : LocalCSIProcessor {

    override suspend fun hasJurisdiction(type: DataArea.Type): Boolean = type == DataArea.Type.PUBLIC_MEDIA

    @Suppress("SimplifiableCallChain")
    override suspend fun identifyArea(target: APath): AreaInfo? = areaManager.currentAreas()
        .filter { it.type == DataArea.Type.PUBLIC_MEDIA }
        .filter { it.path.isAncestorOf(target) }
        .singleOrNull()
        ?.let {
            AreaInfo(
                dataArea = it,
                file = target,
                prefix = it.path,
                isBlackListLocation = true
            )
        }

    override suspend fun findOwners(areaInfo: AreaInfo): CSIProcessor.Result {
        require(hasJurisdiction(areaInfo.type)) { "Wrong jurisdiction: ${areaInfo.type}" }

        val owners = mutableSetOf<Owner>()

        val userHandle = areaInfo.userHandle
        val dirNameAsPkg = areaInfo.prefixFreeSegments.first()
        var hiddenDirAsPkg: String? = null

        if (pkgRepo.isInstalled(dirNameAsPkg.toPkgId(), userHandle)) {
            owners.add(Owner(dirNameAsPkg.toPkgId(), userHandle))
        } else {
            hiddenDirAsPkg = cleanDirName(dirNameAsPkg)
            if (hiddenDirAsPkg != null && pkgRepo.isInstalled(hiddenDirAsPkg.toPkgId(), userHandle)) {
                owners.add(Owner(hiddenDirAsPkg.toPkgId(), userHandle))
            }
        }

        if (owners.isEmpty()) {
            clutterRepo.match(areaInfo.type, listOf(dirNameAsPkg))
                .map { it.toOwners(areaInfo) }
                .flatten()
                .run { owners.addAll(this) }
        }

        // Fallback, no downside to assuming that dirname=pkgname for PUBLIC_MEDIA if there are no other owners
        if (owners.isEmpty()) {
            owners.add(Owner((hiddenDirAsPkg ?: dirNameAsPkg).toPkgId(), userHandle))
        }

        return CSIProcessor.Result(
            owners = owners,
            hasKnownUnknownOwner = false
        )
    }

    private fun cleanDirName(name: String): String? = when {
        name.startsWith(".external.") -> {
            // rare modifier, seen it on my N5 with the .external.com.plexapp.android
            name.substring(10)
        }
        name.startsWith("_") || name.startsWith(".") -> {
            // usually used in public/Android/data to hide stuff from uninstall
            // some devs just don't know better
            name.substring(1)
        }
        else -> null
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: PublicMediaCSI): CSIProcessor
    }

    companion object {
        val TAG: String = logTag("CSI", "Public", "Media")
        val BASE_SEGMENTS = arrayOf("Android", "media")
    }
}