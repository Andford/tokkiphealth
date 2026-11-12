package mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock

import mx.loperstack.tokkiphealth.common.areas.DataArea.Type.PUBLIC_DATA
import mx.loperstack.tokkiphealth.common.areas.DataArea.Type.SDCARD
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilterTest
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LinuxFoldersFilterTest : SystemCleanerFilterTest() {

    @BeforeEach
    override fun setup() {
        super.setup()
    }

    @AfterEach
    override fun teardown() {
        super.teardown()
    }

    private fun create() = LinuxFilesFilter(
        sieveFactory = object : SystemCrawlerSieve.Factory {
            override fun create(config: SystemCrawlerSieve.Config): SystemCrawlerSieve =
                SystemCrawlerSieve(config, fileForensics)
        },
        gatewaySwitch = gatewaySwitch,
    )

    @Test fun testFilter() = runTest {
        mockDefaults()
        neg(PUBLIC_DATA, "/.Trash", Flag.Dir)
        neg(SDCARD, "/.Trash", Flag.Dir)
        pos(SDCARD, "/.Trash/file", Flag.Dir)
        neg(SDCARD, "/.Trash-", Flag.Dir)
        neg(SDCARD, "/.Trash-/file", Flag.Dir)
        pos(SDCARD, "/.Trash-0", Flag.Dir)
        pos(SDCARD, "/.Trash-0/file", Flag.Dir)
        pos(SDCARD, "/.Trash-1000", Flag.Dir)
        pos(SDCARD, "/.trash-1000", Flag.Dir)
        pos(SDCARD, "/.trash-1000/file", Flag.Dir)
        neg(SDCARD, "/.Trash-project", Flag.Dir)
        neg(SDCARD, "Android/.Trash-1000", Flag.Dir)
        // Android's own trash, holds files that can still be restored
        neg(SDCARD, "/.trash-storage", Flag.Dir)
        neg(SDCARD, "/.trash-storage/DCIM", Flag.Dir)
        neg(SDCARD, "/.trash-storage/DCIM/Camera", Flag.Dir)
        confirm(create())
    }
}