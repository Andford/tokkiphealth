package mx.loperstack.tokkiphealth.deduplicator.core.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import mx.loperstack.tokkiphealth.deduplicator.core.DeduplicatorSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeduplicatorSettingsBackupContributor @Inject constructor(
    settings: DeduplicatorSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "deduplicator"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DeduplicatorSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: DeduplicatorSettingsBackupContributor): ConfigBackupContributor
}
