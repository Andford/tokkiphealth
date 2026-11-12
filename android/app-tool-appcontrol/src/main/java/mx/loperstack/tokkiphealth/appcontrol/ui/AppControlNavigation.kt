package mx.loperstack.tokkiphealth.appcontrol.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.appcontrol.ui.list.AppControlListScreenHost
import mx.loperstack.tokkiphealth.appcontrol.ui.list.actions.AppActionSheetHost
import mx.loperstack.tokkiphealth.appcontrol.ui.settings.AppControlSettingsScreenHost
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import mx.loperstack.tokkiphealth.common.navigation.modalBottomSheetMetadata
import mx.loperstack.tokkiphealth.common.navigation.routes.AppControlListRoute
import javax.inject.Inject

class AppControlNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<AppControlSettingsRoute> { AppControlSettingsScreenHost() }
        entry<AppControlListRoute> { AppControlListScreenHost() }
        entry<AppActionRoute>(metadata = modalBottomSheetMetadata()) { route ->
            AppActionSheetHost(installId = route.installId)
        }
    }
}
