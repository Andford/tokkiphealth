package mx.loperstack.tokkiphealth.common.theming

import mx.loperstack.tokkiphealth.common.R
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.preferences.EnumPreference
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeColor(override val label: CaString) : EnumPreference<ThemeColor> {
    @SerialName("GREEN") GREEN(R.string.app_name.toCaString()),
    @SerialName("SUNSET") SUNSET(R.string.ui_theme_color_sunset_label.toCaString()),
    @SerialName("AMOLED") AMOLED(R.string.ui_theme_color_amoled_label.toCaString()),
}
