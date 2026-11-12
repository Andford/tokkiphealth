package mx.loperstack.tokkiphealth.common.areas.modules.system

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
import mx.loperstack.tokkiphealth.common.files.canRead
import mx.loperstack.tokkiphealth.common.files.local.LocalGateway
import javax.inject.Inject

@Reusable
class SystemAppModule @Inject constructor(
    private val gatewaySwitch: GatewaySwitch,
) : DataAreaModule {

    override suspend fun secondPass(firstPass: Collection<DataArea>): Collection<DataArea> {
        val gateway = gatewaySwitch.getGateway(APath.PathType.LOCAL) as LocalGateway

        if (!gateway.hasRoot()) {
            log(TAG, INFO) { "LocalGateway has no root, skipping." }
            return emptySet()
        }

        return firstPass
            .filter { it.type == DataArea.Type.SYSTEM }
            .map { systemArea ->
                systemArea to systemArea.path.child("app")
            }
            .filter {
                val canRead = it.second.canRead(gatewaySwitch)
                if (!canRead) log(TAG) { "Can't read ${it.second}" }
                canRead
            }
            .map { (parentArea, path) ->
                DataArea(
                    type = DataArea.Type.SYSTEM_APP,
                    path = path,
                    userHandle = parentArea.userHandle,
                    flags = parentArea.flags,
                )
            }
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: SystemAppModule): DataAreaModule
    }

    companion object {
        val TAG: String = logTag("DataArea", "Module", "System", "App")
    }
}