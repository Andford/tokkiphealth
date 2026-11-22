package mx.loperstack.tokkiphealth.main.ui.dashboard.cards

import io.kotest.matchers.shouldBe
import mx.loperstack.tokkiphealth.main.core.SDMTool
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HistoryDashboardCardItemTest {
    private val zone = ZoneId.of("America/Monterrey")
    private val today = LocalDate.of(2026, 11, 21)

    @Test
    fun `groups what was freed by day, today last`() {
        val h = HistoryDashboardCardItem.build(
            cleans = listOf(
                CleanEntry(SDMTool.Type.APPCLEANER, 100, Instant.parse("2026-11-21T18:00:00Z")),
                CleanEntry(SDMTool.Type.CORPSEFINDER, 50, Instant.parse("2026-11-21T20:00:00Z")),
                CleanEntry(SDMTool.Type.SYSTEMCLEANER, 30, Instant.parse("2026-11-20T18:00:00Z")),
                CleanEntry(SDMTool.Type.SYSTEMCLEANER, 999, Instant.parse("2026-10-01T18:00:00Z")),
            ),
            totalFreed = 1179,
            today = today,
            zone = zone,
            onOpen = {},
        )
        h.perDay.size shouldBe HistoryDashboardCardItem.DAYS
        h.perDay.last() shouldBe 150
        h.perDay[HistoryDashboardCardItem.DAYS - 2] shouldBe 30
        h.perDay.sum() shouldBe 180
        h.cleans shouldBe 4
        h.recent.map { it.bytes } shouldBe listOf(50L, 100L, 30L)
    }
}
