package mx.loperstack.tokkiphealth.systemcleaner.core

import androidx.compose.ui.graphics.vector.ImageVector
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.FilterIdentifier
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter

data class FilterContent(
    val identifier: FilterIdentifier,
    val icon: ImageVector,
    val label: CaString,
    val description: CaString,
    val items: Collection<SystemCleanerFilter.Match>
) {
    val size: Long
        get() = items.sumOf { it.lookup.size }
}
