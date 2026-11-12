package mx.loperstack.tokkiphealth.scheduler.core.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import mx.loperstack.tokkiphealth.scheduler.core.SchedulerSettings
import javax.inject.Inject
import javax.inject.Singleton

/** Scheduler *settings*; the schedule entries themselves are a separate content contributor. */
@Singleton
class SchedulerSettingsBackupContributor @Inject constructor(
    settings: SchedulerSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "scheduler.settings"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: SchedulerSettingsBackupContributor): ConfigBackupContributor
}
