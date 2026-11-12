package mx.loperstack.tokkiphealth.systemcleaner.ui.details.page

import mx.loperstack.tokkiphealth.systemcleaner.core.FilterContent
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.filterIdentifier
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock.EmptyDirectoryFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock.ScreenshotsFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock.TrashedFilter

internal sealed interface FilterContentElement {
    data class FileRow(
        val match: SystemCleanerFilter.Match,
        val showDate: Boolean,
        val showThumbnailPreview: Boolean,
    ) : FilterContentElement
}

internal fun buildFilterContentElements(content: FilterContent): List<FilterContentElement.FileRow> {
    val sorted = when (content.identifier) {
        EmptyDirectoryFilter::class.filterIdentifier -> content.items.sortedBy { it.path.path }
        TrashedFilter::class.filterIdentifier -> content.items.sortedByDescending { it.lookup.modifiedAt }
        ScreenshotsFilter::class.filterIdentifier -> content.items.sortedBy { it.lookup.modifiedAt }
        else -> content.items.sortedByDescending { it.expectedGain }
    }

    val showDate = when (content.identifier) {
        TrashedFilter::class.filterIdentifier -> true
        ScreenshotsFilter::class.filterIdentifier -> true
        else -> false
    }

    val showThumbnailPreview = when (content.identifier) {
        TrashedFilter::class.filterIdentifier -> true
        ScreenshotsFilter::class.filterIdentifier -> true
        else -> false
    }

    return sorted.map { match ->
        FilterContentElement.FileRow(
            match = match,
            showDate = showDate,
            showThumbnailPreview = showThumbnailPreview,
        )
    }
}
