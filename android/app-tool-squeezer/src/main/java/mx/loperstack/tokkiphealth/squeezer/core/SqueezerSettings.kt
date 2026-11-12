@file:UseSerializers(APathSerializer::class)

package mx.loperstack.tokkiphealth.squeezer.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.datastore.createValue
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.serialization.APathSerializer
import mx.loperstack.tokkiphealth.common.ui.LayoutMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SqueezerSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    json: Json,
) {

    private val Context.dataStore by preferencesDataStore(name = "settings_squeezer")

    val dataStore: DataStore<Preferences>
        get() = context.dataStore

    val minSizeBytes = dataStore.createValue<Long>("filter.minsize.bytes", MIN_FILE_SIZE)
    val minAge = dataStore.createValue("filter.minage", MIN_AGE_DEFAULT, json)
    val compressionQuality = dataStore.createValue("compression.quality", DEFAULT_QUALITY)
    val includeJpeg = dataStore.createValue("filter.type.jpeg.enabled", true)
    val includeWebp = dataStore.createValue("filter.type.webp.enabled", true)
    val includeHeic = dataStore.createValue("filter.type.heic.enabled", false)
    val includeVideo = dataStore.createValue("filter.type.video.enabled", false)

    /**
     * When false (default), photos carrying an HDR gain map or depth/portrait map are skipped at
     * scan time so compression can't silently flatten them to SDR / strip their depth. Opt in to
     * compress them anyway. Format-wide (JPEG Ultra HDR + HEIC), see [mx.loperstack.tokkiphealth.squeezer.core.scanner.LossyAuxDetector].
     */
    val includeLossyAuxImages = dataStore.createValue("filter.image.lossyaux.include", false)

    /** When false (default), JPEG Motion Photos are skipped so the re-encode can't drop their clip. */
    val includeMotionPhotos = dataStore.createValue("filter.image.motionphoto.include", false)

    /**
     * When false (default), images the encoder would decode at reduced resolution are skipped
     * (see [mx.loperstack.tokkiphealth.squeezer.core.processor.ImageCompressor.willDownscale]).
     */
    val includeOversizedImages = dataStore.createValue("filter.image.oversized.include", false)
    val skipPreviouslyCompressed = dataStore.createValue("skip.previously.compressed", true)
    val writeExifMarker = dataStore.createValue("compression.exif.marker.enabled", false)
    val scanPaths = dataStore.createValue("scan.location.paths", ScanPaths(), json)
    val layoutMode = dataStore.createValue("ui.list.layoutmode", LayoutMode.GRID, json)

    @Serializable
    data class ScanPaths(
        @SerialName("paths") val paths: Set<APath> = emptySet(),
    )

    companion object {
        const val MIN_FILE_SIZE = 512 * 1024L
        const val DEFAULT_QUALITY = 80
        val MIN_AGE_DEFAULT: Duration = Duration.ofDays(90)
        internal val TAG = logTag("Squeezer", "Settings")
    }
}
