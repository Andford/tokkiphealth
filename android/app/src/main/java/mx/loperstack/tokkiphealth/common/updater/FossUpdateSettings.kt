package mx.loperstack.tokkiphealth.common.updater

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.datastore.DataStoreValue
import mx.loperstack.tokkiphealth.common.datastore.createValue
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FossUpdateSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    json: Json,
) {

    private val Context.dataStore by preferencesDataStore(name = "settings_updater_foss")

    val dataStore: DataStore<Preferences>
        get() = context.dataStore

    private fun FossUpdateChecker.Update.getSetting(): DataStoreValue<Boolean> {
        return dataStore.createValue("update.${this.versionName}.dismissed", false)
    }

    suspend fun dismiss(update: FossUpdateChecker.Update) {
        update.getSetting().value(true)
    }

    suspend fun isDismissed(update: FossUpdateChecker.Update): Boolean {
        return update.getSetting().value()
    }

    val lastReleaseCheck = dataStore.createValue("check.last", Instant.EPOCH, json)
    val lastReleaseProd = dataStore.createValue<GithubApi.ReleaseInfo?>("check.last.prod", null, json)
    val lastReleaseBeta = dataStore.createValue<GithubApi.ReleaseInfo?>("check.last.beta", null, json)

    companion object {
        private val TAG = logTag("Updater", "Checker", "FOSS", "Settings")
    }
}