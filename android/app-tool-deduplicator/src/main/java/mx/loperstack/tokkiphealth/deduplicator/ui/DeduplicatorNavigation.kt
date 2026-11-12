package mx.loperstack.tokkiphealth.deduplicator.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import mx.loperstack.tokkiphealth.deduplicator.ui.details.DeduplicatorDetailsScreenHost
import mx.loperstack.tokkiphealth.deduplicator.ui.list.DeduplicatorListScreenHost
import mx.loperstack.tokkiphealth.deduplicator.ui.settings.DeduplicatorSettingsScreenHost
import mx.loperstack.tokkiphealth.deduplicator.ui.settings.arbiter.ArbiterConfigScreenHost
import javax.inject.Inject

class DeduplicatorNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<DeduplicatorSettingsRoute> { DeduplicatorSettingsScreenHost() }
        entry<ArbiterConfigRoute> { ArbiterConfigScreenHost() }
        entry<DeduplicatorListRoute> { DeduplicatorListScreenHost() }
        entry<DeduplicatorDetailsRoute> { route -> DeduplicatorDetailsScreenHost(route = route) }
    }
}
