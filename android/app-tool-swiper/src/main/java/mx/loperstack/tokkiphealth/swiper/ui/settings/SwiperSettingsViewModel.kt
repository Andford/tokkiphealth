package mx.loperstack.tokkiphealth.swiper.ui.settings

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.combine
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.swiper.core.SwiperSettings
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SwiperSettingsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val settings: SwiperSettings,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider = dispatcherProvider, tag = TAG) {

    val state: StateFlow<State> = combine(
        settings.swapSwipeDirections.flow,
        settings.showFileDetailsOverlay.flow,
        settings.hapticFeedbackEnabled.flow,
    ) { swap, details, haptic ->
        State(
            swapSwipeDirections = swap,
            showFileDetailsOverlay = details,
            hapticFeedbackEnabled = haptic,
        )
    }.safeStateIn(
        initialValue = State(),
        onError = { State() },
    )

    fun setSwapSwipeDirections(value: Boolean) = launch {
        settings.swapSwipeDirections.value(value)
    }

    fun setShowFileDetailsOverlay(value: Boolean) = launch {
        settings.showFileDetailsOverlay.value(value)
    }

    fun setHapticFeedbackEnabled(value: Boolean) = launch {
        settings.hapticFeedbackEnabled.value(value)
    }

    fun onHelpClick() {
        webpageTool.open(HELP_URL)
    }

    data class State(
        val swapSwipeDirections: Boolean = false,
        val showFileDetailsOverlay: Boolean = true,
        val hapticFeedbackEnabled: Boolean = true,
    )

    companion object {
        private val TAG = logTag("Swiper", "Settings", "ViewModel")
        private const val HELP_URL = "https://sdmse.darken.eu/swiper#settings"
    }
}
