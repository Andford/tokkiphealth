package mx.loperstack.tokkiphealth.common.areas.modules.priv

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.hasFlags
import mx.loperstack.tokkiphealth.common.areas.modules.DataAreaModule
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.canRead
import mx.loperstack.tokkiphealth.common.files.local.LocalGateway
import mx.loperstack.tokkiphealth.common.user.UserManager2
import javax.inject.Inject


@Reusable
class DataSystemDEModule @Inject constructor(
    private val userManager2: UserManager2,
    private val gatewaySwitch: GatewaySwitch,
) : DataAreaModule {

    override suspend fun secondPass(firstPass: Collection<DataArea>): Collection<DataArea> {
        val gateway = gatewaySwitch.getGateway(APath.PathType.LOCAL) as LocalGateway

        if (!gateway.hasRoot()) {
            log(TAG, INFO) { "LocalGateway has no root, skipping." }
            return emptySet()
        }

        return firstPass
            .filter { it.type == DataArea.Type.DATA && it.hasFlags(DataArea.Flag.PRIMARY) }
            .map { parentArea ->
                userManager2.allUsers().map { profile ->
                    DataArea(
                        type = DataArea.Type.DATA_SYSTEM_DE,
                        path = parentArea.path.child("system_de", profile.handle.handleId.toString()),
                        userHandle = profile.handle,
                        flags = parentArea.flags,
                    )
                }
            }
            .flatten()
            .filter {
                val canRead = it.path.canRead(gatewaySwitch)
                if (!canRead) log(TAG) { "Can't read $it" }
                canRead
            }
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: DataSystemDEModule): DataAreaModule
    }

    companion object {
        val TAG: String = logTag("DataArea", "Module", "DataSystemDE")
    }
}