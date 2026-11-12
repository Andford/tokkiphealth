package mx.loperstack.tokkiphealth.main.ui.settings.cards

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.main.core.DashboardCardConfig
import mx.loperstack.tokkiphealth.main.core.DashboardCardType
import mx.loperstack.tokkiphealth.main.core.GeneralSettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class DashboardCardConfigViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val generalSettings: GeneralSettings,
) : ViewModel4(dispatcherProvider, TAG) {

    data class State(
        val entries: List<DashboardCardConfig.CardEntry> = emptyList(),
    )

    val state: StateFlow<State> = generalSettings.dashboardCardConfig.flow
        .map { config -> State(entries = config.cards) }
        .safeStateIn(State()) { State() }

    fun applyReorder(newOrder: List<DashboardCardType>) = launch {
        val current = generalSettings.dashboardCardConfig.value()
        val byType = current.cards.associateBy { it.type }
        val reordered = newOrder.mapNotNull { byType[it] }

        // If reorder does not include all known types (drag-in-progress snapshot drift), bail out.
        if (reordered.size != current.cards.size) {
            log(TAG) { "applyReorder($newOrder) ignored: size mismatch with ${current.cards.size}" }
            return@launch
        }

        if (reordered.map { it.type } == current.cards.map { it.type }) return@launch

        log(TAG) { "applyReorder(): ${reordered.map { it.type }}" }
        generalSettings.dashboardCardConfig.value(DashboardCardConfig(cards = reordered))
    }

    fun toggleVisibility(type: DashboardCardType) = launch {
        val current = generalSettings.dashboardCardConfig.value()
        val newCards = current.cards.map { entry ->
            if (entry.type == type) entry.copy(isVisible = !entry.isVisible) else entry
        }
        log(TAG) { "toggleVisibility($type): ${newCards.first { it.type == type }.isVisible}" }
        generalSettings.dashboardCardConfig.value(DashboardCardConfig(cards = newCards))
    }

    fun resetToDefaults() = launch {
        log(TAG) { "resetToDefaults()" }
        generalSettings.dashboardCardConfig.value(DashboardCardConfig())
    }

    companion object {
        private val TAG = logTag("Dashboard", "CardConfig", "ViewModel")
    }
}
