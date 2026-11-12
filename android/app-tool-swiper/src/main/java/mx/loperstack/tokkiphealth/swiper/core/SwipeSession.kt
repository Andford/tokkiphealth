package mx.loperstack.tokkiphealth.swiper.core

import mx.loperstack.tokkiphealth.common.files.APath
import java.time.Instant

data class SwipeSession(
    val sessionId: String,
    val sourcePaths: List<APath>,
    val currentIndex: Int,
    val totalItems: Int,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val state: SessionState,
    val label: String? = null,
    val keptCount: Int = 0,
    val deletedCount: Int = 0,
    val fileTypeFilter: FileTypeFilter = FileTypeFilter.EMPTY,
    val sortOrder: SortOrder = SortOrder.OLDEST_FIRST,
)
