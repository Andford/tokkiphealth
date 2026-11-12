package mx.loperstack.tokkiphealth.common.root.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import mx.loperstack.tokkiphealth.common.root.RootSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootSettingsBackupContributor @Inject constructor(
    settings: RootSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "root"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RootSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: RootSettingsBackupContributor): ConfigBackupContributor
}
