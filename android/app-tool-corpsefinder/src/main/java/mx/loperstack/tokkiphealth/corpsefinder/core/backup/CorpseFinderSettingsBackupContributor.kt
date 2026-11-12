package mx.loperstack.tokkiphealth.corpsefinder.core.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import mx.loperstack.tokkiphealth.corpsefinder.core.CorpseFinderSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CorpseFinderSettingsBackupContributor @Inject constructor(
    settings: CorpseFinderSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "corpsefinder"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CorpseFinderSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: CorpseFinderSettingsBackupContributor): ConfigBackupContributor
}
