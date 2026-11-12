package mx.loperstack.tokkiphealth.squeezer.ui.setup

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.MimeTypeTool
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.APathGateway
import mx.loperstack.tokkiphealth.common.files.isFile
import mx.loperstack.tokkiphealth.common.files.local.LocalGateway
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.flow.SingleEventFlow
import mx.loperstack.tokkiphealth.common.flow.combine
import mx.loperstack.tokkiphealth.common.navigation.NavigationController
import mx.loperstack.tokkiphealth.common.picker.PickerRequest
import mx.loperstack.tokkiphealth.common.picker.PickerResultKey
import mx.loperstack.tokkiphealth.common.picker.PickerRoute
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.storage.PathMapper
import mx.loperstack.tokkiphealth.common.storage.StorageEnvironment
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskSubmitter
import mx.loperstack.tokkiphealth.squeezer.core.CompressibleImage
import mx.loperstack.tokkiphealth.squeezer.core.CompressionEstimator
import mx.loperstack.tokkiphealth.squeezer.core.Squeezer
import mx.loperstack.tokkiphealth.squeezer.core.SqueezerEligibility
import mx.loperstack.tokkiphealth.squeezer.core.SqueezerPathNormalizer
import mx.loperstack.tokkiphealth.squeezer.core.SqueezerSettings
import mx.loperstack.tokkiphealth.squeezer.core.hasData
import mx.loperstack.tokkiphealth.squeezer.core.tasks.SqueezerProcessTask
import mx.loperstack.tokkiphealth.squeezer.core.tasks.SqueezerScanTask
import mx.loperstack.tokkiphealth.squeezer.ui.SqueezerListRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Duration
import javax.inject.Inject

