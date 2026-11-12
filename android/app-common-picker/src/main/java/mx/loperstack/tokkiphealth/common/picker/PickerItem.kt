package mx.loperstack.tokkiphealth.common.picker

import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.files.APathLookup

data class PickerItem(
    val lookup: APathLookup<*>,
    val parent: PickerItem?,
    val dataArea: DataArea,
    val selected: Boolean,
    val selectable: Boolean,
)