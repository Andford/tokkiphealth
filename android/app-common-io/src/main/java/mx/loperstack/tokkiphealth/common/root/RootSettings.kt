package mx.loperstack.tokkiphealth.common.root

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.datastore.createValue
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val Context.dataStore by preferencesDataStore(name = "settings_root")

    val dataStore: DataStore<Preferences>
        get() = context.dataStore

    val useRoot = dataStore.createValue("core.root.enabled", null as Boolean?)

    companion object {
        internal val TAG = logTag("Root", "Settings")
    }
}