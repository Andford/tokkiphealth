package mx.loperstack.tokkiphealth.deduplicator.ui.dialogs

import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate

/**
 * Describes what set of items the [PreviewDeletionDialog] should preview and confirm deletion of.
 * Mirrors the legacy `PreviewDeletionDialog.Mode` so dashboard/list/cluster call sites map 1:1.
 */
sealed interface PreviewDeletionMode {
    val allowDeleteAll: Boolean
    val count: Int
    val previews: List<APathLookup<*>>

    data class All(val clusters: Collection<Duplicate.Cluster>) : PreviewDeletionMode {
        override val allowDeleteAll: Boolean = false
        override val count: Int get() = clusters.size
        override val previews: List<APathLookup<*>> get() = clusters.map { it.previewFile }
    }

    data class Clusters(
        val clusters: Collection<Duplicate.Cluster>,
        override val allowDeleteAll: Boolean,
        /** Duplicate copies that will be deleted (keeper excluded). Null = caller didn't compute it. */
        val targetCount: Int? = null,
        /** Bytes freed by the deletion (keeper excluded). Null = caller didn't compute it. */
        val freeableSize: Long? = null,
    ) : PreviewDeletionMode {
        override val count: Int get() = clusters.size
        override val previews: List<APathLookup<*>> get() = clusters.map { it.previewFile }
    }

    data class Groups(
        val groups: Collection<Duplicate.Group>,
        override val allowDeleteAll: Boolean,
    ) : PreviewDeletionMode {
        override val count: Int get() = groups.size
        override val previews: List<APathLookup<*>> get() = groups.map { it.previewFile }
    }

    data class Duplicates(
        val duplicates: Collection<Duplicate>,
        override val allowDeleteAll: Boolean = false,
        /** What the dialog shows while the delete-all toggle is on. Null = the toggle doesn't change the list. */
        val deleteAllDuplicates: Collection<Duplicate>? = null,
    ) : PreviewDeletionMode {
        override val count: Int get() = duplicates.size
        override val previews: List<APathLookup<*>> get() = duplicates.map { it.lookup }
    }
}
