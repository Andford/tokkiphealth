package mx.loperstack.tokkiphealth.appcleaner.core.forensics

import android.content.Context
import android.content.res.AssetManager
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.sieves.DynamicAppSieve2
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.sieves.JsonAppSieve
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.DataArea.Type
import mx.loperstack.tokkiphealth.common.areas.DataAreaManager
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.ReadException
import mx.loperstack.tokkiphealth.common.files.Segments
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.forensics.FileForensics
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.rngString
import mx.loperstack.tokkiphealth.common.serialization.SerializationAppModule
import mx.loperstack.tokkiphealth.common.storage.StorageEnvironment
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleanerSettings
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import testhelpers.BaseTest
import java.time.Instant

abstract class BaseFilterTest : BaseTest() {

    @MockK lateinit var pkgOps: PkgOps
    @MockK lateinit var pkgRepo: PkgRepo
    @MockK lateinit var areaManager: DataAreaManager
    @MockK lateinit var fileForensics: FileForensics
    @MockK lateinit var gatewaySwitch: GatewaySwitch
    @MockK lateinit var storageEnvironment: StorageEnvironment
    @MockK lateinit var systemCleanerSettings: SystemCleanerSettings
    @MockK lateinit var context: Context
    @MockK lateinit var assetManager: AssetManager

    val storageData1 = mockk<DataArea>().apply {
        every { flags } returns setOf(DataArea.Flag.PRIMARY)
        every { type } returns Type.DATA
        every { path } returns LocalPath.build("/data")
    }

    val storageData2 = mockk<DataArea>().apply {
        every { flags } returns emptySet()
        every { type } returns Type.DATA
        every { path } returns LocalPath.build("/mnt/expand", rngString)
    }

    val storageDataApp1 = mockk<DataArea>().apply {
        every { flags } returns storageData1.flags
        every { type } returns Type.APP_APP
        every { path } returns storageData1.path.child("app")
    }

    val storageDataApp2 = mockk<DataArea>().apply {
        every { flags } returns storageData2.flags
        every { type } returns Type.APP_APP
        every { path } returns storageData2.path.child("app")
    }

    val storageDataAppAsec1 = mockk<DataArea>().apply {
        every { flags } returns storageData1.flags
        every { type } returns Type.APP_ASEC
        every { path } returns storageData1.path.child("app-asec")
    }

    val storageDataAppAsec2 = mockk<DataArea>().apply {
        every { flags } returns storageData2.flags
        every { type } returns Type.APP_ASEC
        every { path } returns storageData2.path.child("app-asec")
    }

    val storageDataAppPrivate1 = mockk<DataArea>().apply {
        every { flags } returns storageData1.flags
        every { type } returns Type.APP_APP_PRIVATE
        every { path } returns storageData1.path.child("app-private")
    }

    val storageDataAppPrivate2 = mockk<DataArea>().apply {
        every { flags } returns storageData2.flags
        every { type } returns Type.APP_APP_PRIVATE
        every { path } returns storageData2.path.child("app-private")
    }

    val storageDataAppLib1 = mockk<DataArea>().apply {
        every { flags } returns storageData1.flags
        every { type } returns Type.APP_LIB
        every { path } returns storageData1.path.child("app-lib")
    }

    val storageDataAppLib2 = mockk<DataArea>().apply {
        every { flags } returns storageData2.flags
        every { type } returns Type.APP_LIB
        every { path } returns storageData2.path.child("app-lib")
    }

    val storageDataSystem1 = mockk<DataArea>().apply {
        every { flags } returns storageData1.flags
        every { type } returns Type.DATA_SYSTEM
        every { path } returns storageData1.path.child("system")
    }

    val storageDataSystem2 = mockk<DataArea>().apply {
        every { flags } returns storageData2.flags
        every { type } returns Type.DATA_SYSTEM
        every { path } returns storageData2.path.child("system")
    }

    val storageDataSystemCE1 = mockk<DataArea>().apply {
        every { flags } returns storageData1.flags
        every { type } returns Type.DATA_SYSTEM_CE
        every { path } returns storageData1.path.child("system_ce")
    }

    val storageDataSystemCE2 = mockk<DataArea>().apply {
        every { flags } returns storageData2.flags
        every { type } returns Type.DATA_SYSTEM_CE
        every { path } returns storageData2.path.child("system_ce")
    }

    val storageDataSystemDE1 = mockk<DataArea>().apply {
        every { flags } returns storageData1.flags
        every { type } returns Type.DATA_SYSTEM_DE
        every { path } returns storageData1.path.child("system_de")
    }

    val storageDataSystemDE2 = mockk<DataArea>().apply {
        every { flags } returns storageData2.flags
        every { type } returns Type.DATA_SYSTEM_DE
        every { path } returns storageData2.path.child("system_de")
    }

