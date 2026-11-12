package mx.loperstack.tokkiphealth.systemcleaner.core.filter

import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.delete
import mx.loperstack.tokkiphealth.common.files.filterDistinctRoots
import mx.loperstack.tokkiphealth.common.flow.throttleLatest
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.progress.increaseProgress
import mx.loperstack.tokkiphealth.common.progress.updateProgressCount
import mx.loperstack.tokkiphealth.common.progress.updateProgressPrimary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

abstract class BaseSystemCleanerFilter : SystemCleanerFilter {

    private val progressPub = MutableStateFlow<Progress.Data?>(Progress.Data())
    override val progress: Flow<Progress.Data?> = progressPub.throttleLatest(250)

    override fun updateProgress(update: (Progress.Data?) -> Progress.Data?) {
        progressPub.value = update(progressPub.value)
    }

    suspend fun Collection<SystemCleanerFilter.Match.Deletion>.deleteAll(
        gatewaySwitch: GatewaySwitch
    ): Collection<SystemCleanerFilter.Processed> {
        val uniqueRoots = this.map { it.lookup }.filterDistinctRoots()
        val uniqueMatches = this.filter { it.lookup in uniqueRoots }

        if (uniqueMatches.size != uniqueRoots.size) {
            throw IllegalStateException("Matches (${uniqueMatches.size}) != Roots (${uniqueRoots.size})")
        }

        updateProgressCount(Progress.Count.Percent(uniqueRoots.size))

        return uniqueMatches.map { match ->
            updateProgressPrimary(match.lookup.userReadablePath)
            var error: Throwable? = null
            try {
                match.lookup.delete(gatewaySwitch, recursive = true)
            } catch (e: Exception) {
                log(identifier, WARN) { "Failed to delete ${match.lookup}" }
                error = e
            } finally {
                increaseProgress()
            }
            SystemCleanerFilter.Processed(
                match = match,
                error = error,
            )
        }
    }
}