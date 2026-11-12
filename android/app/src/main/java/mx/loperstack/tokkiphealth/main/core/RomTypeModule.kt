package mx.loperstack.tokkiphealth.main.core

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.device.RomTypeProvider
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RomTypeModule {

    @Provides
    @Singleton
    fun provideRomTypeProvider(generalSettings: GeneralSettings): RomTypeProvider = RomTypeProvider {
        generalSettings.romTypeDetection.value()
    }
}
