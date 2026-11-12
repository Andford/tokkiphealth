package mx.loperstack.tokkiphealth.common.room

import androidx.room.TypeConverter
import mx.loperstack.tokkiphealth.main.core.SDMTool

class SDMToolTypeConverter {
    @TypeConverter
    fun from(value: SDMTool.Type): String = value.name

    @TypeConverter
    fun to(value: String): SDMTool.Type = SDMTool.Type.valueOf(value)
}