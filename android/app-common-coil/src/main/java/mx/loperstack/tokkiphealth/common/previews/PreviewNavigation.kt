package mx.loperstack.tokkiphealth.common.previews

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import javax.inject.Inject

class PreviewNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<PreviewRoute> { route -> PreviewScreenHost(route = route) }
    }
}
