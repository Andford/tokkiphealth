package mx.loperstack.tokkiphealth.swiper.core.db

import androidx.room.TypeConverter
import mx.loperstack.tokkiphealth.swiper.core.SessionState

class SessionStateConverter {
    @TypeConverter
    fun fromValue(value: String): SessionState = SessionState.valueOf(value)

    @TypeConverter
    fun toValue(state: SessionState): String = state.name
}
