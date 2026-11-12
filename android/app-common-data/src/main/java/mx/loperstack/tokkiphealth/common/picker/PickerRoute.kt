package mx.loperstack.tokkiphealth.common.picker

import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import kotlinx.serialization.Serializable

@Serializable
data class PickerRoute(
    val request: PickerRequest,
) : NavigationDestination
