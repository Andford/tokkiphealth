package mx.loperstack.tokkiphealth.common.previews

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.lookup
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val gatewaySwitch: GatewaySwitch,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    suspend fun resolveLookup(path: APath): APathLookup<*>? = try {
        path.lookup(gatewaySwitch)
    } catch (e: Exception) {
        log(TAG, WARN) { "resolveLookup($path) failed: ${e.asLog()}" }
        errorEvents.tryEmit(e)
        null
    }

    companion object {
        private val TAG = logTag("Preview", "ViewModel")
    }
}
