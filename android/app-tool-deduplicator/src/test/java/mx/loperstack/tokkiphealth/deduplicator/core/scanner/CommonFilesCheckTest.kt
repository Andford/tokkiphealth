package mx.loperstack.tokkiphealth.deduplicator.core.scanner

import mx.loperstack.tokkiphealth.common.MimeTypeTool
import mx.loperstack.tokkiphealth.common.MimeTypes
import mx.loperstack.tokkiphealth.common.files.APathLookup
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import testhelpers.BaseTest

class CommonFilesCheckTest : BaseTest() {

    private val mimeTypeTool: MimeTypeTool = mockk()

    private val subject = CommonFilesCheck(mimeTypeTool)

    private fun lookupOfType(mimeType: String): APathLookup<*> = mockk<APathLookup<*>>(relaxed = true).also {
        coEvery { mimeTypeTool.determineMimeType(it) } returns mimeType
    }

    @Test
    fun `heic is common and an image`() = runTest {
        val lookup = lookupOfType("image/heic")
        subject.isCommon(lookup) shouldBe true
        subject.isImage(lookup) shouldBe true
    }

    @Test
    fun `heif is common and an image`() = runTest {
        val lookup = lookupOfType("image/heif")
        subject.isCommon(lookup) shouldBe true
        subject.isImage(lookup) shouldBe true
    }

    @Test
    fun `apk is common`() = runTest {
        subject.isCommon(lookupOfType(MimeTypes.Apk.value)) shouldBe true
    }

    @Test
    fun `an unlisted mime type is neither common nor an image`() = runTest {
        val lookup = lookupOfType("application/x-unlisted")
        subject.isCommon(lookup) shouldBe false
        subject.isImage(lookup) shouldBe false
    }
}
