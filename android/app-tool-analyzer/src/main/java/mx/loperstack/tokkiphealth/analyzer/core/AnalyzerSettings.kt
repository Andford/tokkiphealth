package mx.loperstack.tokkiphealth.analyzer.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.datastore.createValue
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.ui.LayoutMode
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AnalyzerSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    json: Json,
) {

    private val Context.dataStore by preferencesDataStore(name = "settings_analyzer")

    val dataStore: DataStore<Preferences>
        get() = context.dataStore

    val contentLayoutMode = dataStore.createValue("ui.content.layoutmode", LayoutMode.LINEAR, json)

    /**
     * When free space drops to or below this, storage counts as running out.
     * `null` means automatic, see [mx.loperstack.tokkiphealth.stats.core.LowStorage.resolveThreshold].
     */
    val lowStorageThresholdBytes = dataStore.createValue<Long?>("storage.low.threshold.bytes", null)

    /** Pro-gated: warn before the device runs out of space. */
    val lowSpaceNotificationEnabled = dataStore.createValue("storage.low.notification.enabled", false)

    /**
     * Transition latch: `true` means "a crossing into the warning band may notify".
     *
     * Runtime state, not a preference, and therefore excluded from backup
     * (see [mx.loperstack.tokkiphealth.analyzer.core.backup.AnalyzerSettingsBackupContributor]).
     */
    val lowSpaceNotificationArmed = dataStore.createValue("storage.low.notification.armed", true)

    val hintLowSpaceDismissed = dataStore.createValue("hint.lowspace.dismissed", false)

    companion object {
        internal val TAG = logTag("Analyzer", "Settings")

        const val LOW_STORAGE_THRESHOLD_MIN = 100L * 1024 * 1024
        const val LOW_STORAGE_THRESHOLD_MAX = 32L * 1024 * 1024 * 1024
    }
}
