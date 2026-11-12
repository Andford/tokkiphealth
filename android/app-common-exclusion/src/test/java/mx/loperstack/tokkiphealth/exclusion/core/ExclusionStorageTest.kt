package mx.loperstack.tokkiphealth.exclusion.core

import android.content.Context
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.serialization.SerializationIOModule
import mx.loperstack.tokkiphealth.exclusion.core.types.PathExclusion
import mx.loperstack.tokkiphealth.exclusion.core.types.PkgExclusion
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import testhelpers.BaseTest
import testhelpers.coroutine.TestDispatcherProvider
import testhelpers.json.toComparableJson
import java.io.File

class ExclusionStorageTest : BaseTest() {
    @TempDir lateinit var testFolder: File
    private val json = SerializationIOModule().json()

    private fun createContext(): Context = mockk<Context>().apply {
        every { filesDir } returns testFolder
    }

    fun create() = ExclusionStorage(
        dispatcherProvider = TestDispatcherProvider(),
        context = createContext(),
        json = json,
    )

    @Test
    fun `combined serialization`() = runTest {
        val storage = create()

        val original = setOf(
            PkgExclusion(
                pkgId = "test.pkg".toPkgId()
            ),
            PathExclusion(LocalPath.build("test", "path"))
        )

        storage.save(original)

        val saveFile = File(testFolder, "exclusions/exclusions-v1.json")
        saveFile.readText().toComparableJson() shouldBe """
            [
                {
                    "pkgId": {
                        "name": "test.pkg"
                    },
                    "tags": [
                        "GENERAL"
                    ]
                },
                {
                    "path": {
                        "file": "/test/path"
                    },
                    "tags": [
                        "GENERAL"
                    ]
                }
            ]
        """.toComparableJson()

        storage.load() shouldBe original
    }
}
