package mx.loperstack.tokkiphealth.stats.core

import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.withPrevious
import mx.loperstack.tokkiphealth.common.storage.StorageRescue
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskSubmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskStatsCoordinator @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    private val taskSubmitter: TaskSubmitter,
    private val statsRepo: StatsRepo,
    private val storageRescue: StorageRescue,
) {
    private val isStarted = AtomicBoolean(false)

    fun start() {
        if (!isStarted.compareAndSet(false, true)) return

        taskSubmitter.state
            .map { state -> state.tasks.associateBy { task -> task.id } }
            .withPrevious()
            .onEach { (oldTasks, newTasks) ->
                val oldById = oldTasks ?: emptyMap()
                val newlyCompleted = newTasks.values.filter { task ->
                    task.isComplete && oldById[task.id]?.isComplete != true
                }
                newlyCompleted.forEach { task ->
                    try {
                        statsRepo.report(task)
                    } catch (e: Exception) {
                        log(TAG, WARN) { "Failed to report task ${task.id}: ${e.asLog()}" }
                    }
                }
            }
            .launchIn(appScope)

        taskSubmitter.state
            .map { it.isIdle }
            .distinctUntilChanged()
            .withPrevious()
            .onEach { (oldIdle, newIdle) ->
                if (oldIdle == false && newIdle) {
                    try {
                        statsRepo.recordSnapshot(force = true)
                    } catch (e: Exception) {
                        log(TAG, WARN) { "Failed to record idle snapshot: ${e.asLog()}" }
                    }
                    // restoreIfPossible() catches all non-cancellation exceptions internally,
                    // so a wrapper try/catch here would be dead code.
                    storageRescue.restoreIfPossible()
                }
            }
            .launchIn(appScope)
    }

    companion object {
        private val TAG = logTag("Stats", "TaskCoordinator")
    }
}
