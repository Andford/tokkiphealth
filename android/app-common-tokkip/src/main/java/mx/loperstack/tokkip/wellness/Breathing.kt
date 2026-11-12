// Copied from Tokkip (Andford/tokkip, android/shared wellness/Logic.kt): only
// what Tokkip's drawing needs to breathe. Change it there, not here.

package mx.loperstack.tokkip.wellness

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

const val INHALE_S = 4.0
const val EXHALE_S = 6.0

enum class BreathPhase { IN, HOLD_IN, OUT, HOLD_OUT }

data class Breathing(
    val startedAt: Long? = null,
    val minutes: Int = 1,
    val inhale: Double = INHALE_S,
    val holdIn: Double = 0.0,
    val exhale: Double = EXHALE_S,
    val holdOut: Double = 0.0,
) {
    val cycle: Double get() = (inhale + holdIn + exhale + holdOut).coerceAtLeast(1.0)

}

data class BreathFrame(
    val active: Boolean,
    /** Breathing in, or holding full. */
    val inhaling: Boolean,
    /** How full Tokkip is: 0 empty … 1 full, eased. */
    val fill: Float,
    /** Seconds left in the session. */
    val left: Int,
    val breaths: Int,
    val done: Boolean,
    /** 0…1 of the session gone by. */
    val progress: Float,
    val phase: BreathPhase = BreathPhase.IN,
    /** Seconds left in this phase. */
    val phaseLeft: Double = 0.0,
)

private fun easeInOut(x: Double) = 0.5 - 0.5 * cos(PI * x.coerceIn(0.0, 1.0))

fun breathFrame(b: Breathing, nowMs: Long): BreathFrame {
    val start = b.startedAt ?: return BreathFrame(false, true, 0f, 0, 0, false, 0f)
    val total = b.minutes * 60.0
    val elapsed = max(0.0, (nowMs - start) / 1000.0)
    val done = elapsed >= total
    val t = min(elapsed, total)
    val c = t % b.cycle
    val (phase, p, len) = when {
        c < b.inhale -> Triple(BreathPhase.IN, c / b.inhale, b.inhale)
        c < b.inhale + b.holdIn -> Triple(BreathPhase.HOLD_IN, (c - b.inhale) / b.holdIn, b.holdIn)
        c < b.inhale + b.holdIn + b.exhale -> Triple(BreathPhase.OUT, (c - b.inhale - b.holdIn) / b.exhale, b.exhale)
        else -> Triple(BreathPhase.HOLD_OUT, (c - b.inhale - b.holdIn - b.exhale) / b.holdOut.coerceAtLeast(0.001), b.holdOut)
    }
    val fill = if (done) 0.0 else when (phase) {
        BreathPhase.IN -> easeInOut(p)
        BreathPhase.HOLD_IN -> 1.0
        BreathPhase.OUT -> 1 - easeInOut(p)
        BreathPhase.HOLD_OUT -> 0.0
    }
    return BreathFrame(
        active = !done,
        inhaling = phase == BreathPhase.IN || phase == BreathPhase.HOLD_IN,
        fill = fill.toFloat(),
        left = max(0, ceil(total - elapsed).toInt()),
        breaths = floor(t / b.cycle).toInt(),
        done = done,
        progress = (t / total).toFloat(),
        phase = phase,
        phaseLeft = len * (1 - p),
    )
}

