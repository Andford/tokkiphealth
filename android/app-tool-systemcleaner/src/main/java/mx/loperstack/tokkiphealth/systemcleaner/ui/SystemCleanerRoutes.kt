package mx.loperstack.tokkiphealth.systemcleaner.ui

import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import kotlinx.serialization.Serializable

@Serializable
data object SystemCleanerSettingsRoute : NavigationDestination

@Serializable
data object SystemCleanerListRoute : NavigationDestination

@Serializable
data class FilterContentDetailsRoute(
    val filterIdentifier: String? = null,
) : NavigationDestination
