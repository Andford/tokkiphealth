package mx.loperstack.tokkiphealth.common.navigation

import mx.loperstack.tokkiphealth.common.flow.SingleEventFlow

interface NavigationEventSource {
    val navEvents: SingleEventFlow<NavEvent>
}
