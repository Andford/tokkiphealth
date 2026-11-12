package mx.loperstack.tokkiphealth.systemcleaner.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Delete
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.datastore.DataStoreValue
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.FileType
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.files.local.LocalPathLookup
import mx.loperstack.tokkiphealth.common.forensics.FileForensics
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.sharedresource.SharedResource
import mx.loperstack.tokkiphealth.exclusion.core.ExclusionManager
import mx.loperstack.tokkiphealth.exclusion.core.types.Exclusion
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.BaseSystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.FilterIdentifier
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.FilterSource
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Provider

/**
 * Shared fixtures for SystemCleaner tests. Module-local — consolidated because more test files
 * need them than CorpseFinder's inline-helper approach scales to.
 */

internal fun fakeMatch(
    name: String = "file.dat",
    parent: Array<String> = arrayOf("storage", "emulated", "0", "Android", "data"),
    size: Long = 1024L,
    fileType: FileType = FileType.FILE,
): SystemCleanerFilter.Match.Deletion = SystemCleanerFilter.Match.Deletion(
    lookup = LocalPathLookup(
        lookedUp = LocalPath.build(*parent, name),
        fileType = fileType,
        size = size,
        modifiedAt = Instant.parse("2026-04-01T12:00:00Z"),
        target = null,
    ),
)

internal fun fakeFilterContent(
    identifier: FilterIdentifier = "fake.filter.${FilterContentCounter.next()}",
    label: String = "Fake filter",
    description: String = "Fake description",
    items: Collection<SystemCleanerFilter.Match> = listOf(fakeMatch()),
): FilterContent = FilterContent(
    identifier = identifier,
    icon = Icons.TwoTone.Delete,
    label = label.toCaString(),
    description = description.toCaString(),
    items = items,
)

private object FilterContentCounter {
    private var n = 0
    fun next(): Int = ++n
}

/**
 * Counting fake filter — analog of `CountingFactory` in `CorpseFinderTest`. The processed
 * collection is what `process(matches)` returns; tests can supply failed/successful results.
 */
internal class FakeSystemCleanerFilter(
    override val identifier: FilterIdentifier,
    private val processResults: (Collection<SystemCleanerFilter.Match>) -> Collection<SystemCleanerFilter.Processed> = { matches ->
        matches.map { SystemCleanerFilter.Processed(match = it, error = null) }
    },
) : BaseSystemCleanerFilter() {
    var processInvocations: Int = 0
        private set
    var lastProcessInput: Collection<SystemCleanerFilter.Match>? = null
        private set

    override val icon = Icons.TwoTone.Delete

    override suspend fun getLabel() = "Fake $identifier".toCaString()
    override suspend fun getDescription() = "Fake description for $identifier".toCaString()
    override suspend fun targetAreas() = emptySet<DataArea.Type>()
    override suspend fun initialize() = Unit
    override suspend fun match(item: APathLookup<*>) = null
    override suspend fun process(matches: Collection<SystemCleanerFilter.Match>): Collection<SystemCleanerFilter.Processed> {
        processInvocations++
        lastProcessInput = matches
        return processResults(matches)
    }
}

internal class FakeThrowingFilter(
    override val identifier: FilterIdentifier,
    private val toThrow: Throwable,
) : BaseSystemCleanerFilter() {
    var processInvocations: Int = 0
        private set

    override val icon = Icons.TwoTone.Delete
    override suspend fun getLabel() = "Throwing $identifier".toCaString()
    override suspend fun getDescription() = "Throws on process".toCaString()
    override suspend fun targetAreas() = emptySet<DataArea.Type>()
    override suspend fun initialize() = Unit
    override suspend fun match(item: APathLookup<*>) = null
    override suspend fun process(matches: Collection<SystemCleanerFilter.Match>): Collection<SystemCleanerFilter.Processed> {
        processInvocations++
        throw toThrow
    }
}

internal fun keepAliveSharedResource(tag: String, scope: CoroutineScope) =
    SharedResource.createKeepAlive(tag, scope)

