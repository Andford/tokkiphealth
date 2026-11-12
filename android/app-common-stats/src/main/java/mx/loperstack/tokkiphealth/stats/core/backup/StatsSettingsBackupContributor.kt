package mx.loperstack.tokkiphealth.stats.core.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import mx.loperstack.tokkiphealth.stats.core.StatsSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsSettingsBackupContributor @Inject constructor(
    settings: StatsSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "stats"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class StatsSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: StatsSettingsBackupContributor): ConfigBackupContributor
}
