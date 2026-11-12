package mx.loperstack.tokkiphealth.swiper.core.db

import androidx.room.TypeConverter
import mx.loperstack.tokkiphealth.swiper.core.SwipeDecision

class SwipeDecisionConverter {
    @TypeConverter
    fun fromValue(value: String): SwipeDecision = SwipeDecision.valueOf(value)

    @TypeConverter
    fun toValue(decision: SwipeDecision): String = decision.name
}
