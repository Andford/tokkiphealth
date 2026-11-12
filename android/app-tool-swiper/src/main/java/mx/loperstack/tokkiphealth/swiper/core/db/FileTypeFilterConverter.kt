package mx.loperstack.tokkiphealth.swiper.core.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import dagger.Reusable
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.swiper.core.FileTypeFilter
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Reusable
@ProvidedTypeConverter
class FileTypeFilterConverter @Inject constructor(
    private val json: Json,
) {
    @TypeConverter
    fun from(value: FileTypeFilter?): String? {
        if (value == null) return null
        return json.encodeToString(FileTypeFilter.serializer(), value)
    }

    @TypeConverter
    fun to(value: String?): FileTypeFilter? {
        if (value == null) return null
        return try {
            json.decodeFromString(FileTypeFilter.serializer(), value)
        } catch (e: Exception) {
            log(TAG, ERROR) { "Failed to parse FileTypeFilter (${value.length} chars), falling back to no filter" }
            null
        }
    }

    companion object {
        private const val TAG = "FileTypeFilterConverter"
    }
}
