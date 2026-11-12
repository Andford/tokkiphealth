package mx.loperstack.tokkiphealth.systemcleaner.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.common.filter.CustomFilterEditorRoute
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import mx.loperstack.tokkiphealth.common.navigation.routes.CustomFilterListRoute
import mx.loperstack.tokkiphealth.systemcleaner.ui.customfilter.editor.CustomFilterEditorScreenHost
import mx.loperstack.tokkiphealth.systemcleaner.ui.customfilter.list.CustomFilterListScreenHost
import mx.loperstack.tokkiphealth.systemcleaner.ui.details.FilterContentDetailsScreenHost
import mx.loperstack.tokkiphealth.systemcleaner.ui.list.SystemCleanerListScreenHost
import mx.loperstack.tokkiphealth.systemcleaner.ui.settings.SystemCleanerSettingsScreenHost
import javax.inject.Inject

class SystemCleanerNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<SystemCleanerSettingsRoute> { SystemCleanerSettingsScreenHost() }
        entry<CustomFilterListRoute> { CustomFilterListScreenHost() }
        entry<SystemCleanerListRoute> { SystemCleanerListScreenHost() }
        entry<FilterContentDetailsRoute> { route -> FilterContentDetailsScreenHost(route = route) }
        entry<CustomFilterEditorRoute> { route -> CustomFilterEditorScreenHost(route = route) }
    }
}
