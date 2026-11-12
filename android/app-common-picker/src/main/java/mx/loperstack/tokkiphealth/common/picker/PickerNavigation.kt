package mx.loperstack.tokkiphealth.common.picker

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import javax.inject.Inject

class PickerNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<PickerRoute> { route -> PickerScreenHost(route = route) }
    }
}
