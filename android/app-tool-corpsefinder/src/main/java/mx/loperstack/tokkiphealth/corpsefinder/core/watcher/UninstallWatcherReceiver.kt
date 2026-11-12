package mx.loperstack.tokkiphealth.corpsefinder.core.watcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.corpsefinder.core.CorpseFinderSettings
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.UninstallWatcherTask
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskSubmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class UninstallWatcherReceiver : BroadcastReceiver() {

    @Inject @AppScope lateinit var appScope: CoroutineScope
    @Inject lateinit var taskSubmitter: TaskSubmitter
    @Inject lateinit var corpseFinderSettings: CorpseFinderSettings

    override fun onReceive(context: Context, intent: Intent) {
        log(TAG) { "onReceive($context,$intent)" }
        if (intent.action != Intent.ACTION_PACKAGE_FULLY_REMOVED) {
            log(TAG, ERROR) { "Unknown intent: $intent" }
            return
        }

        val uri = intent.data
        val pkg = uri?.schemeSpecificPart?.toPkgId()
        if (pkg == null) {
            log(TAG, ERROR) { "Package data was null" }
            return
        }

        log(TAG, INFO) { "$pkg was uninstalled" }

        val asyncPi = goAsync()

        Bugs.leaveBreadCrumb("Uninstall event")

        appScope.launch {
            if (!corpseFinderSettings.isWatcherEnabled.value()) {
                log(TAG, WARN) { "Uninstall watcher is disabled in settings, skipping." }
                return@launch
            }

            val task = UninstallWatcherTask(
                target = pkg,
                autoDelete = corpseFinderSettings.isWatcherAutoDeleteEnabled.value()
            )
            try {
                log(TAG) { "Submitting task: $task" }
                taskSubmitter.submit(task, notifyOnFinish = false)
            } catch (e: Exception) {
                log(TAG, ERROR) { "Uninstall task ($task) failed: ${e.asLog()}" }
            }
        }

        appScope.launch {
            delay(3000)
            log(TAG) { "Finished watcher trigger" }
            asyncPi.finish()
        }
    }

    companion object {
        internal val TAG = logTag("CorpseFinder", "Watcher", "Uninstall", "Receiver")
    }
}
