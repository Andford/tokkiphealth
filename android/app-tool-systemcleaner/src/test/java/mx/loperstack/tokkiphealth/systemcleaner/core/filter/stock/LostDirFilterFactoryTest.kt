package mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock

import mx.loperstack.tokkiphealth.common.areas.DataArea.Type.SDCARD
import mx.loperstack.tokkiphealth.common.rngString
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilterTest
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LostDirFilterFactoryTest : SystemCleanerFilterTest() {

    @BeforeEach
    override fun setup() {
        super.setup()
    }

    @AfterEach
    override fun teardown() {
        super.teardown()
    }

    private fun create() = LostDirFilter(
        sieveFactory = object : SystemCrawlerSieve.Factory {
            override fun create(config: SystemCrawlerSieve.Config): SystemCrawlerSieve =
                SystemCrawlerSieve(config, fileForensics)
        },
        gatewaySwitch = gatewaySwitch,
    )

    @Test fun testFilter() = runTest {
        mockDefaults()
        neg(SDCARD, "LOST.DIR", Flag.Dir)
        neg(SDCARD, "somedir", Flag.Dir)
        neg(SDCARD, "somedir/LOST.DIR", Flag.Dir)
        neg(SDCARD, "LOST.DIR/$rngString", Flag.Dir)
        pos(SDCARD, "LOST.DIR/$rngString", Flag.File)
        confirm(create())
    }
}