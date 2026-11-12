package mx.loperstack.tokkiphealth.exclusion.core

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.serialization.SerializedStorage
import mx.loperstack.tokkiphealth.exclusion.core.types.Exclusion
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExclusionStorage @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    json: Json,
) : SerializedStorage<Set<Exclusion>>(dispatcherProvider, json, TAG) {

    override val provideBackupPath: () -> File = { File(context.filesDir, "exclusions") }

    override val provideBackupFileName: () -> String = { "exclusions-v1" }

    override val provideSerializer: () -> KSerializer<Set<Exclusion>> = { SetSerializer(Exclusion.serializer()) }

    companion object {
        private val TAG = logTag("Exclusion", "Storage")
    }
}
