package mx.loperstack.tokkiphealth.main.ui.onboarding.privacy

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.SdmSeLinks
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.common.updater.UpdateChecker
import mx.loperstack.tokkiphealth.main.core.GeneralSettings
import mx.loperstack.tokkiphealth.main.core.motd.MotdSettings
import mx.loperstack.tokkiphealth.main.ui.navigation.OnboardingSetupRoute
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class OnboardingPrivacyViewModel @Inject constructor(
    @Suppress("unused") private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val generalSettings: GeneralSettings,
    private val motdSettings: MotdSettings,
    private val webpageTool: WebpageTool,
    private val updateChecker: UpdateChecker,
) : ViewModel4(dispatcherProvider = dispatcherProvider, tag = TAG) {

    val state: StateFlow<State> = combine(
        motdSettings.isMotdEnabled.flow,
        generalSettings.isUpdateCheckEnabled.flow,
        flow { emit(updateChecker.isCheckSupported()) },
    ) { isMotdEnabled, isUpdateCheckEnabled, isUpdateCheckSupported ->
        State(
            isMotdEnabled = isMotdEnabled,
            isUpdateCheckEnabled = isUpdateCheckEnabled,
            isUpdateCheckSupported = isUpdateCheckSupported,
        )
    }.safeStateIn(
        initialValue = State(),
        onError = { State() },
    )

    fun onContinue() {
        navTo(OnboardingSetupRoute)
    }

    fun goPrivacyPolicy() {
        log(TAG) { "goPrivacyPolicy()" }
        webpageTool.open(SdmSeLinks.PRIVACY_POLICY)
    }

    fun toggleMotd() {
        log(TAG) { "toggleMotd()" }
        // Use the suspend value() on vmScope instead of valueBlocking (runBlocking on the main thread).
        launch { motdSettings.isMotdEnabled.value(!motdSettings.isMotdEnabled.value()) }
    }

    fun toggleUpdateCheck() {
        log(TAG) { "toggleUpdateCheck()" }
        launch { generalSettings.isUpdateCheckEnabled.value(!generalSettings.isUpdateCheckEnabled.value()) }
    }

    data class State(
        val isMotdEnabled: Boolean = true,
        val isUpdateCheckEnabled: Boolean = true,
        val isUpdateCheckSupported: Boolean = false,
    )

    companion object {
        private val TAG = logTag("Onboarding", "Privacy", "ViewModel")
    }
}
