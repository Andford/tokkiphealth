package mx.loperstack.tokkiphealth.corpsefinder.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import mx.loperstack.tokkiphealth.corpsefinder.ui.details.CorpseDetailsScreenHost
import mx.loperstack.tokkiphealth.corpsefinder.ui.list.CorpseFinderListScreenHost
import mx.loperstack.tokkiphealth.corpsefinder.ui.settings.CorpseFinderSettingsScreenHost
import javax.inject.Inject

class CorpseFinderNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<CorpseFinderSettingsRoute> { CorpseFinderSettingsScreenHost() }
        entry<CorpseFinderListRoute> { CorpseFinderListScreenHost() }
        entry<CorpseDetailsRoute> { route -> CorpseDetailsScreenHost(route = route) }
    }
}
