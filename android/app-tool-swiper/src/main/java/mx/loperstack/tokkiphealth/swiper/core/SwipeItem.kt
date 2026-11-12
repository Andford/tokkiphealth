package mx.loperstack.tokkiphealth.swiper.core

import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.APathLookup

data class SwipeItem(
    val id: Long,
    val sessionId: String,
    val itemIndex: Int,
    val lookup: APathLookup<APath>,
    val decision: SwipeDecision,
)
