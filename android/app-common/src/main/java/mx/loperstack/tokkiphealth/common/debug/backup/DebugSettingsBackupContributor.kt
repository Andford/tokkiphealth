package mx.loperstack.tokkiphealth.common.debug.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import mx.loperstack.tokkiphealth.common.debug.DebugSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugSettingsBackupContributor @Inject constructor(
    settings: DebugSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "debug"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DebugSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: DebugSettingsBackupContributor): ConfigBackupContributor
}
