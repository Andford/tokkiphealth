package mx.loperstack.tokkiphealth.appcleaner.ui

import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import kotlinx.serialization.Serializable

@Serializable
data object AppCleanerSettingsRoute : NavigationDestination

@Serializable
data object AppCleanerListRoute : NavigationDestination

@Serializable
data class AppJunkDetailsRoute(
    val identifier: InstallId? = null,
) : NavigationDestination
