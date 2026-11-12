package mx.loperstack.tokkiphealth.common.forensics.csi.pub

import android.os.storage.StorageManager
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
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.*
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
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
class PublicObbCSI @Inject constructor(
    private val clutterRepo: ClutterRepo,
    private val pkgRepo: PkgRepo,
    private val areaManager: DataAreaManager,
    private val storageManager: StorageManager,
    private val gatewaySwitch: GatewaySwitch,
) : LocalCSIProcessor {

    override suspend fun hasJurisdiction(type: DataArea.Type): Boolean = type == DataArea.Type.PUBLIC_OBB

    @Suppress("SimplifiableCallChain")
    override suspend fun identifyArea(target: APath): AreaInfo? = areaManager.currentAreas()
        .filter { it.type == DataArea.Type.PUBLIC_OBB }
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
        var hasKnownUnknownOwner = false

        val userHandle = areaInfo.userHandle
        val dirNameAsPkg = areaInfo.prefixFreeSegments.first()

        if (pkgRepo.isInstalled(dirNameAsPkg.toPkgId(), userHandle)) {
            owners.add(Owner(dirNameAsPkg.toPkgId(), userHandle))
        } else {
            clutterRepo.match(areaInfo.type, listOf(dirNameAsPkg))
                .map { it.toOwners(areaInfo) }
                .flatten()
                .run { owners.addAll(this) }
        }

        if (owners.isEmpty()) {
            try {
                val content = areaInfo.file.listFiles(gatewaySwitch)
                hasKnownUnknownOwner = content
                    .filterIsInstance<LocalPath>()
                    .any { storageManager.isObbMounted(it.path) }
            } catch (e: Exception) {
                log(TAG, ERROR) { "Path listing failed: ${e.asLog()}" }
            }
        }

        return CSIProcessor.Result(
            owners = owners,
            hasKnownUnknownOwner = hasKnownUnknownOwner
        )
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: PublicObbCSI): CSIProcessor
    }

    companion object {
        val TAG: String = logTag("CSI", "Public", "Obb")
        val BASE_SEGMENTS = arrayOf("Android", "obb")
    }
}