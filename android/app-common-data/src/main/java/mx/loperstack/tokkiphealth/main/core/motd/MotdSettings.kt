package mx.loperstack.tokkiphealth.main.core.motd

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.datastore.createValue
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MotdSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    json: Json,
) {

    private val Context.dataStore by preferencesDataStore(name = "settings_motd")

    val dataStore: DataStore<Preferences>
        get() = context.dataStore

    val lastMotd = dataStore.createValue<MotdState?>("motd.state.cache", null, json)
    val lastDismissedMotd = dataStore.createValue<UUID?>("motd.last.dismissed", null, json)
    val isMotdEnabled = dataStore.createValue("motd.enabled", true)


    companion object {
        internal val TAG = logTag("Motd", "Settings")
    }
}