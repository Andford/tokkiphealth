package mx.loperstack.tokkiphealth.common.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LayoutMode {
    @SerialName("LINEAR") LINEAR,
    @SerialName("GRID") GRID,
    ;
}
