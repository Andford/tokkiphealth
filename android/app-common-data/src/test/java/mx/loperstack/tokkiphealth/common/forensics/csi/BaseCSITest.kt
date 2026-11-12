package mx.loperstack.tokkiphealth.common.forensics.csi

import android.os.storage.StorageManager
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.DataAreaManager
import mx.loperstack.tokkiphealth.common.clutter.ClutterRepo
import mx.loperstack.tokkiphealth.common.clutter.Marker
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.forensics.CSIProcessor
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.features.SourceAvailable
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.shell.ShellOps
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsResult
import mx.loperstack.tokkiphealth.common.storage.StorageEnvironment
import mx.loperstack.tokkiphealth.common.user.UserHandle2
import mx.loperstack.tokkiphealth.common.user.UserManager2
import mx.loperstack.tokkiphealth.common.user.UserProfile2
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import testhelpers.BaseTest

abstract class BaseCSITest : BaseTest() {

    @MockK lateinit var pkgRepo: PkgRepo
    @MockK lateinit var areaManager: DataAreaManager
    @MockK lateinit var clutterRepo: ClutterRepo
    @MockK lateinit var storageManager: StorageManager
    @MockK lateinit var gatewaySwitch: GatewaySwitch
    @MockK lateinit var userManager2: UserManager2
    @MockK lateinit var storageEnvironment: StorageEnvironment
    @MockK lateinit var pkgOps: PkgOps
    @MockK lateinit var shellOps: ShellOps

    private val pkgs = mutableSetOf<Installed>()

    @BeforeEach
    open fun setup() {
        if (!::pkgOps.isInitialized) {
            MockKAnnotations.init(this)
        }
        coEvery { clutterRepo.match(any(), any()) } returns emptySet()
        coEvery { gatewaySwitch.listFiles(any()) } returns emptyFlow()
        coEvery { gatewaySwitch.exists(any()) } returns false
        coEvery { userManager2.currentUser() } returns UserProfile2(UserHandle2(0))
        every { storageEnvironment.dataDir } returns LocalPath.build("/data")

        coEvery { pkgRepo.query(any(), any()) } returns emptySet()
        every { pkgRepo.data } answers { flowOf(PkgRepo.PkgData.from(pkgs)) }
        coEvery { pkgOps.viewArchive(any(), any()) } returns null
        coEvery { shellOps.execute(any(), any()) } returns ShellOpsResult(-1, emptyList(), emptyList())
    }

    @AfterEach
    open fun teardown() {
        pkgs.clear()
    }

    suspend fun CSIProcessor.assertJurisdiction(type: DataArea.Type) {
        DataArea.Type.values().forEach {
            if (it == type) {
                hasJurisdiction(it) shouldBe true
            } else {
                hasJurisdiction(it) shouldBe false
            }
        }
    }

    open fun mockPkg(
        pkgId: Pkg.Id,
        source: LocalPath? = null,
        userHandle: UserHandle2 = UserHandle2(0),
    ): Installed {
        val mockPkg = mockk<SourceAvailable>().apply {
            every { id } returns pkgId
            every { sourceDir } returns source
            every { packageInfo } returns mockk()
            every { this@apply.userHandle } returns userHandle
        }
        coEvery { pkgRepo.query(pkgId, UserHandle2(-1)) } returns setOf(mockPkg)
        coEvery { pkgRepo.query(pkgId, userHandle) } returns setOf(mockPkg)

        return mockPkg.also {
            pkgs.add(it)
        }
    }

    open fun mockMarker(pkgId: Pkg.Id, location: DataArea.Type, prefixFree: String) {

        val marker = mockk<Marker>().apply {
            every { areaType } returns location
            every { segments } returns prefixFree.split("/")
            every { flags } returns emptySet()
        }

        val match = mockk<Marker.Match>().apply {
            every { packageNames } returns setOf(pkgId)
            every { flags } returns emptySet()
        }
        every { marker.match(location, prefixFree.split("/")) } returns match

        clutterRepo.apply {
            coEvery { getMarkerForPkg(pkgId) } returns setOf(marker)
            coEvery { getMarkerForLocation(location) } returns setOf(marker)
            coEvery { match(location, prefixFree.split("/")) } returns setOf(match)
        }
    }

    @Test abstract fun `test jurisdiction`()

    @Test abstract fun `determine area successfully`()

    @Test abstract fun `fail to determine area`()

}