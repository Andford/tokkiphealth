package mx.loperstack.tokkiphealth.common.areas.modules.priv

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.modules.DataAreaModule
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.canRead
import mx.loperstack.tokkiphealth.common.files.exists
import mx.loperstack.tokkiphealth.common.files.local.LocalGateway
import mx.loperstack.tokkiphealth.common.files.local.toLocalPath
import mx.loperstack.tokkiphealth.common.storage.StorageEnvironment
import mx.loperstack.tokkiphealth.common.storage.StorageManager2
import mx.loperstack.tokkiphealth.common.user.UserManager2
import javax.inject.Inject

@Reusable
class DataModule @Inject constructor(
    private val storageEnvironment: StorageEnvironment,
    private val userManager2: UserManager2,
    private val storageManager2: StorageManager2,
    private val gatewaySwitch: GatewaySwitch,
) : DataAreaModule {

    override suspend fun firstPass(): Collection<DataArea> {
        val areas = mutableSetOf<DataArea>()

        val gateway = gatewaySwitch.getGateway(APath.PathType.LOCAL) as LocalGateway
        if (!gateway.hasRoot()) {
            log(TAG, INFO) { "LocalGateway has no root, skipping." }
            return emptySet()
        }

        storageEnvironment.dataDir
            .takeIf { it.exists(gatewaySwitch) }
            ?.let {
                DataArea(
                    type = DataArea.Type.DATA,
                    path = it,
                    userHandle = userManager2.currentUser().handle,
                    flags = setOf(DataArea.Flag.PRIMARY)
                )
            }
            ?.run { areas.add(this) }

        try {
            storageManager2.volumes
                ?.also { log(TAG, VERBOSE) { "Checking $it" } }
                ?.mapNotNull { volume ->
                    if (!volume.isPrivate || volume.id?.startsWith("private:") != true || !volume.isMounted) {
                        return@mapNotNull null
                    }

                    volume.path?.toLocalPath() ?: return@mapNotNull null
                }
                ?.filter { it.canRead(gatewaySwitch) }
                ?.map { path ->
                    DataArea(
                        type = DataArea.Type.DATA,
                        path = path,
                        userHandle = userManager2.currentUser().handle,
                        flags = emptySet(),
                    )
                }
                ?.run { areas.addAll(this) }
        } catch (e: Exception) {
            log(TAG, ERROR) { "Error accessing volumes: ${e.asLog()}" }
        }

        log(TAG, VERBOSE) { "firstPass(): $areas" }

        return areas
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: DataModule): DataAreaModule
    }

    companion object {
        val TAG: String = logTag("DataArea", "Module", "Data")
    }
}