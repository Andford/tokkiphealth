package mx.loperstack.tokkiphealth.swiper.core.deleter

import android.content.Context
import android.media.MediaScannerConnection
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.*
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.delete
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.files.saf.SAFPath
import mx.loperstack.tokkiphealth.common.storage.PathMapper
import mx.loperstack.tokkiphealth.common.flow.throttleLatest
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.progress.updateProgressCount
import mx.loperstack.tokkiphealth.common.progress.updateProgressPrimary
import mx.loperstack.tokkiphealth.common.progress.updateProgressSecondary
import mx.loperstack.tokkiphealth.swiper.core.SwipeDecision
import mx.loperstack.tokkiphealth.swiper.core.SwipeItem
import mx.loperstack.tokkiphealth.swiper.core.db.SwipeItemDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class SwiperDeleter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gatewaySwitch: GatewaySwitch,
    private val pathMapper: PathMapper,
) : Progress.Host, Progress.Client {

    private val progressPub = MutableStateFlow<Progress.Data?>(Progress.Data())
    override val progress: Flow<Progress.Data?> = progressPub.throttleLatest(250)

    override fun updateProgress(update: (Progress.Data?) -> Progress.Data?) {
        progressPub.value = update(progressPub.value)
    }

    data class Result(
        val deletedPaths: Set<APath>,
        val deletedSize: Long,
        val failedPaths: Set<APath>,
    )

    suspend fun delete(items: List<SwipeItem>, itemDao: SwipeItemDao): Result {
        log(TAG) { "delete(items=${items.size})" }

        updateProgressPrimary(mx.loperstack.tokkiphealth.common.R.string.general_progress_deleting)

        val deletedPaths = mutableSetOf<APath>()
        val failedPaths = mutableSetOf<APath>()
        var deletedSize = 0L

        items.forEachIndexed { index, item ->
            updateProgressSecondary(item.lookup.userReadablePath)
            updateProgressCount(Progress.Count.Percent(index, items.size))

            try {
                item.lookup.lookedUp.delete(gatewaySwitch)
                deletedPaths.add(item.lookup.lookedUp)
                deletedSize += item.lookup.size
                log(TAG, VERBOSE) { "Deleted: ${item.lookup.lookedUp}" }

                // Mark item as DELETED
                itemDao.updateDecision(item.id, SwipeDecision.DELETED)

                // Notify MediaScanner about deleted file
                notifyMediaScanner(item.lookup.lookedUp)
            } catch (e: Exception) {
                log(TAG, WARN) { "Failed to delete ${item.lookup.lookedUp}: ${e.message}" }
                failedPaths.add(item.lookup.lookedUp)

                // Mark item as DELETE_FAILED
                itemDao.updateDecision(item.id, SwipeDecision.DELETE_FAILED)
            }
        }

        log(TAG) { "Deletion complete: deleted=${deletedPaths.size}, failed=${failedPaths.size}, size=$deletedSize" }

        return Result(
            deletedPaths = deletedPaths,
            deletedSize = deletedSize,
            failedPaths = failedPaths,
        )
    }

    private suspend fun notifyMediaScanner(path: APath) {
        when (path.pathType) {
            APath.PathType.LOCAL -> scanFile(path as LocalPath)
            APath.PathType.SAF -> {
                // SAF documents live on real storage; map the tree URI back to its mount point so
                // gallery apps drop the deleted entry instead of showing it until their own rescan.
                val localPath = pathMapper.toLocalPath(path as SAFPath)
                if (localPath != null) {
                    scanFile(localPath)
                } else {
                    log(TAG, WARN) { "No filesystem mapping for $path, media scanner not notified" }
                }
            }
            APath.PathType.RAW -> throw IllegalStateException("How did we get $path here")
        }
    }

    private fun scanFile(localPath: LocalPath) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(localPath.file.absolutePath),
                null,
                null,
            )
        } catch (e: Exception) {
            log(TAG, WARN) { "MediaScanner notification failed for $localPath: ${e.message}" }
        }
    }

    companion object {
        private val TAG = logTag("Swiper", "Deleter")
    }
}
