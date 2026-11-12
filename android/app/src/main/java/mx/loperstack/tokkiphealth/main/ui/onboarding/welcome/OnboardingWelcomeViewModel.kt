package mx.loperstack.tokkiphealth.main.ui.onboarding.welcome

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.main.ui.navigation.OnboardingPrivacyRoute
import mx.loperstack.tokkiphealth.main.ui.navigation.VersusSetupRoute
import javax.inject.Inject

@HiltViewModel
class OnboardingWelcomeViewModel @Inject constructor(
    @Suppress("unused") private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
) : ViewModel4(dispatcherProvider = dispatcherProvider, tag = TAG) {

    fun onContinue(isLegacySdmInstalled: Boolean) {
        if (isLegacySdmInstalled) {
            navTo(VersusSetupRoute)
        } else {
            navTo(OnboardingPrivacyRoute)
        }
    }

    companion object {
        private val TAG = logTag("Onboarding", "Welcome", "ViewModel")
    }
}
