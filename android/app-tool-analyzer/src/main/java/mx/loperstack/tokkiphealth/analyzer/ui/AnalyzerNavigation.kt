package mx.loperstack.tokkiphealth.analyzer.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.analyzer.ui.settings.AnalyzerSettingsScreenHost
import mx.loperstack.tokkiphealth.analyzer.ui.storage.app.AppDetailsScreenHost
import mx.loperstack.tokkiphealth.analyzer.ui.storage.apps.AppsScreenHost
import mx.loperstack.tokkiphealth.analyzer.ui.storage.content.ContentScreenHost
import mx.loperstack.tokkiphealth.analyzer.ui.storage.device.DeviceStorageScreenHost
import mx.loperstack.tokkiphealth.analyzer.ui.storage.storage.StorageContentScreenHost
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import mx.loperstack.tokkiphealth.common.navigation.routes.DeviceStorageRoute
import javax.inject.Inject

class AnalyzerNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<AnalyzerSettingsRoute> { AnalyzerSettingsScreenHost() }
        entry<DeviceStorageRoute> { DeviceStorageScreenHost() }
        entry<StorageContentRoute> { route -> StorageContentScreenHost(route = route) }
        entry<AppsRoute> { route -> AppsScreenHost(route = route) }
        entry<AppDetailsRoute> { route -> AppDetailsScreenHost(route = route) }
        entry<ContentRoute> { route -> ContentScreenHost(route = route) }
    }
}
