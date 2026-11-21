package mx.loperstack.tokkiphealth.main.core.health

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class PhoneHealthTest {
    private val now = Instant.parse("2026-11-21T12:00:00Z")

    @Test
    fun `plenty of space and a recent clean is happy`() {
        val h = PhoneHealth.assess(1000, 500, now - Duration.ofDays(3), 0, now)
        h.score shouldBe 100
        h.mood shouldBe PhoneHealth.Mood.HAPPY
        h.disk shouldBe PhoneHealth.Level.OK
        h.clean shouldBe PhoneHealth.Level.OK
    }

    @Test
    fun `a full phone that was never cleaned makes him dizzy`() {
        val h = PhoneHealth.assess(1000, 50, null, 0, now)
        h.score shouldBe 40
        h.mood shouldBe PhoneHealth.Mood.DIZZY
        h.disk shouldBe PhoneHealth.Level.BAD
        h.clean shouldBe PhoneHealth.Level.BAD
    }

    @Test
    fun `same thresholds as Healthy PC`() {
        PhoneHealth.assess(1000, 150, now - Duration.ofDays(20), 0, now).let {
            it.score shouldBe 70
            it.mood shouldBe PhoneHealth.Mood.CALM
        }
        PhoneHealth.assess(1000, 150, now - Duration.ofDays(40), 0, now).mood shouldBe PhoneHealth.Mood.TIRED
    }
}
