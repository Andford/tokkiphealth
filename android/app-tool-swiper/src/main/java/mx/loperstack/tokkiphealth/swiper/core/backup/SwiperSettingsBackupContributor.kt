package mx.loperstack.tokkiphealth.swiper.core.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import mx.loperstack.tokkiphealth.swiper.core.SwiperSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwiperSettingsBackupContributor @Inject constructor(
    settings: SwiperSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "swiper"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SwiperSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: SwiperSettingsBackupContributor): ConfigBackupContributor
}
