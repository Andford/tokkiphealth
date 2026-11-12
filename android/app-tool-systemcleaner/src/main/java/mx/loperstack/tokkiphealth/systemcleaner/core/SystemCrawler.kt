package mx.loperstack.tokkiphealth.systemcleaner.core

import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.DataAreaManager
import mx.loperstack.tokkiphealth.common.areas.currentAreas
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.APathGateway
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.ReadException
import mx.loperstack.tokkiphealth.common.files.Segments
import mx.loperstack.tokkiphealth.common.files.isDirectory
import mx.loperstack.tokkiphealth.common.files.lookup
import mx.loperstack.tokkiphealth.common.files.segs
import mx.loperstack.tokkiphealth.common.files.startsWith
import mx.loperstack.tokkiphealth.common.files.walk
import mx.loperstack.tokkiphealth.common.flow.throttleLatest
import mx.loperstack.tokkiphealth.common.forensics.FileForensics
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.progress.updateProgressCount
import mx.loperstack.tokkiphealth.common.progress.updateProgressPrimary
import mx.loperstack.tokkiphealth.common.progress.updateProgressSecondary
import mx.loperstack.tokkiphealth.common.sharedresource.useRes
import mx.loperstack.tokkiphealth.exclusion.core.ExclusionManager
import mx.loperstack.tokkiphealth.exclusion.core.pathExclusions
import mx.loperstack.tokkiphealth.exclusion.core.types.PathExclusionIndex
import mx.loperstack.tokkiphealth.exclusion.core.types.matches
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.FilterIdentifier
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilterException
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.excludeNestedLookups
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import java.io.IOException
import javax.inject.Inject

