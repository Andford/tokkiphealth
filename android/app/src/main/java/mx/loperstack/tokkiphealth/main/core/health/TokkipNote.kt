package mx.loperstack.tokkiphealth.main.core.health

import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import mx.loperstack.tokkiphealth.R
import org.json.JSONObject
import java.time.Instant

/**
 * Tokkip (the app) hears how the phone is doing, as Tokkip PC hears Healthy PC: one small note,
 * sent as an explicit broadcast to mx.loperstack.tokkip when the phone's mood changes or after a
 * clean. The same contract as Healthy PC's healthy-pc.json (src-tauri/src/notice.rs): app,
 * version, seq, at, score, mood, kind, message and, after a clean, bytes. Nothing leaves the phone.
 */
object TokkipNote {
    const val TOKKIP = "mx.loperstack.tokkip"
    const val ACTION = "mx.loperstack.tokkip.action.PHONE_HEALTH"
    const val EXTRA_JSON = "json"
    private const val PREFS = "tokkip_note"

    @Volatile private var app: Context? = null

    fun init(context: Context) {
        app = context.applicationContext
    }

    fun mood(m: PhoneHealth.Mood) = when (m) {
        PhoneHealth.Mood.HAPPY -> "happy"
        PhoneHealth.Mood.CALM -> "calm"
        PhoneHealth.Mood.TIRED -> "tired"
        PhoneHealth.Mood.DIZZY -> "dizzy"
    }

    /** The note as both apps read it: pure, so it's tested without a phone. */
    fun json(seq: Long, at: Long, score: Int, mood: String, kind: String, message: String, bytes: Long?): String =
        JSONObject()
            .put("app", "tokkip-health").put("version", 1).put("seq", seq).put("at", at)
            .put("score", score.coerceIn(0, 100)).put("mood", mood).put("kind", kind).put("message", message.take(300))
            .apply { if (bytes != null) put("bytes", bytes) }
            .toString()

    /** Called with each fresh look at the phone; says something only when there's news. */
    fun onHealth(h: PhoneHealth, lastCleanFreed: Long?) {
        val context = app ?: return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cleanAt = h.lastCleanAt?.toEpochMilli() ?: 0L
        val isClean = cleanAt > p.getLong("cleanAt", 0L) && p.contains("cleanAt") && (lastCleanFreed ?: 0L) > 0L
        val key = "${h.mood}|${h.disk}|${h.clean}"
        if (!isClean && key == p.getString("key", null)) {
            p.edit().putLong("cleanAt", cleanAt).apply()
            return
        }
        val res = context.resources
        val message = if (isClean) {
            res.getString(R.string.tokkip_note_clean, Formatter.formatShortFileSize(context, lastCleanFreed ?: 0L))
        } else {
            res.getString(
                when (h.mood) {
                    PhoneHealth.Mood.HAPPY -> R.string.dashboard_health_mood_happy
                    PhoneHealth.Mood.CALM -> R.string.dashboard_health_mood_calm
                    PhoneHealth.Mood.TIRED -> R.string.dashboard_health_mood_tired
                    PhoneHealth.Mood.DIZZY -> R.string.dashboard_health_mood_dizzy
                },
            )
        }
        val seq = p.getLong("seq", 0L) + 1
        val note = json(
            seq = seq,
            at = Instant.now().toEpochMilli(),
            score = h.score,
            mood = mood(h.mood),
            kind = if (isClean) "clean" else "status",
            message = message,
            bytes = if (isClean) lastCleanFreed else null,
        )
        val sent = runCatching {
            context.sendBroadcast(
                Intent(ACTION).setPackage(TOKKIP).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES).putExtra(EXTRA_JSON, note),
            )
        }.isSuccess
        if (sent) p.edit().putLong("seq", seq).putString("key", key).putLong("cleanAt", cleanAt).apply()
    }
}
