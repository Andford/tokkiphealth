package mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter

import mx.loperstack.tokkiphealth.appcleaner.core.forensics.BaseFilterTest
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.neg
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.pos
import mx.loperstack.tokkiphealth.common.areas.DataArea.Type
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CodeCacheFilterTest : BaseFilterTest() {

    @BeforeEach
    override fun setup() {
        super.setup()
    }

    @AfterEach
    override fun teardown() {
        super.teardown()
    }

    private fun create() = CodeCacheFilter(
        environment = storageEnvironment,
        gatewaySwitch = gatewaySwitch,
    )

    @Test fun `test code cache filter`() = runTest {
        neg(testPkg, Type.PRIVATE_DATA, "com.tumblr", "code_cache")
        pos(testPkg, Type.PRIVATE_DATA, "com.tumblr", "code_cache", "test")

        confirm(create())
    }
}