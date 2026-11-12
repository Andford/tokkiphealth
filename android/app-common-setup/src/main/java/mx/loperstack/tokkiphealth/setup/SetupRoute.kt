package mx.loperstack.tokkiphealth.setup

import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import kotlinx.serialization.Serializable

@Serializable
data class SetupRoute(
    val options: SetupScreenOptions? = null,
) : NavigationDestination
