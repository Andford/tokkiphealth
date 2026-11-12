package mx.loperstack.tokkiphealth.analyzer.core.storage.categories

import mx.loperstack.tokkiphealth.analyzer.core.content.ContentGroup
import mx.loperstack.tokkiphealth.common.storage.StorageId

data class MediaCategory(
    override val storageId: StorageId,
    override val groups: Collection<ContentGroup>,
    val spaceUsedOverride: Long? = null,
    val isReadOnly: Boolean = false,
) : ContentCategory {

    override val spaceUsed: Long
        get() = spaceUsedOverride ?: groups.sumOf { it.groupSize }
}
