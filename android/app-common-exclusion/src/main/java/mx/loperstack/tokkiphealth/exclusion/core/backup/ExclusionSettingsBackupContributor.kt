package mx.loperstack.tokkiphealth.exclusion.core.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import mx.loperstack.tokkiphealth.exclusion.core.ExclusionSettings
import javax.inject.Inject
import javax.inject.Singleton

/** Backs up exclusion *settings* (e.g. which built-in defaults were removed). The user's own
 *  exclusions are handled separately by the exclusions content contributor. */
@Singleton
class ExclusionSettingsBackupContributor @Inject constructor(
    settings: ExclusionSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "exclusion.settings"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ExclusionSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: ExclusionSettingsBackupContributor): ConfigBackupContributor
}
