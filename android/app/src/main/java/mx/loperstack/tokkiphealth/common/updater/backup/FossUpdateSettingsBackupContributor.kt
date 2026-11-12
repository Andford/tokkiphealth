package mx.loperstack.tokkiphealth.common.updater.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import mx.loperstack.tokkiphealth.common.updater.FossUpdateSettings
import javax.inject.Inject
import javax.inject.Singleton

/** FOSS-only. On a GPLAY restore this section has no matching contributor and is skipped. */
@Singleton
class FossUpdateSettingsBackupContributor @Inject constructor(
    settings: FossUpdateSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "updater.foss"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FossUpdateSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: FossUpdateSettingsBackupContributor): ConfigBackupContributor
}
