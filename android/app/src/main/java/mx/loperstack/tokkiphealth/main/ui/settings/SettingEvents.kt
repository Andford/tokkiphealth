package mx.loperstack.tokkiphealth.main.ui.settings

sealed class SettingEvents {
    data class ShowVersionInfo(val info: String) : SettingEvents()
}
