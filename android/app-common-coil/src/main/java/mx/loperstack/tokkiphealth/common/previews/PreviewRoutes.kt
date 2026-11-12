package mx.loperstack.tokkiphealth.common.previews

import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import kotlinx.serialization.Serializable

@Serializable
data class PreviewRoute(
    val options: PreviewOptions,
) : NavigationDestination
