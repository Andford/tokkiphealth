package mx.loperstack.tokkiphealth.widget.tokkip

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.widget.RemoteViews
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import mx.loperstack.tokkip.character.MShapes
import mx.loperstack.tokkip.character.Mood
import mx.loperstack.tokkip.character.PolygonShape
import mx.loperstack.tokkip.character.TokkipEngine
import mx.loperstack.tokkip.character.tokkipBitmap
import mx.loperstack.tokkiphealth.R
import mx.loperstack.tokkiphealth.common.hasApiLevel
import mx.loperstack.tokkiphealth.main.core.health.PhoneHealth
import mx.loperstack.tokkiphealth.common.ui.R as CommonUiR

/**
 * What Tokkip's widgets share: his colours resolved for bitmaps (Material You on Android 12+,
 * Tokkip's teal before), his cookie, Tokkip himself moving (a ViewFlipper of a few frames, each
 * flip a smooth tween the launcher plays, as in the Tokkip app), a ring and bars.
 */
internal object TokkipWidgetKit {

    /** Tokkip's own body colour: a brand mark, like in the Tokkip app. */
    val Body = Color(0xFFF5F6F8)

    fun night(context: Context) =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun primary(context: Context): Int = when {
        hasApiLevel(31) -> context.getColor(if (night(context)) android.R.color.system_accent1_200 else android.R.color.system_accent1_600)
        else -> context.getColor(CommonUiR.color.md_theme_primary)
    }

    fun primaryContainer(context: Context): Int = when {
        hasApiLevel(31) -> context.getColor(if (night(context)) android.R.color.system_accent1_700 else android.R.color.system_accent1_100)
        else -> context.getColor(CommonUiR.color.md_theme_primaryContainer)
    }

    fun onSurface(context: Context): Int = when {
        hasApiLevel(31) -> context.getColor(if (night(context)) android.R.color.system_neutral1_50 else android.R.color.system_neutral1_900)
        else -> if (night(context)) 0xFFECECEC.toInt() else 0xFF1B1B1B.toInt()
    }

    fun track(context: Context): Int = if (night(context)) 0x33FFFFFF else 0x1F000000

    fun warn(context: Context): Int = context.getColor(CommonUiR.color.md_theme_storageLow)

    /** How Tokkip looks for this health: the same mapping as the dashboard. */
    fun mood(h: PhoneHealth, working: Boolean): Pair<Mood, TokkipEngine.Eye?> = when {
        working -> Mood.FOCUS to null
        h.mood == PhoneHealth.Mood.HAPPY -> Mood.HAPPY to null
        h.mood == PhoneHealth.Mood.TIRED -> Mood.SLEEPY to null
        h.mood == PhoneHealth.Mood.DIZZY -> Mood.IDLE to TokkipEngine.Eye.SPIRAL
        else -> Mood.IDLE to null
    }

    /** His cookie, the app icon's shape, filled with the primary container. */
    fun cookie(context: Context, px: Int): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val outline = PolygonShape(MShapes.Cookie9).createOutline(Size(px.toFloat(), px.toFloat()), LayoutDirection.Ltr, Density(1f))
        if (outline is Outline.Generic) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryContainer(context) }
            Canvas(bmp).drawPath(outline.path.asAndroidPath(), paint)
        }
        return bmp
    }

    /** Tokkip still, sitting on his cookie, in one picture (for the small spots). */
    fun onCookie(context: Context, px: Int, mood: Mood, face: TokkipEngine.Eye?): Bitmap {
        val bmp = cookie(context, px)
        val w = (px * 0.9f).toInt().coerceAtLeast(1)
        val h = (px * 0.8f).toInt().coerceAtLeast(1)
        val him = tokkipBitmap(w, h, Body, mood, face, wear = true, headroom = 0.3f)
        Canvas(bmp).drawBitmap(him, (px - w) / 2f, (px - h) / 2f + px * 0.04f, null)
        return bmp
    }

    /**
     * Tokkip moving: happy hops when the phone feels great, a soft bob otherwise. The same picture
     * three times (RemoteViews send a repeated bitmap once) plus a blink.
     */
    fun moving(context: Context, mood: Mood, face: TokkipEngine.Eye?, w: Int, h: Int): RemoteViews {
        val hop = mood == Mood.HAPPY
        val headroom = if (hop) 0.4f else 0.32f
        val flip = RemoteViews(context.packageName, if (hop) R.layout.tokkip_widget_flip_hop else R.layout.tokkip_widget_flip_bob)
        val main = tokkipBitmap(w, h, Body, mood, face, wear = true, headroom = headroom)
        val blink = if (face == null) tokkipBitmap(w, h, Body, mood, TokkipEngine.Eye.CLOSED, wear = true, headroom = headroom) else main
        for (b in listOf(main, main, main, blink)) {
            val frame = RemoteViews(context.packageName, R.layout.tokkip_widget_frame)
            frame.setImageViewBitmap(R.id.frame, b)
            flip.addView(R.id.flip, frame)
        }
        return flip
    }

    /** The health score in a ring, the number in the middle. */
    fun scoreRing(context: Context, score: Int, px: Int, warn: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val stroke = px * 0.11f
        val r = RectF(stroke / 2, stroke / 2, px - stroke / 2, px - stroke / 2)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND }
        p.color = track(context)
        c.drawArc(r, 0f, 360f, false, p)
        p.color = if (warn) warn(context) else primary(context)
        c.drawArc(r, -90f, 360f * score.coerceIn(0, 100) / 100f, false, p)
        val t = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onSurface(context)
            textAlign = Paint.Align.CENTER
            textSize = px * 0.34f
            typeface = if (hasApiLevel(28)) Typeface.create(Typeface.DEFAULT, 800, false) else Typeface.DEFAULT_BOLD
        }
        val fm = t.fontMetrics
        c.drawText(score.coerceIn(0, 100).toString(), px / 2f, px / 2f - (fm.ascent + fm.descent) / 2f, t)
        return bmp
    }

    /** Freed per day as rounded bars; today's in the primary colour, the rest a softer primary. */
    fun bars(context: Context, perDay: List<Long>, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        if (perDay.isEmpty()) return bmp
        val c = Canvas(bmp)
        val max = perDay.max().coerceAtLeast(1L)
        val n = perDay.size
        val gap = w * 0.025f
        val bw = (w - gap * (n - 1)) / n
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val primary = primary(context)
        val soft = (primary and 0x00FFFFFF) or (0x73 shl 24)
        perDay.forEachIndexed { i, v ->
            val x = i * (bw + gap)
            val bh = if (v <= 0L) bw.coerceAtMost(h * 0.08f) else (h * (v.toFloat() / max)).coerceAtLeast(bw)
            p.color = when {
                v <= 0L -> track(context)
                i == n - 1 -> primary
                else -> soft
            }
            c.drawRoundRect(RectF(x, h - bh, x + bw, h.toFloat()), bw / 2, bw / 2, p)
        }
        return bmp
    }
}
