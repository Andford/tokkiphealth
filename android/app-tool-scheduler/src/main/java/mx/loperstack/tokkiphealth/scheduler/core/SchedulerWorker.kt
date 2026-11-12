package mx.loperstack.tokkiphealth.scheduler.core

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import mx.loperstack.tokkiphealth.common.adb.AdbManager
import mx.loperstack.tokkiphealth.common.adb.canUseAdbNow
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.error.hasCause
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.automation.core.errors.AutomationException
import mx.loperstack.tokkiphealth.automation.core.errors.AutomationSchedulerException
import mx.loperstack.tokkiphealth.common.root.canUseRootNow
import mx.loperstack.tokkiphealth.common.shell.ShellOps
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsCmd
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.main.core.taskmanager.SchedulerTaskFactory
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskSubmitter
import mx.loperstack.tokkiphealth.setup.SetupHealerSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take


@HiltWorker
class SchedulerWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    dispatcherProvider: DispatcherProvider,
    private val taskSubmitter: TaskSubmitter,
    private val schedulerTaskFactory: SchedulerTaskFactory,
    private val schedulerManager: SchedulerManager,
    private val schedulerNotifications: SchedulerNotifications,
    private val setupHealer: SetupHealerSource,
    private val rootManager: RootManager,
    private val adbManager: AdbManager,
    private val shellOps: ShellOps,
) : CoroutineWorker(context, params) {

    init {
        log(TAG, VERBOSE) { "init(): workerId=$id" }
    }

    private val workerScope = CoroutineScope(dispatcherProvider.Default + SupervisorJob())
    private var finishedWithError = false
    private var currentSchedule: Schedule? = null

    private val scheduleId: ScheduleId
        get() = inputData.getString(INPUT_SCHEDULE_ID) as ScheduleId

    private suspend fun getSchedule(): Schedule {
        return schedulerManager.getSchedule(scheduleId) ?: throw IllegalStateException("Schedule not found $scheduleId")
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val schedule = currentSchedule
        return if (schedule != null) {
            schedulerNotifications.getForegroundInfo(schedule)
        } else {
            schedulerNotifications.getForegroundInfo(scheduleId)
        }
    }

    override suspend fun doWork(): Result = try {
        log(TAG, VERBOSE) { "Executing $inputData now (runAttemptCount=$runAttemptCount)" }

        try {
            log(TAG) { "Declaring as foreground task" }
            setForeground(getForegroundInfo())
            log(TAG, INFO) { "Foreground state declared!" }
        } catch (e: IllegalStateException) {
            log(TAG, ERROR) { "Can't execute in foreground: ${e.asLog()}" }
        }

        val schedule = getSchedule().also { currentSchedule = it }

        if (runAttemptCount > 0) {
            log(TAG, WARN) { "Repeat execution attempt ($runAttemptCount) for $schedule" }
            // The next occurrence is appended as a dependent of this still RUNNING work (see `finally`).
            // Returning failure would make WorkManager fail the appended successor too,
            // permanently severing the recurring chain until the next app start re-arms it.
            Result.success(inputData)
        } else {
            val start = System.currentTimeMillis()
            log(TAG, INFO) { "Executing schedule $schedule" }
            Bugs.leaveBreadCrumb("Executing schedule")

            try {
                setForeground(schedulerNotifications.getForegroundInfo(schedule))
            } catch (e: IllegalStateException) {
                log(TAG, ERROR) { "Can't update foreground info, falling back to notifyState: ${e.asLog()}" }
                schedulerNotifications.notifyState(schedule)
            }

            doDoWork(schedule)

            val duration = System.currentTimeMillis() - start
            log(TAG, INFO) { "Execution finished after ${duration}ms: $schedule" }

            Result.success(inputData)
        }
    } catch (e: Throwable) {
        if (e !is CancellationException) {
            log(TAG, ERROR) { "Execution failed: ${e.asLog()}" }
            finishedWithError = true
            try {
                schedulerNotifications.notifyError(scheduleId)
            } catch (e2: Exception) {
                log(TAG, ERROR) { "Failed to notify about error (error=$e): ${e2.asLog()}" }
            }

            // Not Result.failure(...), that would sever the recurring chain, see above.
            Result.success(inputData)
        } else {
            Result.success()
        }
    } finally {
        // First, so stray task jobs can't outlive the worker even if cleanup below rethrows cancellation
        try {
            workerScope.cancel("Worker finished (withError?=$finishedWithError).")
        } catch (e: Exception) {
            log(TAG, ERROR) { "Failed to cancel worker scope (error=$finishedWithError): ${e.asLog()}" }
        }

        try {
            schedulerManager.updateExecutedNow(scheduleId)
        } catch (e: Exception) {
            // On cancellation (e.g. schedule disabled) the schedule must not be re-armed
            if (e is CancellationException) throw e
            log(TAG, ERROR) { "Failed to update execution timestamp (error=$finishedWithError): ${e.asLog()}" }
        }

        try {
            schedulerNotifications.cancel(scheduleId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            log(TAG, ERROR) { "Failed to clean up notifications (error=$finishedWithError): ${e.asLog()}" }
        }

        try {
            schedulerManager.reschedule(scheduleId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            log(TAG, ERROR) { "Failed to reschedule (error=$finishedWithError): ${e.asLog()}" }
        }
    }

    private suspend fun doDoWork(schedule: Schedule) {
        val enabledTools = buildSet {
            if (schedule.useCorpseFinder) add(SDMTool.Type.CORPSEFINDER)
            if (schedule.useSystemCleaner) add(SDMTool.Type.SYSTEMCLEANER)
            if (schedule.useAppCleaner) add(SDMTool.Type.APPCLEANER)
        }
        val tasks = schedulerTaskFactory.createTasks(schedule.id, enabledTools)

        delay(1000)

        // If the worker was launched from a cold-app-start some permission may be broken
        // Instead of driving straight into an error, let's slightly delay and see if we can fix any setup issues
        setupHealer.state
            .filter { it.healAttemptCount > 0 }
            .take(1)
            .first()

        val taskJobs = tasks.map { task ->
            workerScope.async {
                try {
                    log(TAG) { "Launching $task" }
                    // Scheduler aggregates results into its own notification (SchedulerNotifications.notifyResult),
                    // so opt out of the per-task background completion notification.
                    val result = taskSubmitter.submit(task, notifyOnFinish = false)
                    log(TAG) { "Finished $task -> $result" }
                    SchedulerNotifications.Results(task, result = result)
                } catch (e: Exception) {
                    log(TAG, ERROR) { "Scheduler task failed ($task): ${e.asLog()}" }
                    val wrappedError = if (e.hasCause(AutomationException::class)) {
                        AutomationSchedulerException(e)
                    } else {
                        e
                    }
                    SchedulerNotifications.Results(task, error = wrappedError)
                }
            }
        }

        log(TAG) { "Waiting for jobs to complete: $taskJobs" }
        val taskResults = taskJobs.awaitAll().toSet()
        schedulerNotifications.notifyResult(schedule.id, taskResults)
        log(TAG) { "All task jobs have finished." }

        schedule.commandsAfterSchedule.takeIf { it.isNotEmpty() }?.let { cmds ->
            log(TAG, INFO) { "Post-schedule commands are available" }
            cmds.forEachIndexed { index, s -> log(TAG, INFO) { "Command #$index: $s" } }

            val shellOpsMode = when {
                rootManager.canUseRootNow() -> ShellOps.Mode.ROOT
                adbManager.canUseAdbNow() -> ShellOps.Mode.ADB
                else -> ShellOps.Mode.NORMAL
            }
            val result = shellOps.execute(ShellOpsCmd(cmds = cmds), shellOpsMode)
            log(TAG, INFO) { "Post-schedule ShellOps result: $result" }
        }
    }

    companion object {
        const val INPUT_SCHEDULE_ID = "scheduler.worker.input.scheduleid"
        val TAG = logTag("Scheduler", "Worker")
    }
}
