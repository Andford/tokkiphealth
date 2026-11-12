package mx.loperstack.tokkiphealth.common.areas.modules.misc

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.modules.DataAreaModule
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.local.LocalGateway
import mx.loperstack.tokkiphealth.common.storage.StorageEnvironment
import mx.loperstack.tokkiphealth.common.user.UserManager2
import javax.inject.Inject


@Reusable
class DownloadCacheModule @Inject constructor(
    private val environment: StorageEnvironment,
    private val userManager2: UserManager2,
    private val gatewaySwitch: GatewaySwitch,
) : DataAreaModule {

    override suspend fun firstPass(): Collection<DataArea> {
        val gateway = gatewaySwitch.getGateway(APath.PathType.LOCAL) as LocalGateway

        if (!gateway.hasRoot()) {
            log(TAG, INFO) { "LocalGateway has no root, skipping." }
            return emptySet()
        }

        return environment.downloadCacheDirs
            .filter { gateway.canRead(it, mode = LocalGateway.Mode.ROOT) }
            .map {
                DataArea(
                    type = DataArea.Type.DOWNLOAD_CACHE,
                    path = it,
                    userHandle = userManager2.systemUser().handle,
                    flags = if (it == environment.downloadCacheDirs.first()) setOf(DataArea.Flag.PRIMARY) else emptySet()
                )
            }
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: DownloadCacheModule): DataAreaModule
    }

    companion object {
        val TAG: String = logTag("DataArea", "Module", "DownloadCache")
    }
}