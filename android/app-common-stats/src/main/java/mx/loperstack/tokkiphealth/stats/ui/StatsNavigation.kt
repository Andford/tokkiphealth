package mx.loperstack.tokkiphealth.stats.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import mx.loperstack.tokkiphealth.stats.ui.paths.AffectedPathsScreenHost
import mx.loperstack.tokkiphealth.stats.ui.pkgs.AffectedPkgsScreenHost
import mx.loperstack.tokkiphealth.stats.ui.reports.ReportsScreenHost
import mx.loperstack.tokkiphealth.stats.ui.settings.StatsSettingsScreenHost
import mx.loperstack.tokkiphealth.stats.ui.spacehistory.SpaceHistoryScreenHost
import javax.inject.Inject

class StatsNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<StatsSettingsRoute> { StatsSettingsScreenHost() }
        entry<ReportsRoute> { ReportsScreenHost() }
        entry<AffectedPkgsRoute> { route -> AffectedPkgsScreenHost(route = route) }
        entry<AffectedFilesRoute> { route -> AffectedPathsScreenHost(route = route) }
        entry<SpaceHistoryRoute> { route -> SpaceHistoryScreenHost(route = route) }
    }
}
