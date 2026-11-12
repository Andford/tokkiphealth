package mx.loperstack.tokkiphealth.appcleaner.core.scanner

import dagger.Reusable
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.core.local.File
import mx.loperstack.tokkiphealth.common.files.local.toLocalPath
import mx.loperstack.tokkiphealth.common.hasApiLevel
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.isSystemApp
import mx.loperstack.tokkiphealth.common.storage.StorageId
import mx.loperstack.tokkiphealth.common.storage.StorageStatsManager2
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject


@Reusable
class InaccessibleCacheProvider @Inject constructor(
    private val storageStatsManager: StorageStatsManager2,
) {

    suspend fun determineCache(pkg: Installed): InaccessibleCache? {
        val applicationInfo = pkg.applicationInfo

        if (applicationInfo == null) {
            log(TAG, WARN) { "Application info was NULL for ${pkg.id}" }
            return null
        }

        val storageStats = try {
            storageStatsManager.queryStatsForPkg(
                StorageId(internalId = null, externalId = applicationInfo.storageUuid),
                pkg,
            )
        } catch (e: SecurityException) {
            log(TAG, WARN) { "Don't have permission to query app size for ${pkg.id}: $e" }
            return null
        } catch (e: Exception) {
            // Don't swallow cancellation of our own scope: callers poll this in a loop under a
            // deadline, and a null here means "query failed", not "we gave up waiting".
            currentCoroutineContext().ensureActive()
            log(TAG, ERROR) { "Unexpected error when querying app size for ${pkg.id}: ${e.asLog()}" }
            return null
        }

        return InaccessibleCache(
            pkg.installId,
            isSystemApp = pkg.isSystemApp,
            itemCount = 2,
            totalSize = storageStats.cacheBytes,
            publicSize = if (hasApiLevel(31)) {
                @Suppress("NewApi")
                storageStats.externalCacheBytes
            } else null,
            theoreticalPaths = pkg.genTheoreticalPaths(),
        )
    }

    private fun Installed.genTheoreticalPaths() = mutableSetOf<APath>().apply {
        add(File("/storage/emulated/${userHandle.handleId}/Android/data/${id.name}/cache").toLocalPath())
        add(File("/data/user/${userHandle.handleId}/${id.name}/cache").toLocalPath())
    }

    companion object {
        val TAG = logTag("AppCleaner", "Scanner", "Inaccessible")
    }
}