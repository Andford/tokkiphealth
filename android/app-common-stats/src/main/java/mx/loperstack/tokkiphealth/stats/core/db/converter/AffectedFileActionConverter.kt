package mx.loperstack.tokkiphealth.stats.core.db.converter

import androidx.room.TypeConverter
import mx.loperstack.tokkiphealth.stats.core.AffectedPath

class AffectedFileActionConverter {
    @TypeConverter
    fun from(value: AffectedPath.Action): String = value.name

    @TypeConverter
    fun to(value: String): AffectedPath.Action = AffectedPath.Action.valueOf(value)
}