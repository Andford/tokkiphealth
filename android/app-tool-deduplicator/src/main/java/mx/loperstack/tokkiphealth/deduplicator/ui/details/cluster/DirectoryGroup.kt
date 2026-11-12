package mx.loperstack.tokkiphealth.deduplicator.ui.details.cluster

import mx.loperstack.tokkiphealth.common.files.Segments
import mx.loperstack.tokkiphealth.common.files.joinSegments
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate

/**
 * Represents a group of duplicates that share the same parent directory.
 * Used in directory view mode to organize duplicates by folder location.
 */
data class DirectoryGroup(
    val parentSegments: Segments,
    val duplicates: List<Duplicate>,
) {
    val identifier: Id
        get() = Id(parentSegments.joinSegments())

    val count: Int
        get() = duplicates.size

    val totalSize: Long
        get() = duplicates.sumOf { it.size }

    data class Id(val value: String)
}
