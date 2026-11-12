package mx.loperstack.tokkiphealth.common.adb.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.adb.AdbSettings
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbSettingsBackupContributor @Inject constructor(
    settings: AdbSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "adb"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AdbSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: AdbSettingsBackupContributor): ConfigBackupContributor
}
