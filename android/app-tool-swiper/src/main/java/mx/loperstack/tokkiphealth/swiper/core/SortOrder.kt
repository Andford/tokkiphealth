package mx.loperstack.tokkiphealth.swiper.core

import mx.loperstack.tokkiphealth.common.R
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.preferences.EnumPreference

enum class SortOrder(override val label: CaString) : EnumPreference<SortOrder> {
    OLDEST_FIRST(R.string.general_sort_oldest_first.toCaString()),
    NEWEST_FIRST(R.string.general_sort_newest_first.toCaString()),
    NAME_ASC(R.string.general_sort_name_asc.toCaString()),
    SIZE_DESC(R.string.general_sort_size_desc.toCaString()),
    ;

    companion object {
        val DEFAULT = OLDEST_FIRST
    }
}
