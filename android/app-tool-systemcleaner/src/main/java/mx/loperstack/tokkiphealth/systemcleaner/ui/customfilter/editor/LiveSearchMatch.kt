package mx.loperstack.tokkiphealth.systemcleaner.ui.customfilter.editor

import mx.loperstack.tokkiphealth.common.files.APathLookup

/**
 * Domain row for a single hit shown in the live-search bottom sheet.
 * Replaces the legacy `LiveSearchListRow.Item` ViewHolder data class so the VM no longer
 * depends on RecyclerView types.
 */
data class LiveSearchMatch(val lookup: APathLookup<*>) {
    val id: String get() = lookup.path.toString()
}
