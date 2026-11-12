package mx.loperstack.tokkiphealth.common.clutter.manual

import android.content.Context
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.openAsset
import kotlinx.serialization.json.Json
import okio.IOException
import okio.buffer
import javax.inject.Inject

@Reusable
class JsonMarkerParser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {

    fun fromAssets(assetPath: String): List<JsonMarkerGroup> {
        val startTime = System.currentTimeMillis()

        val markers: List<JsonMarkerGroup> = try {
            context.openAsset(assetPath).buffer().use { source ->
                json.decodeFromString<List<JsonMarkerGroup>>(source.readUtf8())
            }
        } catch (e: Exception) {
            throw IOException("Failed to load asset: $assetPath", e)
        }

        val stopTimeDeserialization = System.currentTimeMillis()
        log(TAG) { "Deserialization took ${stopTimeDeserialization - startTime}ms for ${markers.size} entries" }
        return markers
    }

    companion object {
        val TAG: String = logTag("Clutter", "RawManualMarkerParser")
    }
}