    val storageDalvikDex1 = mockk<DataArea>().apply {
        every { flags } returns storageData1.flags
        every { type } returns Type.DALVIK_DEX
        every { path } returns storageData1.path.child("dalvik-cache", "arm64")
    }

    val storageDalvikDex2 = mockk<DataArea>().apply {
        every { flags } returns storageData2.flags
        every { type } returns Type.DALVIK_DEX
        every { path } returns storageData2.path.child("dalvik-cache", "arm64")
    }

    val storageDalvikProfile1 = mockk<DataArea>().apply {
        every { flags } returns storageData1.flags
        every { type } returns Type.DALVIK_PROFILE
        every { path } returns storageData1.path.child("dalvik-cache", "profiles")
    }

    val storageDalvikProfile2 = mockk<DataArea>().apply {
        every { flags } returns storageData2.flags
        every { type } returns Type.DALVIK_PROFILE
        every { path } returns storageData2.path.child("dalvik-cache", "profiles")
    }

    val storageSdcard1 = mockk<DataArea>().apply {
        every { flags } returns setOf(DataArea.Flag.PRIMARY)
        every { type } returns Type.SDCARD
        every { path } returns LocalPath.build("/storage", "emulated", "0")
    }

    val storageSdcard2 = mockk<DataArea>().apply {
        every { flags } returns emptySet()
        every { type } returns Type.SDCARD
        every { path } returns LocalPath.build("/storage", "ABCD-EFGH")
    }

    val storageAndroidData1 = mockk<DataArea>().apply {
        every { flags } returns storageSdcard1.flags
        every { type } returns Type.PUBLIC_DATA
        every { path } returns storageSdcard1.path.child("Android", "data")
    }

    val storageAndroidData2 = mockk<DataArea>().apply {
        every { flags } returns storageSdcard2.flags
        every { type } returns Type.PUBLIC_DATA
        every { path } returns storageSdcard2.path.child("Android", "data")
    }

    val storageAndroidObb1 = mockk<DataArea>().apply {
        every { flags } returns storageSdcard1.flags
        every { type } returns Type.PUBLIC_OBB
        every { path } returns storageSdcard1.path.child("Android", "obb")
    }

    val storageAndroidObb2 = mockk<DataArea>().apply {
        every { flags } returns storageSdcard2.flags
        every { type } returns Type.PUBLIC_OBB
        every { path } returns storageSdcard2.path.child("Android", "obb")
    }

    val storageAndroidMedia1 = mockk<DataArea>().apply {
        every { flags } returns storageSdcard1.flags
        every { type } returns Type.PUBLIC_MEDIA
        every { path } returns storageSdcard1.path.child("Android", "media")
    }

    val storageAndroidMedia2 = mockk<DataArea>().apply {
        every { flags } returns storageSdcard2.flags
        every { type } returns Type.PUBLIC_MEDIA
        every { path } returns storageSdcard2.path.child("Android", "media")
    }

    val storageCachePartition = mockk<DataArea>().apply {
        every { flags } returns emptySet()
        every { type } returns Type.DOWNLOAD_CACHE
        every { path } returns LocalPath.build("cache")
    }

    private val candidates = mutableListOf<Candidate>()

    @BeforeEach
    open fun setup() {
        if (!::pkgOps.isInitialized) {
            MockKAnnotations.init(this)
        }

        every { context.assets } returns assetManager
        every { assetManager.open(any()) } answers {
            BaseFilterTest::class.java.classLoader.getResourceAsStream(arg(0))
        }

        every { areaManager.state } returns flowOf(
            DataAreaManager.State(
                areas = setOf(
                    storageData1,
                    storageData2,
                    storageDataApp1,
                    storageDataApp2,
                    storageDataAppAsec1,
                    storageDataAppAsec2,
                    storageDataAppPrivate1,
                    storageDataAppPrivate2,
                    storageDataAppLib1,
                    storageDataAppLib2,
                    storageDataSystem1,
                    storageDataSystem2,
                    storageDataSystemCE1,
                    storageDataSystemCE2,
                    storageDataSystemDE1,
                    storageDataSystemDE2,
                    storageDalvikDex1,
                    storageDalvikDex2,
                    storageDalvikProfile1,
                    storageDalvikProfile2,
                    storageSdcard1,
                    storageSdcard2,
                    storageAndroidData1,
                    storageAndroidData2,
                    storageAndroidObb1,
                    storageAndroidObb2,
                    storageAndroidMedia1,
                    storageAndroidMedia2,
                    storageCachePartition,
                )
            )
        )

        coEvery { gatewaySwitch.listFiles(any()) } returns emptyFlow()
        coEvery { gatewaySwitch.exists(any()) } returns false
        coEvery { gatewaySwitch.canRead(any()) } returns false
        coEvery { gatewaySwitch.lookupFiles(any()) } coAnswers {
            val path = arg<APath>(0)
            flow { throw ReadException(path = path) }
        }
        storageEnvironment.apply {
            every { dataDir } returns LocalPath.build("/data")
            every { ourCacheDirs } returns listOf(
                LocalPath.build("/data/data/mx.loperstack.tokkiphealth/cache")
            )
            every { ourExternalCacheDirs } returns listOf(
                LocalPath.build("/storage/emulated/0/Android/data/mx.loperstack.tokkiphealth/cache"),
                LocalPath.build("/storage/ABCD-1234/Android/data/mx.loperstack.tokkiphealth/cache"),
            )
            every { ourCodeCacheDirs } returns listOf(
                LocalPath.build("/data/data/mx.loperstack.tokkiphealth/code_cache")
            )
        }

        coEvery { pkgOps.viewArchive(any(), any()) } returns null

        coEvery { pkgRepo.query(any(), any()) } returns emptySet()
    }

