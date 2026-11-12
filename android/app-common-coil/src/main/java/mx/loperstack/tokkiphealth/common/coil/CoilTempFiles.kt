package mx.loperstack.tokkiphealth.common.coil

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.core.local.listFiles2
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoilTempFiles @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {

    private val basePath by lazy { File(context.cacheDir, "coil") }
    private val legacyPath by lazy { context.cacheDir }

    suspend fun getBaseCachePath(): File {
        val path = basePath.apply {
            if (mkdirs()) log(TAG) { "Cache path was created: $this" }
        }
        return path
    }

    suspend fun cleanUp() = withContext(dispatcherProvider.IO + NonCancellable) {
        try {
            // TODO Remove in a later update
            log(TAG) { "Checking for legacy files in $legacyPath" }
            legacyPath.listFiles2()
                .filter { NAME_REGEX.matches(it.name) }
                .forEach {
                    log(TAG, WARN) { "Deleting legacy tmp file: $it (${it.length()} Byte)" }
                    if (it.delete()) log(TAG, VERBOSE) { "Deleted legacy file: $it" }
                }
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to clean up legacy files $basePath:\n${e.asLog()}" }
        }

        if (!basePath.exists()) return@withContext

        // Theoretically no longer necessary since #1435
        try {
            log(TAG) { "Cleaning up $basePath" }
            basePath.listFiles2().forEach {
                log(TAG, WARN) { "Deleting orphaned tmp file: $it (${it.length()} Byte)" }
                if (it.delete()) log(TAG, VERBOSE) { "Deleted: $it" }
            }
        } catch (e: Exception) {
            log(TAG, WARN) { "Failed to clean up $basePath:\n${e.asLog()}" }
        }
    }

    companion object {
        private val TAG = logTag("Coil", "TempFiles")
        internal val NAME_REGEX = Regex("""tmp\d+\.tmp""")
    }
}