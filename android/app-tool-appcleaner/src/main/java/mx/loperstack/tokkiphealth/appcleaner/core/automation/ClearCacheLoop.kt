package mx.loperstack.tokkiphealth.appcleaner.core.automation

import mx.loperstack.tokkiphealth.automation.core.errors.AUTOMATION_FAILURE_LIMIT
import mx.loperstack.tokkiphealth.automation.core.errors.AutomationOverlayException
import mx.loperstack.tokkiphealth.automation.core.errors.InvalidSystemStateException
import mx.loperstack.tokkiphealth.automation.core.errors.PlanAbortException
import mx.loperstack.tokkiphealth.automation.core.errors.UserCancelledAutomationException
import mx.loperstack.tokkiphealth.automation.core.errors.isAutomationUnusable
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.progress.increaseProgress
import mx.loperstack.tokkiphealth.common.progress.updateProgressCount
import mx.loperstack.tokkiphealth.common.progress.updateProgressPrimary
import mx.loperstack.tokkiphealth.common.progress.updateProgressSecondary
import kotlinx.coroutines.CancellationException

/**
 * Clears the cache of each `task.targets` entry via [clearOne] and records what happened to it.
 *
 * Both ways a target can come back cleared - [clearOne] returning, and a [PlanAbortException] the
 * spec flagged as `treatAsSuccess` - report through `task.onSuccess`. That callback is the only
 * channel that survives an interrupted run: the return value here never reaches AppCleaner if the
 * accessibility service dies mid-run.
 *
 * [resolveOne] runs per target, immediately before that target is cleared, so an early exit skips
 * the lookups for everything after it.
 *
 * [onTick] carries the caller's per-target bookkeeping (ops counter).
 */
internal suspend fun Progress.Client.clearCachesFor(
    task: ClearCacheTask,
    resolveOne: suspend (InstallId) -> Installed?,
    onTick: (Pkg.Id) -> Unit,
    clearOne: suspend (Installed) -> Unit,
): ClearCacheModule.ProcessedTask {
    val successful = mutableSetOf<InstallId>()
    val failed = mutableMapOf<InstallId, Exception>()
    var cancelledByUser = false

    for (target in task.targets) {
        val installed = resolveOne(target)

        if (installed == null) {
            log(TAG, WARN) { "$target is not in package repo" }
            failed[target] = IllegalStateException("$target is not in package repo")
            continue
        }

        log(TAG) { "Clearing cache for $installed" }
        updateProgressPrimary(installed.label ?: target.pkgId.name.toCaString())

        try {
            clearOne(installed)
            log(TAG, INFO) { "Successfully cleared cache for for $target" }
            task.onSuccess(target)
            successful.add(target)
        } catch (e: Exception) {
            when {
                e is InvalidSystemStateException -> {
                    log(TAG, WARN) { "Invalid system state for ACS based cache deletion: ${e.asLog()}" }
                    throw e
                }

                e.isAutomationUnusable() -> {
                    log(TAG, WARN) { "Automation unusable while processing $installed: ${e.asLog()}" }
                    task.onError(target, e)
                    failed[target] = e
                    val unusable = failed.count { it.value.isAutomationUnusable() }
                    if (successful.isEmpty() && unusable >= AUTOMATION_FAILURE_LIMIT) break
                }

                e is AutomationOverlayException -> {
                    log(TAG, ERROR) { "Automation overlay error: ${e.asLog()}" }
                    throw e
                }

                e is PlanAbortException && e.treatAsSuccess -> {
                    log(TAG, INFO) { "Treating aborted plan as success for $target:\n${e.asLog()}" }
                    task.onSuccess(target)
                    successful.add(target)
                }

                e is CancellationException -> {
                    log(TAG, WARN) { "We were cancelled: ${e.asLog()}" }
                    updateProgressPrimary(mx.loperstack.tokkiphealth.common.R.string.general_cancel_action)
                    updateProgressSecondary(CaString.EMPTY)
                    updateProgressCount(Progress.Count.Indeterminate())
                    if (e is UserCancelledAutomationException) {
                        log(TAG, INFO) { "User has cancelled automation process, aborting..." }
                        cancelledByUser = true
                        break
                    } else {
                        throw e
                    }
                }

                else -> {
                    log(TAG, WARN) { "Failure for $target:\n${e.asLog()}" }
                    task.onError(target, e)
                    failed[target] = e
                }
            }
        } finally {
            increaseProgress()
            onTick(target.pkgId)
        }
    }

    return ClearCacheModule.ProcessedTask(
        successful = successful,
        failed = failed,
        cancelledByUser = cancelledByUser,
    )
}

private val TAG: String = ClearCacheModule.TAG
