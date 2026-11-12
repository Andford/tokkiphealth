package mx.loperstack.tokkiphealth.common.forensics.csi.source

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
import mx.loperstack.tokkiphealth.common.files.isDescendantOf
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.CSIProcessor
import mx.loperstack.tokkiphealth.common.forensics.Owner
import mx.loperstack.tokkiphealth.common.forensics.csi.LocalCSIProcessor
import mx.loperstack.tokkiphealth.common.forensics.csi.toOwners
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.current
import mx.loperstack.tokkiphealth.common.pkgs.isInstalled
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import javax.inject.Inject

@Reusable
class AppSourceLibCSI @Inject constructor(
    private val areaManager: DataAreaManager,
    private val pkgRepo: PkgRepo,
    private val clutterRepo: ClutterRepo,
) : LocalCSIProcessor {
    override suspend fun hasJurisdiction(type: DataArea.Type): Boolean = type == DataArea.Type.APP_LIB

    override suspend fun identifyArea(target: APath): AreaInfo? = areaManager.currentAreas()
        .filter { it.type == DataArea.Type.APP_LIB }
        .mapNotNull { area ->
            if (!area.path.isAncestorOf(target)) return@mapNotNull null

            AreaInfo(
                dataArea = area,
                file = target,
                prefix = area.path,
                isBlackListLocation = true
            )
        }
        .singleOrNull()

    override suspend fun findOwners(areaInfo: AreaInfo): CSIProcessor.Result {
        require(hasJurisdiction(areaInfo.type)) { "Wrong jurisdiction: ${areaInfo.type}" }

        val owners = mutableSetOf<Owner>()
        val userHandle = areaInfo.userHandle
        val dirName = areaInfo.prefixFreeSegments.first()
        dirName
            .let { APPLIB_DIR.matchEntire(it) }
            ?.let { it.groupValues[1].toPkgId() }
            ?.takeIf { pkgRepo.isInstalled(it, userHandle) }
            ?.let { owners.add(Owner(it, userHandle)) }

        if (owners.isEmpty()) {
            pkgRepo.current()
                .filter { it.applicationInfo != null }
                .filter { pkg ->
                    val nativLibDir = pkg.applicationInfo?.nativeLibraryDir?.let {
                        LocalPath.build(it)
                    } ?: return@filter false
                    areaInfo.file == nativLibDir || areaInfo.file.isDescendantOf(nativLibDir)
                }
                .map { Owner(it.id, userHandle) }
                .run { owners.addAll(this) }
        }

        if (owners.isEmpty()) {
            val matches = clutterRepo.match(areaInfo.type, listOf(dirName))
            owners.addAll(matches.map { it.toOwners(areaInfo) }.flatten())
        }


        return CSIProcessor.Result(owners)
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: AppSourceLibCSI): CSIProcessor
    }

    companion object {
        val TAG: String = logTag("CSI", "AppSource", "Lib")
        const val DIRNAME = "app-lib"
        private val APPLIB_DIR by lazy { Regex("^([\\w.\\-]+)-[0-9]{1,4}$") }
    }
}