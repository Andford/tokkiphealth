package mx.loperstack.tokkiphealth.squeezer.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import mx.loperstack.tokkiphealth.squeezer.ui.list.SqueezerListScreenHost
import mx.loperstack.tokkiphealth.squeezer.ui.settings.SqueezerSettingsScreenHost
import mx.loperstack.tokkiphealth.squeezer.ui.setup.SqueezerSetupScreenHost
import javax.inject.Inject

class SqueezerNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<SqueezerSettingsRoute> { SqueezerSettingsScreenHost() }
        entry<SqueezerSetupRoute> { SqueezerSetupScreenHost() }
        entry<SqueezerListRoute> { SqueezerListScreenHost() }
    }
}
