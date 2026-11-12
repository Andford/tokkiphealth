package mx.loperstack.tokkiphealth.common.forensics.csi.sys

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.DataAreaManager
import mx.loperstack.tokkiphealth.common.areas.currentAreas
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.isAncestorOf
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.CSIProcessor
import mx.loperstack.tokkiphealth.common.forensics.csi.LocalCSIProcessor
import javax.inject.Inject

@Reusable
class DownloadCacheCSI @Inject constructor(
    private val areaManager: DataAreaManager,
) : LocalCSIProcessor {

    override suspend fun hasJurisdiction(type: DataArea.Type): Boolean = type == DataArea.Type.DOWNLOAD_CACHE

    override suspend fun identifyArea(target: APath): AreaInfo? =
        areaManager.currentAreas()
            .filter { it.type == DataArea.Type.DOWNLOAD_CACHE }
            .mapNotNull { area ->
                if (!area.path.isAncestorOf(target)) return@mapNotNull null

                AreaInfo(
                    dataArea = area,
                    file = target,
                    prefix = area.path,
                    isBlackListLocation = false
                )
            }
            .singleOrNull()

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: DownloadCacheCSI): CSIProcessor
    }

    companion object {
        val TAG: String = logTag("CSI", "System", "DownloadCache")
    }
}