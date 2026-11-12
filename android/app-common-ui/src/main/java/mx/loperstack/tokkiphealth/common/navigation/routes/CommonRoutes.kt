package mx.loperstack.tokkiphealth.common.navigation.routes

import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute : NavigationDestination

@Serializable
data class UpgradeRoute(
    val forced: Boolean = false,
    val manage: Boolean = false,
) : NavigationDestination

@Serializable
data object DataAreasRoute : NavigationDestination

@Serializable
data object AppControlListRoute : NavigationDestination

@Serializable
data object DeviceStorageRoute : NavigationDestination

@Serializable
data object SwiperSessionsRoute : NavigationDestination

@Serializable
data object CustomFilterListRoute : NavigationDestination
