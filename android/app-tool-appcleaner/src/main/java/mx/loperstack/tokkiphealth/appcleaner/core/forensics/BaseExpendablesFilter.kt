package mx.loperstack.tokkiphealth.appcleaner.core.forensics

import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathAncestorIndex
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.PathException
import mx.loperstack.tokkiphealth.common.files.delete
import mx.loperstack.tokkiphealth.common.files.exists
import mx.loperstack.tokkiphealth.common.files.filterDistinctRoots
import mx.loperstack.tokkiphealth.common.flow.throttleLatest
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.progress.increaseProgress
import mx.loperstack.tokkiphealth.common.progress.updateProgressCount
import mx.loperstack.tokkiphealth.common.progress.updateProgressPrimary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

abstract class BaseExpendablesFilter : ExpendablesFilter {

    private val progressPub = MutableStateFlow<Progress.Data?>(Progress.Data())
    override val progress: Flow<Progress.Data?> = progressPub.throttleLatest(250)

    override fun updateProgress(update: (Progress.Data?) -> Progress.Data?) {
        progressPub.value = update(progressPub.value)
    }

    suspend fun deleteAll(
        targets: Collection<ExpendablesFilter.Match.Deletion>,
        gatewaySwitch: GatewaySwitch,
        allMatches: Collection<ExpendablesFilter.Match>,
    ): ExpendablesFilter.ProcessResult {
        log(TAG, INFO) { "deleteAll(...) Processing ${targets.size} out of ${allMatches.size} matches" }
        updateProgressPrimary(mx.loperstack.tokkiphealth.common.R.string.general_progress_preparing)
        val successful = mutableSetOf<ExpendablesFilter.Match>()
        val failed = mutableSetOf<Pair<ExpendablesFilter.Match, Exception>>()

        log(TAG, VERBOSE) { "Checking distinct roots..." }
        val distinctRoots = targets.map { it.lookup }.filterDistinctRoots()

        if (distinctRoots.size != targets.size) {
            log(TAG, INFO) { "${targets.size} match objects but only ${distinctRoots.size} distinct roots" }
            if (Bugs.isTrace) {
                targets
                    .filter { !distinctRoots.contains(it.lookup) }
                    .forEachIndexed { index, item -> log(TAG, INFO) { "Non distinct root #$index: $item" } }
            }
        }

        log(TAG) { "Got ${distinctRoots.size} distinct roots" }
        updateProgressCount(Progress.Count.Percent(distinctRoots.size))

        val mainByLookup = HashMap<APathLookup<*>, ExpendablesFilter.Match.Deletion>(targets.size)
        targets.forEach { mainByLookup.getOrPut(it.lookup) { it } }

        val rootIndex = APathAncestorIndex<APathLookup<*>> { it.lookedUp }
        distinctRoots.forEach { rootIndex.add(it) }
        val descendantsByRoot = HashMap<APathLookup<*>, MutableList<ExpendablesFilter.Match>>()
        allMatches.forEach { match ->
            rootIndex.ancestorsOf(match.lookup.lookedUp).forEach { root ->
                descendantsByRoot.getOrPut(root) { ArrayList() }.add(match)
            }
        }

        distinctRoots.forEach { targetRoot ->
            log(TAG) { "Processing root: $targetRoot" }
            updateProgressPrimary(targetRoot.userReadablePath)
            val main = mainByLookup.getValue(targetRoot)

            val mainDeleted = try {
                targetRoot.delete(gatewaySwitch, recursive = true)
                log(TAG) { "Main match deleted: $main" }
                true
            } catch (oge: PathException) {
                try {
                    if (targetRoot.exists(gatewaySwitch)) {
                        log(TAG, WARN) { "Deletion failed, file still exists" }
                        failed.add(main to oge)
                        false
                    } else {
                        log(TAG, WARN) { "Deletion failed as file no longer exists, okay..." }
                        true
                    }
                } catch (e: PathException) {
                    log(TAG, ERROR) { "Post-deletion-failure-exist check failed too on $main\n $e" }
                    failed.add(main to e)
                    false
                }
            }

            // deleteAll(...) starts at leafs, children may have been deleted, even if the top-level dir wasn't
            val affected = descendantsByRoot[targetRoot].orEmpty().filter { it != main }
            if (Bugs.isTrace) {
                log(TAG) { "$main affects ${affected.size} other matches" }
                affected.forEach { log(TAG, VERBOSE) { "Affected: $it" } }
            }

            if (mainDeleted) {
                successful.add(main)
                successful.addAll(affected)
                log(TAG) { "Main match and affected files deleted" }
            } else {
                log(TAG, WARN) { "Main match failed to delete, checking what still exists" }
                affected.forEach { subMatch ->
                    if (subMatch.path.exists(gatewaySwitch)) {
                        log(TAG, WARN) { "Sub match still exists: $subMatch" }
                    } else {
                        log(TAG, INFO) { "Sub match no longer exists: $subMatch" }
                        successful.add(subMatch)
                    }
                }
            }
            increaseProgress()
        }

        return ExpendablesFilter.ProcessResult(
            success = successful,
            failed = failed,
        )
    }

    suspend fun APathLookup<*>.toDeletionMatch() = ExpendablesFilter.Match.Deletion(identifier, this)


    companion object {
        private val TAG = logTag("AppCleaner", "BaseExpendablesFilter")
    }
}