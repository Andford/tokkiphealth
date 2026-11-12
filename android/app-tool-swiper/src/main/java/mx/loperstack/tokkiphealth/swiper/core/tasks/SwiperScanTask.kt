package mx.loperstack.tokkiphealth.swiper.core.tasks

import mx.loperstack.tokkiphealth.swiper.R
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.getQuantityString2
import kotlinx.parcelize.Parcelize

@Parcelize
data class SwiperScanTask(
    val sessionId: String,
) : SwiperTask {

    sealed interface Result : SwiperTask.Result

    @Parcelize
    data class Success(
        val sessionId: String,
        val itemCount: Int,
    ) : Result {
        override val primaryInfo
            get() = caString {
                getQuantityString2(R.plurals.swiper_result_x_items_found, itemCount)
            }
    }
}
