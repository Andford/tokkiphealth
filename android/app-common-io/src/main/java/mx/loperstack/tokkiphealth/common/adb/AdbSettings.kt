package mx.loperstack.tokkiphealth.common.adb

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
class AdbSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val Context.dataStore by preferencesDataStore(name = "settings_shizuku")

    val dataStore: DataStore<Preferences>
        get() = context.dataStore

    val useShizuku = dataStore.createValue("core.shizuku.enabled", null as Boolean?)

    val useAdb = useShizuku

    companion object {
        internal val TAG = logTag("ADB", "Settings")
    }
}