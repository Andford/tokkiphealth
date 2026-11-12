package mx.loperstack.tokkiphealth.setup

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mx.loperstack.tokkiphealth.setup.inventory.InventorySetupModule
import mx.loperstack.tokkiphealth.setup.SetupBinding

@Module
@InstallIn(SingletonComponent::class)
object SetupHeartbeatModule {

    @Provides
    @SetupBinding(SetupModule.Type.INVENTORY)
    fun inventoryHeartbeat(module: InventorySetupModule): SetupHeartbeat = SetupHeartbeat {
        if (!module.isComplete()) {
            throw IncompleteSetupException(SetupModule.Type.INVENTORY)
        }
    }
}
