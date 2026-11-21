package mx.loperstack.tokkiphealth.main.core.health

import java.time.Duration
import java.time.Instant

/**
 * How the phone feels, as Tokkip shows it: the same score and moods as Healthy PC
 * (src-tauri/src/health.rs), so Tokkip feels the same on the PC and on the phone.
 * Free space weighs most, then how long since the last clean.
 */
data class PhoneHealth(
    val score: Int,
    val mood: Mood,
    val storageTotal: Long,
    val storageFree: Long,
    val lastCleanAt: Instant?,
    val totalFreed: Long,
    val disk: Level,
    val clean: Level,
) {
    enum class Mood { HAPPY, CALM, TIRED, DIZZY }
    enum class Level { OK, WARN, BAD }

    val freeRatio: Float get() = if (storageTotal > 0) storageFree.toFloat() / storageTotal else 1f

    companion object {
        fun assess(
            storageTotal: Long,
            storageFree: Long,
            lastCleanAt: Instant?,
            totalFreed: Long,
            now: Instant,
        ): PhoneHealth {
            var score = 100
            val free = if (storageTotal > 0) storageFree.toDouble() / storageTotal else 1.0
            val disk = when {
                free < 0.10 -> Level.BAD.also { score -= 40 }
                free < 0.20 -> Level.WARN.also { score -= 20 }
                else -> Level.OK
            }
            val days = lastCleanAt?.let { Duration.between(it, now).toDays() } ?: Long.MAX_VALUE
            val clean = when {
                days > 30 -> Level.BAD.also { score -= 20 }
                days > 14 -> Level.WARN.also { score -= 10 }
                else -> Level.OK
            }
            score = score.coerceIn(0, 100)
            val mood = when {
                score >= 85 -> Mood.HAPPY
                score >= 65 -> Mood.CALM
                score >= 45 -> Mood.TIRED
                else -> Mood.DIZZY
            }
            return PhoneHealth(score, mood, storageTotal, storageFree, lastCleanAt, totalFreed, disk, clean)
        }
    }
}
