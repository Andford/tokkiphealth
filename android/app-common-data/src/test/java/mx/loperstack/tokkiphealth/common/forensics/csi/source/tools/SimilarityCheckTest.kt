package mx.loperstack.tokkiphealth.common.forensics.csi.source.tools

import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.Owner
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.features.SourceAvailable
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.user.UserHandle2
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import testhelpers.BaseTest

class SimilarityCheckTest : BaseTest() {

    private val pkgRepo: PkgRepo = mockk()
    private val handle = UserHandle2(0)
    private val areaInfo = mockk<AreaInfo>().apply {
        every { prefix } returns LocalPath.build("/abc")
        every { prefixFreeSegments } returns listOf("testdir-1")
        every { userHandle } returns handle
    }

    private fun create() = SimilarityFilter(
        pkgRepo = pkgRepo,
    )

    @Test fun `false overlap, remove one`() = runTest {
        val pkgA = "pkgA".toPkgId()
        val pkgB = "pkgB".toPkgId()
        pkgRepo.apply {
            coEvery { query(pkgA, handle) } returns listOf(mockk<SourceAvailable>().apply {
                every { sourceDir } returns LocalPath.build("/abc/testdir-1")
            })
            coEvery { query(pkgB, handle) } returns listOf(mockk<SourceAvailable>().apply {
                every { sourceDir } returns LocalPath.build("/abc/testdir-2")
            })
        }

        val owners = setOf(
            Owner(pkgA, handle),
            Owner(pkgB, handle)
        )
        create().filterFalsePositives(areaInfo, owners).apply {
            map { it.pkgId } shouldBe setOf("pkgA".toPkgId())
        }
    }

    @Test fun `nothing installed, removing nothing`() = runTest {
        val pkgA = "pkgA".toPkgId()
        val pkgB = "pkgB".toPkgId()
        pkgRepo.apply {
            coEvery { query(any(), handle) } returns emptySet()
        }

        val toCheck = setOf(
            Owner(pkgA, handle),
            Owner(pkgB, handle)
        )

        create().filterFalsePositives(areaInfo, toCheck) shouldBe toCheck
    }

    @Test fun `overlap requires sourcepath to be non null`() = runTest {
        val pkgA = "pkgA".toPkgId()
        val pkgB = "pkgB".toPkgId()
        pkgRepo.apply {
            coEvery { query(pkgA, handle) } returns listOf(mockk<SourceAvailable>().apply {
                every { sourceDir } returns null
            })
            coEvery { query(pkgB, handle) } returns listOf(mockk<SourceAvailable>().apply {
                every { sourceDir } returns null
            })
        }

        val owners = setOf(
            Owner(pkgA, handle),
            Owner(pkgB, handle)
        )

        create().filterFalsePositives(areaInfo, owners) shouldBe owners
    }
}