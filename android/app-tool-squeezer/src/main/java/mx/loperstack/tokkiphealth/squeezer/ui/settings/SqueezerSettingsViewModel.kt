package mx.loperstack.tokkiphealth.squeezer.ui.settings

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.combine
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.squeezer.core.CompressibleImage
import mx.loperstack.tokkiphealth.squeezer.core.SqueezerSettings
import mx.loperstack.tokkiphealth.squeezer.core.history.CompressionHistoryDatabase
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject


@HiltViewModel
class SqueezerSettingsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val settings: SqueezerSettings,
    private val historyDatabase: CompressionHistoryDatabase,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    val state: StateFlow<State> = combine(
        settings.includeJpeg.flow,
        settings.includeWebp.flow,
        settings.includeHeic.flow,
        settings.includeVideo.flow,
        settings.includeLossyAuxImages.flow,
        settings.includeMotionPhotos.flow,
        settings.includeOversizedImages.flow,
        settings.skipPreviouslyCompressed.flow,
        settings.writeExifMarker.flow,
        settings.minSizeBytes.flow,
        historyDatabase.count,
        historyDatabase.databaseSize,
    ) { jpeg, webp, heic, video, lossyAux, motion, oversized, skipCompressed, exif, minSize, historyCount, historySize ->
        State(
            includeJpeg = jpeg,
            includeWebp = webp,
            includeHeic = heic,
            isHeicSupported = CompressibleImage.isHeicEncodingSupported(),
            includeVideo = video,
            includeLossyAuxImages = lossyAux,
            includeMotionPhotos = motion,
            includeOversizedImages = oversized,
            skipPreviouslyCompressed = skipCompressed,
            writeExifMarker = exif,
            minSizeBytes = minSize,
            historyCount = historyCount,
            historyDatabaseSize = historySize,
        )
    }.safeStateIn(
        initialValue = State(),
        onError = { State() },
    )

    fun setIncludeJpeg(value: Boolean) = launch {
        settings.includeJpeg.value(value)
    }

    fun setIncludeWebp(value: Boolean) = launch {
        settings.includeWebp.value(value)
    }

    fun setIncludeHeic(value: Boolean) = launch {
        settings.includeHeic.value(value)
    }

    fun setIncludeVideo(value: Boolean) = launch {
        settings.includeVideo.value(value)
    }

    fun setIncludeLossyAuxImages(value: Boolean) = launch {
        settings.includeLossyAuxImages.value(value)
    }

    fun setIncludeMotionPhotos(value: Boolean) = launch {
        settings.includeMotionPhotos.value(value)
    }

    fun setIncludeOversizedImages(value: Boolean) = launch {
        settings.includeOversizedImages.value(value)
    }

    fun setSkipPreviouslyCompressed(value: Boolean) = launch {
        settings.skipPreviouslyCompressed.value(value)
    }

    fun setWriteExifMarker(value: Boolean) = launch {
        settings.writeExifMarker.value(value)
    }

    fun setMinSizeBytes(value: Long) = launch {
        settings.minSizeBytes.value(value)
    }

    fun clearHistory() = launch {
        log(TAG) { "clearHistory()" }
        historyDatabase.clear()
    }

    fun onHelpClick() {
        webpageTool.open(HELP_URL)
    }

    data class State(
        val includeJpeg: Boolean = true,
        val includeWebp: Boolean = true,
        val includeHeic: Boolean = false,
        val isHeicSupported: Boolean = false,
        val includeVideo: Boolean = false,
        val includeLossyAuxImages: Boolean = false,
        val includeMotionPhotos: Boolean = false,
        val includeOversizedImages: Boolean = false,
        val skipPreviouslyCompressed: Boolean = true,
        val writeExifMarker: Boolean = false,
        val minSizeBytes: Long = SqueezerSettings.MIN_FILE_SIZE,
        val historyCount: Int = 0,
        val historyDatabaseSize: Long = 0L,
    )

    companion object {
        private val TAG = logTag("Settings", "Squeezer", "ViewModel")
        private const val HELP_URL = "https://sdmse.darken.eu/mediasqueeze#settings"
    }
}
