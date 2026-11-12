package mx.loperstack.tokkiphealth.main.ui.onboarding.versus

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.main.ui.navigation.OnboardingPrivacyRoute
import javax.inject.Inject

@HiltViewModel
class VersusSetupViewModel @Inject constructor(
    @Suppress("unused") private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
) : ViewModel4(dispatcherProvider = dispatcherProvider, tag = TAG) {

    fun onContinue() {
        navTo(OnboardingPrivacyRoute)
    }

    companion object {
        private val TAG = logTag("Onboarding", "Versus", "ViewModel")
    }
}
