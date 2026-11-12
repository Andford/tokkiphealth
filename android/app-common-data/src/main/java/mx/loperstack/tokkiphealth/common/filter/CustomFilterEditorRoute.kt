package mx.loperstack.tokkiphealth.common.filter

import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import kotlinx.serialization.Serializable

@Serializable
data class CustomFilterEditorRoute(
    val initial: CustomFilterEditorOptions? = null,
    val identifier: String? = null,
) : NavigationDestination
