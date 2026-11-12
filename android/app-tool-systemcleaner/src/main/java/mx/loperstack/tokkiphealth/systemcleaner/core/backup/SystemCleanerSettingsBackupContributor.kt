package mx.loperstack.tokkiphealth.systemcleaner.core.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleanerSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SystemCleaner *settings*, including the `enabledCustomFilter` id set. Runs at the default
 * (settings) restore order so it lands after the custom-filter files content contributor — otherwise
 * enabled ids would reference filters that don't exist yet and get pruned.
 */
@Singleton
class SystemCleanerSettingsBackupContributor @Inject constructor(
    settings: SystemCleanerSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "systemcleaner.settings"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SystemCleanerSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: SystemCleanerSettingsBackupContributor): ConfigBackupContributor
}
