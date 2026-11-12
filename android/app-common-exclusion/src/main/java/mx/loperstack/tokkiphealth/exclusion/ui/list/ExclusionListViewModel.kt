package mx.loperstack.tokkiphealth.exclusion.ui.list

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.MimeTypes
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.flow.SingleEventFlow
import mx.loperstack.tokkiphealth.common.navigation.routes.UpgradeRoute
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.pkgs
import mx.loperstack.tokkiphealth.common.readAsText
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.common.upgrade.UpgradeRepo
import mx.loperstack.tokkiphealth.common.upgrade.isProForUi
import mx.loperstack.tokkiphealth.exclusion.core.DefaultExclusions
import mx.loperstack.tokkiphealth.exclusion.core.ExclusionImporter
import mx.loperstack.tokkiphealth.exclusion.core.ExclusionManager
import mx.loperstack.tokkiphealth.exclusion.core.LegacyImporter
import mx.loperstack.tokkiphealth.exclusion.core.current
import mx.loperstack.tokkiphealth.exclusion.core.types.DefaultExclusion
import mx.loperstack.tokkiphealth.exclusion.core.types.Exclusion
import mx.loperstack.tokkiphealth.exclusion.core.types.ExclusionId
import mx.loperstack.tokkiphealth.exclusion.core.types.PathExclusion
import mx.loperstack.tokkiphealth.exclusion.core.types.PkgExclusion
import mx.loperstack.tokkiphealth.exclusion.core.types.SegmentExclusion
import mx.loperstack.tokkiphealth.exclusion.core.types.UserExclusion
import mx.loperstack.tokkiphealth.exclusion.ui.PathExclusionEditorRoute
import mx.loperstack.tokkiphealth.exclusion.ui.PkgExclusionEditorRoute
import mx.loperstack.tokkiphealth.exclusion.ui.SegmentExclusionEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ExclusionListViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val exclusionManager: ExclusionManager,
    private val pkgRepo: PkgRepo,
    private val gatewaySwitch: GatewaySwitch,
    private val defaultExclusions: DefaultExclusions,
    private val webpageTool: WebpageTool,
    private val upgradeRepo: UpgradeRepo,
    private val legacyImporter: LegacyImporter,
    private val exclusionImporter: ExclusionImporter,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    val events = SingleEventFlow<Event>()

    private val showDefaults = MutableStateFlow(handle["showDefaults"] ?: false)

    private val lookups = exclusionManager.exclusions
        .map { holders -> holders.map { it.exclusion } }
        .map { excls -> excls.filterIsInstance<PathExclusion>().map { it.path } }
        .map { paths ->
            paths
                .mapNotNull { path ->
                    try {
                        gatewaySwitch.lookup(path)
                    } catch (e: Exception) {
                        log(TAG, VERBOSE) { "Path exclusion lookup failed: $e" }
                        null
                    }
                }
                .groupBy { it.lookedUp }
                .mapValues { it.value.first() }
        }

    val state: StateFlow<State> = combine(
        exclusionManager.exclusions,
        pkgRepo.pkgs().onStart { emit(emptySet()) },
        lookups.onStart { emit(emptyMap()) },
        showDefaults,
    ) { holders, pkgs, lookups, showDefaults ->
        handle["showDefaults"] = showDefaults

        val rows = mutableListOf<Row>()
        holders.forEach { holder ->
            val isDefault = when (holder) {
                is DefaultExclusion -> true
                is UserExclusion -> false
            }
            if (isDefault && !showDefaults) return@forEach

            val reasonUrl = (holder as? DefaultExclusion)?.reason

            when (val exclusion = holder.exclusion) {
                is PkgExclusion -> rows.add(
                    Row.Pkg(
                        exclusion = exclusion,
                        pkg = pkgs.firstOrNull { it.id == exclusion.pkgId },
                        isDefault = isDefault,
                        reasonUrl = reasonUrl,
                        label = exclusion.label.get(context),
                    ),
                )

                is PathExclusion -> rows.add(
                    Row.Path(
                        exclusion = exclusion,
                        lookup = lookups[exclusion.path],
                        isDefault = isDefault,
                        reasonUrl = reasonUrl,
                        label = exclusion.label.get(context),
                    ),
                )

                is SegmentExclusion -> rows.add(
                    Row.Segment(
                        exclusion = exclusion,
                        isDefault = isDefault,
                        reasonUrl = reasonUrl,
                        label = exclusion.label.get(context),
                    ),
                )

                else -> Unit
            }
        }

        val sorted = rows.sortedWith(
            compareBy<Row> { it.isDefault }
                .thenBy {
                    when (it) {
                        is Row.Pkg -> 0
                        is Row.Path -> 1
                        is Row.Segment -> 2
                    }
                }
                .thenBy { it.label },
        )

        // A built-in default is "modified" when it's no longer an effective DefaultExclusion holder,
        // i.e. it was removed OR shadowed by a user exclusion with the same ID. Both are already
        // filtered out of exclusionManager.exclusions, so a missing default ID means modified.
        val defaultsModified = defaultExclusions.defaultIds.any { id ->
            holders.none { it is DefaultExclusion && it.id == id }
        }

        State(rows = sorted, showDefaults = showDefaults, defaultsModified = defaultsModified)
    }
        .safeStateIn(
            initialValue = State(),
            onError = { State(rows = emptyList()) },
        )

    fun onRowClick(row: Row) {
        log(TAG) { "onRowClick($row)" }
        if (row.isDefault) {
            row.reasonUrl?.let { webpageTool.open(it) }
            return
        }
        when (row) {
            is Row.Pkg -> navTo(PkgExclusionEditorRoute(exclusionId = row.exclusion.id))
            is Row.Path -> navTo(PathExclusionEditorRoute(exclusionId = row.exclusion.id))
            is Row.Segment -> navTo(SegmentExclusionEditorRoute(exclusionId = row.exclusion.id))
        }
    }

    fun restore(items: Set<Exclusion>) = launch {
        log(TAG) { "restore(${items.size})" }
        exclusionManager.save(items)
    }

    fun removeByIds(ids: Set<ExclusionId>) = launch {
        log(TAG) { "removeByIds(${ids.size})" }
        val exclusions = exclusionManager.current().filter { ids.contains(it.id) }.toSet()
        exclusionManager.remove(exclusions.map { it.id }.toSet())
        events.emit(Event.UndoRemove(exclusions))
    }

    fun resetDefaultExclusions() = launch {
        log(TAG) { "resetDefaultExclusions()" }
        exclusionManager.restoreDefaults()
    }

    fun openHelp() = launch {
        webpageTool.open("https://sdmse.darken.eu/exclusions")
    }

    fun requestImport() = launch {
        log(TAG) { "requestImport()" }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = MimeTypes.Json.value
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        events.emit(Event.ImportEvent(intent))
    }

    fun importExclusions(uris: Collection<Uri>) = launch {
        log(TAG) { "importExclusions($uris)" }
        val exclusion = uris
            .map {
                val result = it.readAsText(context) ?: throw IllegalArgumentException("Failed to read $it")
                log(TAG) { "Read $it ->  $result" }
                result
            }
            .mapNotNull { raw ->
                try {
                    exclusionImporter.import(raw).also {
                        log(TAG, INFO) { "Imported ${it.size}: $it" }
                    }
                } catch (e: Exception) {
                    log(TAG, WARN) { "This is not valid exclusion data, maybe legacy data?" }
                    try {
                        legacyImporter.tryConvert(raw).also {
                            log(TAG, INFO) { "Imported (legacy) ${it.size}: $it" }
                        }
                    } catch (e: Exception) {
                        log(TAG, WARN) { "Legacy import failed for $raw:\n${e.asLog()}" }
                        null
                    }
                }
            }
            .flatten()
            .toSet()

        try {
            exclusionManager.save(exclusion)
        } catch (e: Exception) {
            errorEvents.emit(e)
        }

        events.emit(Event.ImportSuccess(exclusion))
    }

    private var stagedExportIds: Set<ExclusionId>?
        set(value) {
            handle["stagedExportIds"] = value?.toTypedArray()
        }
        get() = handle.get<Array<ExclusionId>>("stagedExportIds")?.toSet()

    fun exportExclusions(ids: Set<ExclusionId>) = launch {
        log(TAG) { "exportExclusions($ids)" }

        if (!upgradeRepo.isProForUi()) {
            log(TAG) { "Pro upgrade required" }
            navTo(UpgradeRoute())
            return@launch
        }

        stagedExportIds = ids
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        events.emit(Event.ExportEvent(intent))
    }

    fun onExportAborted(error: Throwable? = null) = launch {
        log(TAG) { "onExportAborted(error=$error)" }
        stagedExportIds = null
        error?.let { errorEvents.emit(it) }
    }

    fun performExport(directoryUri: Uri?) = launch {
        if (directoryUri == null) {
            log(TAG, WARN) { "Export failed, no path picked" }
            return@launch
        }

        val exportIds = stagedExportIds ?: throw IllegalStateException("No staged export data available")

        val exportData = exclusionManager.current().filter { exportIds.contains(it.id) }.toSet()

        val saveDir = DocumentFile.fromTreeUri(context, directoryUri)
            ?: throw IOException("Failed to access $directoryUri")

        val filename = "Tokkip Health Exclusions ${System.currentTimeMillis()}"

        val targetFile = saveDir.createFile(MimeTypes.Json.value, filename)
            ?: throw IOException("Failed to create $filename in $saveDir")

        val rawContainer = exclusionImporter.export(exportData)
        context.contentResolver.openOutputStream(targetFile.uri)?.use { out ->
            out.write(rawContainer.toByteArray())
        }

        log(TAG, VERBOSE) { "Wrote $rawContainer to ${targetFile.uri}" }

        events.emit(Event.ExportSuccess(exportData))
    }

    fun showDefaultExclusions(show: Boolean) {
        log(TAG) { "showDefaultExclusions($show)" }
        showDefaults.value = show
    }

    fun openAppControl() {
        navTo(mx.loperstack.tokkiphealth.common.navigation.routes.AppControlListRoute)
    }

    fun openStoragePicker() {
        navTo(mx.loperstack.tokkiphealth.common.navigation.routes.DeviceStorageRoute)
    }

    fun openSegmentEditor() {
        navTo(
            SegmentExclusionEditorRoute(
                exclusionId = null,
                initial = mx.loperstack.tokkiphealth.exclusion.ui.editor.segment.SegmentExclusionEditorOptions(),
            ),
        )
    }

    data class State(
        val rows: List<Row>? = null,
        val showDefaults: Boolean = false,
        val defaultsModified: Boolean = false,
    )

    sealed interface Row {
        val exclusion: Exclusion
        val isDefault: Boolean
        val reasonUrl: String?
        val label: String
        val stableId: String get() = exclusion.id

        data class Pkg(
            override val exclusion: PkgExclusion,
            val pkg: mx.loperstack.tokkiphealth.common.pkgs.Pkg?,
            override val isDefault: Boolean,
            override val reasonUrl: String?,
            override val label: String,
        ) : Row

        data class Path(
            override val exclusion: PathExclusion,
            val lookup: APathLookup<*>?,
            override val isDefault: Boolean,
            override val reasonUrl: String?,
            override val label: String,
        ) : Row

        data class Segment(
            override val exclusion: SegmentExclusion,
            override val isDefault: Boolean,
            override val reasonUrl: String?,
            override val label: String,
        ) : Row
    }

    sealed interface Event {
        data class UndoRemove(val exclusions: Set<Exclusion>) : Event
        data class ImportEvent(val intent: Intent) : Event
        data class ExportEvent(val intent: Intent) : Event
        data class ImportSuccess(val exclusions: Set<Exclusion>) : Event
        data class ExportSuccess(val exclusions: Set<Exclusion>) : Event
    }

    companion object {
        private val TAG = logTag("Exclusions", "List", "ViewModel")
    }
}
