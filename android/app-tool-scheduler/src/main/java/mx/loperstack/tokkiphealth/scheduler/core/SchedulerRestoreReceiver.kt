package mx.loperstack.tokkiphealth.scheduler.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class SchedulerRestoreReceiver : BroadcastReceiver() {

    @Inject @AppScope lateinit var appScope: CoroutineScope
    @Inject lateinit var schedulerManager: SchedulerManager

    override fun onReceive(context: Context, intent: Intent) {
        log(TAG) { "onReceive($context,$intent)" }
        if (!ALLOWED_INTENTS.contains(intent.action)) {
            log(TAG, ERROR) { "Unknown intent: $intent" }
            return
        }

        log(TAG, INFO) { "Rechecking scheduler states (intent.data=${intent.data})" }

        val asyncPi = goAsync()

        Bugs.leaveBreadCrumb("Scheduler restore")

        appScope.launch {
            schedulerManager.state.take(1).first()
            // The manager checks the scheduling states automatically when initialised, so just give it some time
            delay(3000)

            log(TAG) { "Finished scheduler checks" }
            asyncPi.finish()
        }
    }

    companion object {
        private val ALLOWED_INTENTS = setOf(
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_FULLY_REMOVED,
            Intent.ACTION_BOOT_COMPLETED
        )
        internal val TAG = logTag("Scheduler", "Receiver", "Restore")
    }
}
