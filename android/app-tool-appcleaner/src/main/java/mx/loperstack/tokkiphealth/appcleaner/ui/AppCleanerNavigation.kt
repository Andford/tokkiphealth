package mx.loperstack.tokkiphealth.appcleaner.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.appcleaner.ui.details.AppJunkDetailsScreenHost
import mx.loperstack.tokkiphealth.appcleaner.ui.list.AppCleanerListScreenHost
import mx.loperstack.tokkiphealth.appcleaner.ui.settings.AppCleanerSettingsScreenHost
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import javax.inject.Inject

class AppCleanerNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<AppCleanerSettingsRoute> { AppCleanerSettingsScreenHost() }
        entry<AppCleanerListRoute> { AppCleanerListScreenHost() }
        entry<AppJunkDetailsRoute> { route -> AppJunkDetailsScreenHost(route = route) }
    }
}