/**
 * A writable mocked `DataStoreValue`. The shared `mockDataStoreValue` only stubs `.flow`;
 * `.value(new)` writes call `DataStoreValue.update(...)` which by default fails in MockK.
 * This helper also stubs `update` so writes succeed and can be verified by tests.
 * Mirrors the inline helper at `CorpseFinderSettingsViewModelTest.kt:31-35`.
 */
internal fun <T> rwDataStoreValue(
    initial: T,
    flow: Flow<T> = flowOf(initial),
): DataStoreValue<T> = mockk<DataStoreValue<T>>().apply {
    every { this@apply.flow } returns flow
    coEvery { update(any()) } returns DataStoreValue.Updated(old = initial, new = initial)
}

internal suspend fun SystemCleaner.dataFromState(): SystemCleaner.Data? =
    state.map { it.data }.first()

/**
 * Shared engine-test harness. Wires the heavy `SharedResource` dependencies (fileForensics,
 * gatewaySwitch, pkgOps) to real keep-alive resources backed by `keepAliveScope`; mocks the
 * rest (`crawler`, `FilterSource`, `ExclusionManager`, `RootManager`).
 *
 * Used by SystemCleanerScanTest / SystemCleanerProcessingTest / SystemCleanerExclusionTest.
 */
internal class SystemCleanerHarness(
    private val keepAliveScope: CoroutineScope,
) {
    val crawler: SystemCrawler = mockk()
    val exclusionManager: ExclusionManager = mockk()
    val filterSource: FilterSource = mockk()
    val rootManager: RootManager = mockk()
    val fileForensics: FileForensics = mockk()
    val gatewaySwitch: GatewaySwitch = mockk()
    val pkgOps: PkgOps = mockk()

    fun build(
        useRoot: Boolean = false,
        crawlerResults: Collection<FilterContent> = emptyList(),
        filtersForScan: List<SystemCleanerFilter> = emptyList(),
        filtersForProcess: List<SystemCleanerFilter> = emptyList(),
        captureOnlyEnabled: CapturingSlot<Boolean>? = null,
        savedExclusions: Collection<Exclusion> = emptyList(),
        captureSavedExclusions: CapturingSlot<Set<Exclusion>>? = null,
    ): SystemCleaner {
        every { fileForensics.sharedResource } returns SharedResource.createKeepAlive("ff", keepAliveScope)
        every { gatewaySwitch.sharedResource } returns SharedResource.createKeepAlive("gw", keepAliveScope)
        every { pkgOps.sharedResource } returns SharedResource.createKeepAlive("po", keepAliveScope)
        every { rootManager.useRoot } returns flowOf(useRoot)
        every { exclusionManager.exclusions } returns flowOf(emptyList())
        if (captureSavedExclusions != null) {
            coEvery { exclusionManager.save(capture(captureSavedExclusions)) } returns savedExclusions
        } else {
            coEvery { exclusionManager.save(any()) } returns savedExclusions
        }
        coEvery { exclusionManager.remove(any()) } returns Unit
        if (captureOnlyEnabled != null) {
            coEvery { filterSource.create(capture(captureOnlyEnabled)) } answers {
                if (captureOnlyEnabled.captured) filtersForScan.toSet() else filtersForProcess.toSet()
            }
        } else {
            coEvery { filterSource.create(true) } returns filtersForScan.toSet()
            coEvery { filterSource.create(false) } returns filtersForProcess.toSet()
        }
        every { crawler.progress } returns MutableStateFlow<Progress.Data?>(null)
        coEvery { crawler.crawl(any()) } returns crawlerResults

        return SystemCleaner(
            appScope = keepAliveScope,
            fileForensics = fileForensics,
            gatewaySwitch = gatewaySwitch,
            crawler = crawler,
            exclusionManager = exclusionManager,
            filterSourceProvider = Provider { filterSource },
            pkgOps = pkgOps,
            rootManager = rootManager,
        )
    }
}
