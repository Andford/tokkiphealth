package mx.loperstack.tokkiphealth.main.ui.settings

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.BuildConfigWrap
import mx.loperstack.tokkiphealth.common.ClipboardHelper
import mx.loperstack.tokkiphealth.common.SdmSeLinks
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.SingleEventFlow
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.main.core.CurriculumVitae
import mx.loperstack.tokkiphealth.setup.SetupManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @Suppress("unused") val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val setupManager: SetupManager,
    private val webpageTool: WebpageTool,
    private val clipboardHelper: ClipboardHelper,
    private val curriculumVitae: CurriculumVitae,
) : ViewModel4(dispatcherProvider, TAG) {

    val events = SingleEventFlow<SettingEvents>()

    val state: StateFlow<State> = setupManager.state
        .map { State(setupDone = it.isDone) }
        .safeStateIn(
            initialValue = State(),
            onError = { State() },
        )

    fun openWebsite(url: String) {
        webpageTool.open(url)
    }

    fun openPrivacyPolicy() {
        webpageTool.open(SdmSeLinks.PRIVACY_POLICY)
    }

    private suspend fun getVersionText() = """
            Build: `${BuildConfigWrap.VERSION_DESCRIPTION}`
            Update history: `${curriculumVitae.history.first()}`
            ROM: `${Build.FINGERPRINT}`
        """.trimIndent()

    fun showVersionInfos() = launch {
        events.emit(SettingEvents.ShowVersionInfo(getVersionText()))
    }

    fun copyVersionInfos() = launch {
        clipboardHelper.copyToClipboard(getVersionText())
    }

    data class State(
        // Nullable = "not yet known"; avoids a one-frame flash of the setup tint for set-up users
        // before the first upstream emission under WhileSubscribed.
        val setupDone: Boolean? = null,
    )

    companion object {
        private val TAG = logTag("Settings", "ViewModel")
    }
}