@HiltViewModel
class SqueezerSetupViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val settings: SqueezerSettings,
    private val squeezer: Squeezer,
    private val taskSubmitter: TaskSubmitter,
    private val compressionEstimator: CompressionEstimator,
    private val localGateway: LocalGateway,
    private val pathMapper: PathMapper,
    @Suppress("unused") private val storageEnvironment: StorageEnvironment,
    private val mimeTypeTool: MimeTypeTool,
    private val navCtrl: NavigationController,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    val events = SingleEventFlow<Event>()

    private val isLoadingExample = MutableStateFlow(false)

    init {
        navCtrl.consumeResults(PickerResultKey(PICKER_REQUEST_KEY))
            .onEach { result ->
                log(TAG, INFO) { "Picker returned ${result.selectedPaths.size} paths" }
                updatePaths(result.selectedPaths)
            }
            .launchIn(vmScope)
    }

    val state: StateFlow<State> = combine(
        settings.scanPaths.flow,
        settings.compressionQuality.flow,
        settings.minAge.flow,
        squeezer.state,
        isLoadingExample,
    ) { scanPaths, quality, minAge, squeezerState, loadingExample ->
        val jpegRatio = compressionEstimator.estimateOutputRatio(CompressibleImage.MIME_TYPE_JPEG, quality)
        val estimatedSavings = jpegRatio?.let { ((1.0 - it) * 100).toInt() }

        State(
            scanPaths = scanPaths.paths.sortedBy { it.path },
            quality = quality,
            minAge = minAge,
            estimatedSavingsPercent = estimatedSavings,
            progress = squeezerState.progress,
            // Process results only: `performProcess` prunes the data before `submit()` stores its
            // result, so there is a window where `lastResult` is still the preceding scan success.
            // Unfiltered, the card would flash "x items found" before "x items compressed" and
            // would keep showing scan summaries where a compression receipt belongs.
            processResult = squeezerState.lastResult as? SqueezerProcessTask.Result,
            isLoadingExample = loadingExample,
            canStartScan = scanPaths.paths.isNotEmpty(),
        )
    }.safeStateIn(
        // The setup entry stays in the back stack while the list screen runs a compression on top
        // of it. With the default `WhileSubscribed(5000)` this ViewModel would stop collecting
        // while it is covered and, on back navigation, resume from its stale pre-navigation value
        // (progress == null) — flashing the configuration before the progress overlay returns.
        started = SharingStarted.Eagerly,
        initialValue = State(),
        onError = { State() },
    )

    data class State(
        val scanPaths: List<APath> = emptyList(),
        val quality: Int = SqueezerSettings.DEFAULT_QUALITY,
        val minAge: Duration = SqueezerSettings.MIN_AGE_DEFAULT,
        val estimatedSavingsPercent: Int? = null,
        val progress: Progress.Data? = null,
        val processResult: SqueezerProcessTask.Result? = null,
        val isLoadingExample: Boolean = false,
        val canStartScan: Boolean = false,
    )

    fun updateQuality(quality: Int) = launch {
        log(TAG, INFO) { "updateQuality($quality)" }
        settings.compressionQuality.value(quality)
    }

    fun updateMinAge(age: Duration) = launch {
        log(TAG, INFO) { "updateMinAge($age)" }
        settings.minAge.value(age)
    }

    fun updatePaths(paths: Set<APath>) = launch {
        log(TAG, INFO) { "updatePaths(${paths.size} paths)" }

        // Squeezer can only process files reachable via java.io.File (Transformer +
        // BitmapFactory hard-require raw paths). SAF roots on primary storage can usually
        // be mapped back to a LocalPath via PathMapper; anything that can't be mapped is
        // surfaced to the UI so the user isn't left wondering why their scan returned
        // nothing.
        val normalized = SqueezerPathNormalizer.normalize(paths, pathMapper)

        if (normalized.dropped.isNotEmpty()) {
            log(TAG, WARN) { "Dropped ${normalized.dropped.size} non-local paths: ${normalized.dropped}" }
            events.tryEmit(Event.PathsDropped(normalized.dropped))
        }

        settings.scanPaths.value(SqueezerSettings.ScanPaths(paths = normalized.accepted))
    }

    fun openPathPicker() = launch {
        log(TAG) { "openPathPicker()" }
        val current = settings.scanPaths.value().paths
        navTo(
            PickerRoute(
                request = PickerRequest(
                    requestKey = PICKER_REQUEST_KEY,
                    mode = PickerRequest.PickMode.DIRS,
                    allowedAreas = setOf(
                        DataArea.Type.PORTABLE,
                        DataArea.Type.SDCARD,
                        DataArea.Type.PUBLIC_DATA,
                        DataArea.Type.PUBLIC_MEDIA,
                    ),
                    selectedPaths = current.toList(),
                ),
            ),
        )
    }

    fun startScan() = launch {
        log(TAG, INFO) { "startScan()" }

        val result = taskSubmitter.submit(SqueezerScanTask())
        log(TAG, INFO) { "Scan result: $result" }

        val data = squeezer.state.first().data
        if (data.hasData) {
            navTo(SqueezerListRoute)
        } else {
            events.tryEmit(
                Event.NoResultsFound(
                    skippedLossyAuxCount = data?.skippedLossyAuxCount ?: 0,
                    skippedMotionPhotoCount = data?.skippedMotionPhotoCount ?: 0,
                    skippedOversizedCount = data?.skippedOversizedCount ?: 0,
                )
            )
        }
    }

    fun showExample() = launch {
        log(TAG) { "showExample()" }
        isLoadingExample.value = true
        try {
            val quality = settings.compressionQuality.value()
            val sampleImage = findSampleImage()
            if (sampleImage != null) {
                events.tryEmit(Event.ShowExample(sampleImage, quality))
            } else {
                events.tryEmit(Event.NoExampleFound)
            }
        } finally {
            isLoadingExample.value = false
        }
    }

    private suspend fun findSampleImage(): CompressibleImage? {
        val searchPaths = settings.scanPaths.value().paths
        if (searchPaths.isEmpty()) return null

        for (searchPath in searchPaths) {
            val localPath = searchPath as? LocalPath ?: continue
            try {
                val lookup = localGateway.walk(
                    path = localPath,
                    options = APathGateway.WalkOptions(),
                    mode = LocalGateway.Mode.NORMAL,
                ).firstOrNull { lookup ->
                    if (!lookup.isFile) return@firstOrNull false
                    if (SqueezerEligibility.check(lookup.lookedUp) != SqueezerEligibility.Verdict.OK) {
                        return@firstOrNull false
                    }
                    if (lookup.size < settings.minSizeBytes.value()) return@firstOrNull false
                    val mimeType = mimeTypeTool.determineMimeType(lookup)
                    // The onboarding example compresses in-memory and shows a quick preview; HEIC
                    // can't demonstrate savings that way, so exclude HEIC samples even on devices
                    // that could otherwise encode them.
                    mimeType in CompressibleImage.SUPPORTED_MIME_TYPES &&
                        mimeType !in CompressibleImage.HEIC_MIME_TYPES
                }

                if (lookup != null) {
                    val mimeType = mimeTypeTool.determineMimeType(lookup)
                    val quality = settings.compressionQuality.value()
                    val estimatedCompressedSize = compressionEstimator.estimateCompressedSize(
                        lookup.size,
                        mimeType,
                        quality,
                    )
                    return CompressibleImage(
                        lookup = lookup,
                        mimeType = mimeType,
                        estimatedCompressedSize = estimatedCompressedSize,
                        priorCompression = null,
                        hasLossyAux = false,
                    )
                }
            } catch (e: Exception) {
                log(TAG) { "Failed to search path $searchPath: ${e.message}" }
            }
        }
        return null
    }

    fun onHelpClick() {
        webpageTool.open(HELP_URL)
    }

    sealed interface Event {
        data class ShowExample(val sampleImage: CompressibleImage, val quality: Int) : Event
        data object NoExampleFound : Event
        data class NoResultsFound(
            val skippedLossyAuxCount: Int = 0,
            val skippedMotionPhotoCount: Int = 0,
            val skippedOversizedCount: Int = 0,
        ) : Event
        data class PathsDropped(val droppedPaths: List<APath>) : Event
    }

    companion object {
        internal const val PICKER_REQUEST_KEY = "squeezer.setup.paths"
        private val TAG = logTag("Squeezer", "Setup", "ViewModel")
        private const val HELP_URL = "https://sdmse.darken.eu/mediasqueeze"
    }
}
