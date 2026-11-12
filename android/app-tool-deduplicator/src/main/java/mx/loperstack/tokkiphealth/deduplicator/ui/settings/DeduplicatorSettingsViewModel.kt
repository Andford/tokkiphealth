package mx.loperstack.tokkiphealth.deduplicator.ui.settings

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.flow.combine
import mx.loperstack.tokkiphealth.common.navigation.NavigationController
import mx.loperstack.tokkiphealth.common.picker.PickerRequest
import mx.loperstack.tokkiphealth.common.picker.PickerResultKey
import mx.loperstack.tokkiphealth.common.picker.PickerRoute
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.deduplicator.core.DeduplicatorSettings
import mx.loperstack.tokkiphealth.deduplicator.ui.ArbiterConfigRoute
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject


@HiltViewModel
class DeduplicatorSettingsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val settings: DeduplicatorSettings,
    navCtrl: NavigationController,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    private data class TopFlags(
        val scanPaths: List<APath>,
        val allowDeleteAll: Boolean,
        val minSizeBytes: Long,
        val skipUncommon: Boolean,
    )

    private data class SleuthFlags(
        val checksum: Boolean,
        val phash: Boolean,
        val media: Boolean,
    )

    private val topFlagsFlow = kotlinx.coroutines.flow.combine(
        settings.scanPaths.flow,
        settings.allowDeleteAll.flow,
        settings.minSizeBytes.flow,
        settings.skipUncommon.flow,
    ) { scanPaths, allowDeleteAll, minSize, skipUncommon ->
        TopFlags(
            scanPaths = scanPaths.paths.sortedBy { it.path },
            allowDeleteAll = allowDeleteAll,
            minSizeBytes = minSize,
            skipUncommon = skipUncommon,
        )
    }

    private val sleuthFlagsFlow = combine(
        settings.isSleuthChecksumEnabled.flow,
        settings.isSleuthPHashEnabled.flow,
        settings.isSleuthMediaEnabled.flow,
    ) { checksum, phash, media ->
        SleuthFlags(checksum, phash, media)
    }

    val state: StateFlow<State> = kotlinx.coroutines.flow.combine(
        topFlagsFlow,
        sleuthFlagsFlow,
    ) { top, sleuth ->
        State(
            scanPaths = top.scanPaths,
            allowDeleteAll = top.allowDeleteAll,
            minSizeBytes = top.minSizeBytes,
            skipUncommon = top.skipUncommon,
            isSleuthChecksumEnabled = sleuth.checksum,
            isSleuthPHashEnabled = sleuth.phash,
            isSleuthMediaEnabled = sleuth.media,
        )
    }.safeStateIn(
        initialValue = State(),
        onError = { State() },
    )

    init {
        navCtrl.consumeResults(PickerResultKey(SCAN_PATHS_REQUEST_KEY))
            .onEach { result ->
                log(TAG) { "Received picker result with ${result.selectedPaths.size} paths" }
                settings.scanPaths.value(DeduplicatorSettings.ScanPaths(paths = result.selectedPaths))
            }
            .launchIn(vmScope)
    }

    fun resetScanPaths() = launch {
        log(TAG) { "resetScanPaths()" }
        settings.scanPaths.value(DeduplicatorSettings.ScanPaths())
    }

    fun onSearchLocationsClick() {
        val current = state.value.scanPaths
        navTo(
            PickerRoute(
                request = PickerRequest(
                    requestKey = SCAN_PATHS_REQUEST_KEY,
                    mode = PickerRequest.PickMode.DIRS,
                    allowedAreas = setOf(
                        DataArea.Type.PORTABLE,
                        DataArea.Type.SDCARD,
                        DataArea.Type.PUBLIC_DATA,
                        DataArea.Type.PUBLIC_MEDIA,
                    ),
                    selectedPaths = current,
                ),
            ),
        )
    }

    fun onArbiterConfigClick() {
        navTo(ArbiterConfigRoute)
    }

    fun setAllowDeleteAll(value: Boolean) = launch { settings.allowDeleteAll.value(value) }
    fun setSkipUncommon(value: Boolean) = launch { settings.skipUncommon.value(value) }
    fun setMinSizeBytes(value: Long) = launch { settings.minSizeBytes.value(value) }
    fun resetMinSizeBytes() = launch { settings.minSizeBytes.value(DeduplicatorSettings.MIN_FILE_SIZE) }
    fun setSleuthChecksumEnabled(value: Boolean) = launch { settings.isSleuthChecksumEnabled.value(value) }
    fun setSleuthPHashEnabled(value: Boolean) = launch { settings.isSleuthPHashEnabled.value(value) }
    fun setSleuthMediaEnabled(value: Boolean) = launch { settings.isSleuthMediaEnabled.value(value) }

    fun onHelpClick() {
        webpageTool.open(HELP_URL)
    }

    data class State(
        val scanPaths: List<APath> = emptyList(),
        val allowDeleteAll: Boolean = false,
        val minSizeBytes: Long = DeduplicatorSettings.MIN_FILE_SIZE,
        val skipUncommon: Boolean = true,
        val isSleuthChecksumEnabled: Boolean = true,
        val isSleuthPHashEnabled: Boolean = false,
        val isSleuthMediaEnabled: Boolean = false,
    )

    companion object {
        private val TAG = logTag("Settings", "Deduplicator", "ViewModel")
        private const val HELP_URL = "https://sdmse.darken.eu/deduplicator#settings"
        private const val SCAN_PATHS_REQUEST_KEY = "scan.location.paths"
    }
}