class SystemCrawler @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val areaManager: DataAreaManager,
    private val gatewaySwitch: GatewaySwitch,
    private val exclusionManager: ExclusionManager,
    private val forensics: FileForensics,
) : Progress.Host, Progress.Client {

    private val progressPub = MutableStateFlow<Progress.Data?>(Progress.Data())
    override val progress: Flow<Progress.Data?> = progressPub.throttleLatest(250)

    override fun updateProgress(update: (Progress.Data?) -> Progress.Data?) {
        progressPub.value = update(progressPub.value)
    }

    private val matchesInternal = MutableSharedFlow<MatchEvent>(extraBufferCapacity = 1024)
    val matchEvents: Flow<MatchEvent> = matchesInternal

    suspend fun crawl(filters: Set<SystemCleanerFilter>): Collection<FilterContent> {
        log(TAG) { "crawl()" }
        updateProgressPrimary(mx.loperstack.tokkiphealth.common.R.string.general_progress_searching)
        updateProgressSecondary(mx.loperstack.tokkiphealth.common.R.string.general_progress_generating_searchpaths)
        updateProgressCount(Progress.Count.Indeterminate())

        val exclusions = exclusionManager.pathExclusions(SDMTool.Type.SYSTEMCLEANER)
        log(TAG, INFO) { "Loaded exclusions: $exclusions" }
        val exclusionIndex = PathExclusionIndex(exclusions)

        val currentAreas = areaManager.currentAreas()
        val targetAreas = filters
            .map { it.targetAreas() }.flatten().toSet()
            .map { type -> currentAreas.filter { it.type == type } }.flatten().toSet()
        log(TAG) { "Target areas wanted by filters: $targetAreas" }

        val globalSkips = mutableSetOf<Segments>()
        if (targetAreas.all { it.path.segments != segs("", "data", "data") }) {
            globalSkips.add(segs("", "data", "data"))
            globalSkips.add(segs("", "data", "user", "0"))
        }
        globalSkips.add(segs("", "data", "media", "0"))
        log(TAG) { "Global skip segments: $globalSkips" }

        val sieveContents = mutableMapOf<FilterIdentifier, Set<SystemCleanerFilter.Match>>()

        listOf(gatewaySwitch, forensics).useRes {
            targetAreas
                .asFlow()
                .flowOn(dispatcherProvider.Default)
                .flatMapMerge(2) { targetArea ->
                    val filter: suspend (APathLookup<*>) -> Boolean = filter@{ toCheck: APathLookup<*> ->
                        if (globalSkips.any { toCheck.segments.startsWith(it) }) {
                            log(TAG, INFO) { "Excluded $toCheck by global skip" }
                            return@filter false
                        }

                        if (toCheck.isDirectory) {
                            val contentAreaCheck = toCheck.child("peek-content-area")
                            val area: DataArea? = forensics.identifyArea(contentAreaCheck)?.dataArea

                            if (area?.type != targetArea.type) {
                                if (Bugs.isTrace) log(TAG, VERBOSE) {
                                    "AreaCheck: Want ${targetArea.type}, Got ${area?.type} for ${toCheck.path}"
                                }
                                return@filter false
                            }
                        }

                        val isExcluded = exclusionIndex.matches(toCheck)
                        if (isExcluded) log(TAG, INFO) { "Excluded $toCheck" }
                        !isExcluded
                    }
                    log(TAG) { "Scanning $targetArea" }

                    val areaWalk: Flow<APathLookup<APath>> = try {
                        targetArea.path.lookup(gatewaySwitch)
                        targetArea.path.walk(
                            gatewaySwitch,
                            options = APathGateway.WalkOptions(onFilter = filter)
                        )
                    } catch (e: ReadException) {
                        currentCoroutineContext().ensureActive()
                        log(TAG, WARN) { "Skipping unreadable area $targetArea: ${e.asLog()}" }
                        emptyFlow()
                    }

                    areaWalk
                        .map { targetArea to it }
                        .onCompletion { log(TAG) { "Finished scanning $targetArea" } }
                }
                .buffer(2048 * 4)
                .flatMapMerge(4) { (area, item) ->
                    flow {
                        if (Bugs.isTrace) log(TAG, VERBOSE) { "Trying to match $item" }
                        updateProgressSecondary(item.path)
                        val matched: Pair<SystemCleanerFilter, SystemCleanerFilter.Match>? = filters
                            .filter { it.targetAreas().contains(area.type) }
                            .firstNotNullOfOrNull { filter ->
                                val match = try {
                                    filter.match(item)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: IOException) {
                                    log(TAG, WARN) { "IO error while matching ($filter): ${e.asLog()}" }
                                    null
                                } catch (e: Exception) {
                                    log(TAG, ERROR) { "Sieve failed ($filter): ${e.asLog()}" }
                                    throw SystemCleanerFilterException(filter, e)
                                }
                                if (match != null) filter to match else null
                            }
                        if (matched != null) emit(item to matched)
                    }
                }
                .collect { (item, matched) ->
                    log(TAG, INFO) { "Filter match: $matched <- $item" }
                    sieveContents[matched.first.identifier] =
                        (sieveContents[matched.first.identifier] ?: emptySet()).plus(matched.second)
                    matchesInternal.emit(MatchEvent(matched.first, item))
                }
        }

        val firstPass = sieveContents.map { entry ->
            log(TAG, INFO) { "${entry.key} has ${entry.value.size} matches (first pass)." }
            val filter = filters.single { it.identifier == entry.key }
            FilterContent(
                identifier = entry.key,
                icon = filter.icon,
                label = filter.getLabel(),
                description = filter.getDescription(),
                items = entry.value,
            )
        }

        // With nested exclusions applied

        return firstPass
            .map { it.copy(items = exclusions.excludeNestedLookups(it.items)) }
            .onEach { log(TAG, INFO) { "${it.identifier} has ${it.items.size} matches (second pass)." } }
            .filter { it.items.isNotEmpty() }
    }

    data class MatchEvent(
        val filter: SystemCleanerFilter,
        val match: APathLookup<*>,
    )

    companion object {
        private val TAG = logTag("SystemCleaner", "Crawler")
    }
}