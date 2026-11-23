package mx.loperstack.tokkiphealth.widget.tokkip

import io.kotest.matchers.shouldBe
import mx.loperstack.tokkiphealth.main.core.health.PhoneHealth
import org.junit.jupiter.api.Test
import testhelpers.BaseTest
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class TokkipWidgetStateTest : BaseTest() {
    private val zone = ZoneId.of("America/Monterrey")
    private val now = Instant.parse("2026-11-22T18:00:00Z")
    private val GB = 1_000_000_000L

    @Test
    fun `cleans land on their day, today last`() {
        val s = TokkipWidgetState.build(
            total = 128 * GB, free = 64 * GB,
            cleans = listOf(now to 2 * GB, now.minus(Duration.ofDays(1)) to GB, now.minus(Duration.ofDays(30)) to 5 * GB, now to 0L),
            totalFreed = 8 * GB, working = false, now = now, zone = zone,
        )
        s.perDay.size shouldBe TokkipWidgetState.DAYS
        s.perDay.last() shouldBe 2 * GB
        s.perDay[TokkipWidgetState.DAYS - 2] shouldBe GB
        s.perDay.sum() shouldBe 3 * GB
        s.cleans shouldBe 3
        s.health.lastCleanAt shouldBe now
        s.health.mood shouldBe PhoneHealth.Mood.HAPPY
    }

    @Test
    fun `no cleans and a full phone make Tokkip dizzy`() {
        val s = TokkipWidgetState.build(128 * GB, 5 * GB, emptyList(), 0L, working = false, now = now, zone = zone)
        s.cleans shouldBe 0
        s.perDay.all { it == 0L } shouldBe true
        s.health.mood shouldBe PhoneHealth.Mood.DIZZY
    }
}
