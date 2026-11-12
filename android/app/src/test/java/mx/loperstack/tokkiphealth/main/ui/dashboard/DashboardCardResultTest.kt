package mx.loperstack.tokkiphealth.main.ui.dashboard

import mx.loperstack.tokkiphealth.appcleaner.core.AppCleaner
import mx.loperstack.tokkiphealth.appcleaner.core.AppJunk
import mx.loperstack.tokkiphealth.appcleaner.core.tasks.AppCleanerScanTask
import mx.loperstack.tokkiphealth.corpsefinder.core.Corpse
import mx.loperstack.tokkiphealth.corpsefinder.core.CorpseFinder
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderScanTask
import mx.loperstack.tokkiphealth.deduplicator.core.Deduplicator
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import mx.loperstack.tokkiphealth.deduplicator.core.tasks.DeduplicatorScanTask
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.systemcleaner.core.FilterContent
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleaner
import mx.loperstack.tokkiphealth.systemcleaner.core.tasks.SystemCleanerScanTask
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import testhelpers.BaseTest

/**
 * Unit tests for the dashboard tool card result logic:
 *  - each tool's [*.Data.toScanSuccess] mapping (the single source of truth shared with performScan)
 *  - [resolveScanCardResult]'s state-resolution branches
 */
class DashboardCardResultTest : BaseTest() {

    // --- Per-tool Data.toScanSuccess() mappings ---------------------------------------------------

    @Test
    fun `appcleaner data maps to scan success by item count and size`() {
        val data = AppCleaner.Data(
            junks = listOf(
                mockk<AppJunk> { every { size } returns 400L; every { itemCount } returns 3 },
                mockk<AppJunk> { every { size } returns 100L; every { itemCount } returns 2 },
            ),
        )
        data.toScanSuccess() shouldBe AppCleanerScanTask.Success(itemCount = 5, recoverableSpace = 500L)
    }

    @Test
    fun `corpsefinder data maps to scan success by corpse count and size`() {
        val data = CorpseFinder.Data(
            corpses = listOf(
                mockk<Corpse> { every { size } returns 400L },
                mockk<Corpse> { every { size } returns 100L },
            ),
        )
        data.toScanSuccess() shouldBe CorpseFinderScanTask.Success(itemCount = 2, recoverableSpace = 500L)
    }

    @Test
    fun `systemcleaner data maps to scan success by contained item count and size`() {
        val data = SystemCleaner.Data(
            filterContents = listOf(
                mockk<FilterContent> { every { items } returns listOf(mockk(), mockk(), mockk()); every { size } returns 400L },
                mockk<FilterContent> { every { items } returns listOf(mockk(), mockk()); every { size } returns 100L },
            ),
        )
        data.toScanSuccess() shouldBe SystemCleanerScanTask.Success(itemCount = 5, recoverableSpace = 500L)
    }

    @Test
    fun `deduplicator data maps item count to cluster count not redundant count`() {
        // Trap guard: itemCount must be the cluster count (2), NOT the summed redundantCount (11).
        val data = Deduplicator.Data(
            clusters = setOf(
                mockk<Duplicate.Cluster> { every { redundantSize } returns 400L; every { redundantCount } returns 7 },
                mockk<Duplicate.Cluster> { every { redundantSize } returns 100L; every { redundantCount } returns 4 },
            ),
        )
        data.toScanSuccess() shouldBe DeduplicatorScanTask.Success(itemCount = 2, recoverableSpace = 500L)
    }

    // --- resolveScanCardResult branches -----------------------------------------------------------

    private val frozen = mockk<SDMTool.Task.Result>()
    private val rebuilt = mockk<SDMTool.Task.Result>()

    private fun resolve(
        isInitializing: Boolean = false,
        isWorking: Boolean = false,
        data: String? = "data",
        hasData: Boolean = true,
        lastResult: SDMTool.Task.Result? = frozen,
        lastResultIsScan: Boolean = true,
    ): SDMTool.Task.Result? = resolveScanCardResult(
        isInitializing = isInitializing,
        isWorking = isWorking,
        data = data,
        hasData = hasData,
        lastResult = lastResult,
        lastResultIsScan = lastResultIsScan,
    ) { rebuilt }

    @Test
    fun `while initializing keeps the frozen result without reconstructing`() {
        resolveScanCardResult(
            isInitializing = true,
            isWorking = false,
            data = "data",
            hasData = true,
            lastResult = frozen,
            lastResultIsScan = true,
        ) { error("must not reconstruct while initializing") } shouldBe frozen
    }

    @Test
    fun `while working keeps the frozen result without reconstructing`() {
        resolveScanCardResult(
            isInitializing = false,
            isWorking = true,
            data = "data",
            hasData = true,
            lastResult = frozen,
            lastResultIsScan = true,
        ) { error("must not reconstruct while working") } shouldBe frozen
    }

    @Test
    fun `live data present rebuilds the summary`() {
        resolve(hasData = true) shouldBe rebuilt
    }

    @Test
    fun `empty live data superseding a scan rebuilds to a zero summary`() {
        // exclude-all / scan-found-nothing: data loaded but empty, last result was a scan.
        resolve(hasData = false, lastResultIsScan = true) shouldBe rebuilt
    }

    @Test
    fun `empty live data with a non-scan result keeps the freed outcome`() {
        // post-delete "freed X": empty data, last result is a processing result.
        resolve(hasData = false, lastResultIsScan = false) shouldBe frozen
    }

    @Test
    fun `surviving live data with a non-scan result still keeps the freed outcome`() {
        // Regression for "the cleanup freed 1 GB but the card said 5.8 MB can be freed": a delete that
        // leaves residue behind (a locked system app's cache can never be cleared, so this is the norm)
        // must not have that residue rebuilt into a fresh freeable summary over its own "freed X".
        resolve(hasData = true, lastResultIsScan = false) shouldBe frozen
    }

    @Test
    fun `invalidated null data drops a stale scan result`() {
        resolve(data = null, hasData = false, lastResultIsScan = true) shouldBe null
    }

    @Test
    fun `invalidated null data keeps a non-scan result`() {
        resolve(data = null, hasData = false, lastResultIsScan = false) shouldBe frozen
    }
}
