package mx.loperstack.tokkiphealth.appcontrol.core.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.appcontrol.core.AppControlSettings
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppControlSettingsBackupContributor @Inject constructor(
    settings: AppControlSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "appcontrol"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppControlSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: AppControlSettingsBackupContributor): ConfigBackupContributor
}