    @AfterEach
    open fun teardown() {
        candidates.clear()
    }

    val testPkg: String = "test.pkg.name"

    fun addDefaultNegatives() {
        neg(testPkg, Type.SDCARD, "sdcard")
        neg(testPkg, Type.SDCARD, "sdcard", rngString)
        neg(testPkg, Type.SDCARD, "eu.thedarken.sdm", "files")
        neg(testPkg, Type.SDCARD, "eu.thedarken.sdm")
        neg(testPkg, Type.SDCARD, "eu.thedarken.sdm.huawei", "files")
        neg(testPkg, Type.SDCARD, "eu.thedarken.sdm.huawei")
        neg(testPkg, Type.SDCARD, "DCIM")
        neg(testPkg, Type.SDCARD, "DCIM", "Camera")
        neg(testPkg, Type.SDCARD, "Android")
        neg(testPkg, Type.SDCARD, "Android", "data")
        neg(testPkg, Type.SDCARD, "Android", "data", rngString)
        neg(testPkg, Type.SDCARD, "Photos")
        neg(testPkg, Type.SDCARD, "Pictures")
        neg(testPkg, Type.SDCARD, "Camera")
        neg(testPkg, Type.SDCARD, "Music")
        neg(testPkg, Type.PRIVATE_DATA, "data", "data")
        neg(testPkg, Type.DATA, "data")
        neg(testPkg, Type.SYSTEM_APP, "system", "app")
        neg(testPkg, Type.SYSTEM, "system")
        neg(testPkg, Type.DOWNLOAD_CACHE, "cache")
    }

    fun addCandidate(candidate: Candidate) {
        candidates.add(candidate)
    }

    suspend fun confirm(filter: ExpendablesFilter) {
        candidates.isEmpty() shouldBe false

        filter.initialize()

        for (c in candidates) {
            val target: APathLookup<APath> = mockk<APathLookup<APath>>().apply {
                c.lastModified?.let {
                    every { modifiedAt } returns it
                }
                every { name } returns c.pfpSegs.last()
            }

            when (c.matchType) {
                Candidate.Type.POSITIVE -> {
                    c.areaTypes.forEach { loc ->
                        c.pkgs.forEach { pkg ->
                            withClue("Should match $pkg, $loc ${c.pfpSegs}") {
                                filter.match(pkg, target, loc, c.pfpSegs) shouldNotBe null
                            }
                        }
                    }
                }

                Candidate.Type.NEGATIVE -> {
                    c.areaTypes.forEach { loc ->
                        c.pkgs.forEach { pkg ->
                            withClue("Should NOT match $pkg, $loc ${c.pfpSegs}") {
                                filter.match(pkg, target, loc, c.pfpSegs) shouldBe null
                            }
                        }
                    }
                }
            }
        }
    }

    data class Candidate(
        val matchType: Type,
        val areaTypes: Collection<DataArea.Type>,
        val pkgs: Collection<Pkg.Id>,
        val pfpSegs: Segments,
        val lastModified: Instant? = null
    ) {
        enum class Type {
            POSITIVE, NEGATIVE
        }

    }

    fun createJsonSieveFactory() = object : JsonAppSieve.Factory {
        override fun create(assetPath: String): JsonAppSieve {
            return JsonAppSieve(
                context = context,
                assetPath = assetPath,
                json = SerializationAppModule().json(),
            )
        }
    }

    fun createDynamicSieve2Factory() = object : DynamicAppSieve2.Factory {
        override fun create(configs: Set<DynamicAppSieve2.MatchConfig>): DynamicAppSieve2 {
            return DynamicAppSieve2(
                configs = configs,
            )
        }
    }
}