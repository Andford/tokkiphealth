package mx.loperstack.tokkiphealth.squeezer.ui

import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import kotlinx.serialization.Serializable

@Serializable
data object SqueezerSetupRoute : NavigationDestination

@Serializable
data object SqueezerListRoute : NavigationDestination

@Serializable
data object SqueezerSettingsRoute : NavigationDestination
