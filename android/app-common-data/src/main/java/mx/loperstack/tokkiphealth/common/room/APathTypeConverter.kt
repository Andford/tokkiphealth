package mx.loperstack.tokkiphealth.common.room

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import dagger.Reusable
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.serialization.APathSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Reusable
@ProvidedTypeConverter
class APathTypeConverter @Inject constructor(
    private val json: Json,
) {
    @TypeConverter
    fun from(value: APath): String = json.encodeToString(APathSerializer, value)

    @TypeConverter
    fun to(value: String): APath = json.decodeFromString(APathSerializer, value)
}
