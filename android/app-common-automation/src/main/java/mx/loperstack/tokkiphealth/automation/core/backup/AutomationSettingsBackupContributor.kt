package mx.loperstack.tokkiphealth.automation.core.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.automation.core.AutomationSettings
import mx.loperstack.tokkiphealth.common.backup.ConfigBackupContributor
import mx.loperstack.tokkiphealth.common.backup.DataStoreSettingsBackupContributor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationSettingsBackupContributor @Inject constructor(
    settings: AutomationSettings,
) : DataStoreSettingsBackupContributor(settings.dataStore) {
    override val key = "automation"

    // Transient mid-operation state + device-specific probe result, not user config.
    override val excludedKeys = setOf(
        "animation.pending.restore.state",
        "acs.directwrite.unreliable.fingerprint",
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AutomationSettingsBackupModule {
    @Binds
    @IntoSet
    abstract fun bind(c: AutomationSettingsBackupContributor): ConfigBackupContributor
}
