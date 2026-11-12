package mx.loperstack.tokkiphealth.main.ui.settings.acks

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import javax.inject.Inject

@HiltViewModel
class AcknowledgementsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider, TAG) {

    fun openUrl(url: String) {
        webpageTool.open(url)
    }

    companion object {
        private val TAG = logTag("Settings", "Acknowledgements", "ViewModel")
    }
}
