package mx.loperstack.tokkiphealth.main.core.taskmanager

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import mx.loperstack.tokkiphealth.common.BuildConfigWrap
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TaskWorkerControl @Inject constructor(
    // Resolved on use, not at construction: resolving WorkManager initializes it (Room database, trackers).
    private val workerManager: Provider<WorkManager>,
    private val dispatcherProvider: DispatcherProvider,
) {

    suspend fun startMonitor() {
        val workerData = Data.Builder().apply {

        }.build()
        log(TAG, VERBOSE) { "Worker data: $workerData" }

        val workRequest = OneTimeWorkRequestBuilder<TaskWorker>().apply {
            setInputData(workerData)
            setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }.build()

        log(TAG, VERBOSE) { "Worker request: $workRequest" }

        val operation = workerManager.get().enqueueUniqueWork(
            "${BuildConfigWrap.APPLICATION_ID}.taskmanager.worker",
            ExistingWorkPolicy.KEEP,
            workRequest,
        )

        operation.result.get()
        log(TAG) { "Worker start request send." }
    }

    companion object {
        val TAG = logTag("TaskManager", "Worker", "Control")
    }
}