package mx.loperstack.tokkiphealth.common.preferences

import mx.loperstack.tokkiphealth.common.ca.CaString

interface EnumPreference<T : Enum<T>> {
    val label: CaString
}