package mx.loperstack.tokkiphealth.deduplicator.core.scanner.checksum

import mx.loperstack.tokkiphealth.deduplicator.R
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.flow.throttleLatest
import mx.loperstack.tokkiphealth.common.hashing.Hasher
import mx.loperstack.tokkiphealth.common.hashing.hash
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.progress.increaseProgress
import mx.loperstack.tokkiphealth.common.progress.updateProgressCount
import mx.loperstack.tokkiphealth.common.progress.updateProgressPrimary
import mx.loperstack.tokkiphealth.common.progress.updateProgressSecondary
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import mx.loperstack.tokkiphealth.deduplicator.core.scanner.Sleuth
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import javax.inject.Inject

class ChecksumSleuth @Inject constructor(
    private val gatewaySwitch: GatewaySwitch,
) : Sleuth {

    private val progressPub = MutableStateFlow<Progress.Data?>(Progress.Data())
    override val progress: Flow<Progress.Data?> = progressPub.throttleLatest(250)

    override fun updateProgress(update: (Progress.Data?) -> Progress.Data?) {
        progressPub.value = update(progressPub.value)
    }

    override suspend fun investigate(searchFlow: Flow<APathLookup<*>>): Set<ChecksumDuplicate.Group> {
        log(TAG) { "investigate($searchFlow):..." }
        updateProgressPrimary(R.string.deduplicator_detection_method_checksum_title.toCaString())
        updateProgressSecondary(mx.loperstack.tokkiphealth.common.R.string.general_progress_searching)

        val suspects = searchFlow.toSet()

        val sizeBuckets: Map<Long, List<APathLookup<*>>> = suspects
            .groupBy { it.size }
            .filterValues { it.size >= 2 }
        log(TAG) { "${sizeBuckets.size} size buckets of 2 or more items" }

        updateProgressSecondary(R.string.deduplicator_progress_comparing_files)
        updateProgressCount(Progress.Count.Percent(sizeBuckets.values.sumOf { it.size }))

        val hashStart = System.currentTimeMillis()
        val hashBuckets = sizeBuckets.values
            .asFlow()
            .flatMapMerge { items ->
                flow {
                    val checksummedGroup = items.map { item ->
                        val start = System.currentTimeMillis()

                        val hash = try {
                            gatewaySwitch.file(item.lookedUp, readWrite = false).source().hash(Hasher.Type.SHA256)
                        } catch (e: Exception) {
                            log(TAG, ERROR) { "Failed to read $item: ${e.asLog()}" }
                            return@flow
                        }
                        val hexHash = hash.format()

                        val stop = System.currentTimeMillis()
                        log(TAG, VERBOSE) {
                            "Checksum: $hexHash - ${String.format("%4d", stop - start)}ms - ${item.path}"
                        }

                        increaseProgress()
                        Triple(item, hash, hexHash)
                    }
                    emit(checksummedGroup)
                }
            }
            .toList()
            .flatten()
            .groupBy { hexHash -> hexHash.third }
            .filter { it.value.size >= 2 }

        val hashStop = System.currentTimeMillis()
        log(TAG) { "Checksum investigation took ${(hashStop - hashStart)}ms (${DEFAULT_CONCURRENCY})" }

        log(TAG) { "${hashBuckets.size} hash buckets of 2 or more items" }

        val duplicates = hashBuckets.values
            .map { dupes: List<Triple<APathLookup<*>, Hasher.Result, String>> ->
                val hexHash = dupes.first().third
                ChecksumDuplicate.Group(
                    identifier = Duplicate.Group.Id(hexHash),
                    duplicates = dupes
                        .map { (item, hash) ->
                            ChecksumDuplicate(
                                lookup = item,
                                hash = hash,
                            )
                        }
                        .toSet()
                )
            }

        return duplicates.toSet()
    }

    companion object {
        private val TAG = logTag("Deduplicator", "Sleuth", "Hash")
    }
}