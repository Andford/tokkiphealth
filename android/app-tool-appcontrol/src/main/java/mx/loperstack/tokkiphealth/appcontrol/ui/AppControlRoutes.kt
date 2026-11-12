package mx.loperstack.tokkiphealth.appcontrol.ui

import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import kotlinx.serialization.Serializable

@Serializable
data object AppControlSettingsRoute : NavigationDestination

@Serializable
data class AppActionRoute(
    val installId: InstallId,
) : NavigationDestination
