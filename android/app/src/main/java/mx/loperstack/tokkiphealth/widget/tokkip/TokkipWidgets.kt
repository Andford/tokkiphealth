package mx.loperstack.tokkiphealth.widget.tokkip

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import mx.loperstack.tokkiphealth.R
import mx.loperstack.tokkiphealth.main.core.health.PhoneHealth
import mx.loperstack.tokkiphealth.main.ui.MainActivity
import mx.loperstack.tokkiphealth.main.ui.shortcuts.ShortcutActivity
import kotlin.math.roundToInt

// Tokkip's own widgets: "Tu teléfono" (how it feels, Tokkip moving on his cookie, the score) and
// "Historial de limpieza" (what he freed, day by day, two weeks). Each card opens the app; a
// distinct data URI per target keeps their PendingIntents apart (filterEquals ignores extras).

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TokkipWidgetEntryPoint {
    fun tokkipWidgetData(): TokkipWidgetData
}

private fun data(context: Context) =
    EntryPointAccessors.fromApplication(context.applicationContext, TokkipWidgetEntryPoint::class.java).tokkipWidgetData()

internal fun tokkipOpenHomeIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        data = Uri.parse("tokkiphealth://widget/tokkip/home")
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

internal fun tokkipOpenReportsIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        data = Uri.parse("tokkiphealth://widget/tokkip/reports")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra(ShortcutActivity.EXTRA_SHORTCUT_ACTION, ShortcutActivity.ACTION_OPEN_REPORTS)
    }

/** Redraws Tokkip's widgets; cheap when none is placed. */
object TokkipWidgets {
    suspend fun updateAll(context: Context) {
        val glance = GlanceAppWidgetManager(context)
        runCatching { if (glance.getGlanceIds(PhoneHealthWidget::class.java).isNotEmpty()) PhoneHealthWidget().updateAll(context) }
        runCatching { if (glance.getGlanceIds(CleanHistoryWidget::class.java).isNotEmpty()) CleanHistoryWidget().updateAll(context) }
    }
}

private fun Context.px(dp: Dp) = (dp.value * resources.displayMetrics.density).roundToInt().coerceAtLeast(1)

private fun bytes(context: Context, bytes: Long) = Formatter.formatShortFileSize(context, bytes)

@Composable
private fun TokkipCard(modifier: GlanceModifier = GlanceModifier, content: @Composable () -> Unit) {
    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(28.dp)
                .then(modifier)
                .padding(14.dp),
        ) { content() }
    }
}

/** Tokkip on his cookie, moving. */
@Composable
private fun TokkipOnCookie(side: Dp, state: TokkipWidgetState) {
    val context = LocalContext.current
    val (mood, face) = TokkipWidgetKit.mood(state.health, state.working)
    Box(modifier = GlanceModifier.size(side), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(TokkipWidgetKit.cookie(context, context.px(side))),
            contentDescription = null,
            modifier = GlanceModifier.size(side),
        )
        val w = side * 0.92f
        val h = side * 0.86f
        AndroidRemoteViews(
            TokkipWidgetKit.moving(context, mood, face, context.px(w).coerceAtMost(260), context.px(h).coerceAtMost(240)),
            GlanceModifier.size(w, h),
        )
    }
}

private fun moodShort(m: PhoneHealth.Mood) = when (m) {
    PhoneHealth.Mood.HAPPY -> R.string.widget_tokkip_mood_happy
    PhoneHealth.Mood.CALM -> R.string.widget_tokkip_mood_calm
    PhoneHealth.Mood.TIRED -> R.string.widget_tokkip_mood_tired
    PhoneHealth.Mood.DIZZY -> R.string.widget_tokkip_mood_dizzy
}

private fun moodLong(m: PhoneHealth.Mood) = when (m) {
    PhoneHealth.Mood.HAPPY -> R.string.dashboard_health_mood_happy
    PhoneHealth.Mood.CALM -> R.string.dashboard_health_mood_calm
    PhoneHealth.Mood.TIRED -> R.string.dashboard_health_mood_tired
    PhoneHealth.Mood.DIZZY -> R.string.dashboard_health_mood_dizzy
}

private fun lastClean(context: Context, h: PhoneHealth): String {
    val at = h.lastCleanAt ?: return context.getString(R.string.dashboard_health_stat_clean_never)
    val now = System.currentTimeMillis()
    val ago = if (now - at.toEpochMilli() < DateUtils.MINUTE_MILLIS) {
        context.getString(R.string.dashboard_health_stat_clean_now)
    } else {
        DateUtils.getRelativeTimeSpanString(at.toEpochMilli(), now, DateUtils.MINUTE_MILLIS).toString()
    }
    return ago
}

// ── Tu teléfono ────────────────────────────────────────────────────────────────

class PhoneHealthWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val provider = data(context)
        val initial = provider.snapshot()
        provideContent {
            val state by provider.state.collectAsState(initial)
            PhoneHealthContent(state)
        }
    }
}

class PhoneHealthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PhoneHealthWidget()
}

@Composable
private fun PhoneHealthContent(state: TokkipWidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val h = state.health
    val warn = h.disk != PhoneHealth.Level.OK || h.clean == PhoneHealth.Level.BAD
    val title = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    val dim = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    TokkipCard(GlanceModifier.clickable(actionStartActivity(tokkipOpenHomeIntent(context)))) {
        if (size.width < 220.dp) {
            // Small: Tokkip, then the score and how he feels.
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val side = minOf(size.width - 28.dp, size.height - 64.dp).coerceIn(48.dp, 110.dp)
                TokkipOnCookie(side, state)
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    if (state.working) context.getString(R.string.dashboard_health_working)
                    else "${h.score} · ${context.getString(moodShort(h.mood))}",
                    style = title,
                    maxLines = 1,
                )
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                    val side = (size.height - 28.dp - (if (size.height >= 170.dp) 52.dp else 0.dp)).coerceIn(56.dp, 112.dp)
                    TokkipOnCookie(side, state)
                    Spacer(GlanceModifier.width(12.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            context.getString(if (state.working) R.string.dashboard_health_working else moodLong(h.mood)),
                            style = title,
                            maxLines = 2,
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            context.getString(R.string.widget_tokkip_free, bytes(context, h.storageFree)),
                            style = dim,
                            maxLines = 1,
                        )
                        Text(
                            context.getString(R.string.widget_tokkip_last_clean, lastClean(context, h)),
                            style = dim,
                            maxLines = 1,
                        )
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Image(
                        provider = ImageProvider(TokkipWidgetKit.scoreRing(context, h.score, context.px(54.dp), warn)),
                        contentDescription = context.getString(R.string.dashboard_health_score_desc, h.score),
                        modifier = GlanceModifier.size(54.dp),
                    )
                }
                if (size.height >= 170.dp) {
                    Spacer(GlanceModifier.height(10.dp))
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .cornerRadius(21.dp)
                            .background(GlanceTheme.colors.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            context.getString(R.string.dashboard_health_action),
                            style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ── Historial de limpieza ──────────────────────────────────────────────────────

class CleanHistoryWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val provider = data(context)
        val initial = provider.snapshot()
        provideContent {
            val state by provider.state.collectAsState(initial)
            CleanHistoryContent(state)
        }
    }
}

class CleanHistoryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CleanHistoryWidget()
}

@Composable
private fun CleanHistoryContent(state: TokkipWidgetState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val dim = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    TokkipCard(GlanceModifier.clickable(actionStartActivity(tokkipOpenReportsIntent(context)))) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        context.getString(R.string.widget_tokkip_history_short),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Text(
                        bytes(context, state.health.totalFreed),
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    if (state.cleans > 0) {
                        Text(
                            context.resources.getQuantityString(R.plurals.dashboard_history_freed_in_x, state.cleans, state.cleans),
                            style = dim,
                            maxLines = 1,
                        )
                    }
                }
                TokkipOnCookie(if (size.height >= 150.dp) 62.dp else 48.dp, state)
            }
            Spacer(GlanceModifier.height(8.dp))
            if (state.cleans == 0 || state.perDay.all { it <= 0L }) {
                Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.CenterStart) {
                    Text(context.getString(R.string.dashboard_history_empty), style = dim, maxLines = 3)
                }
            } else {
                val barsH = (size.height - 28.dp - 92.dp).coerceIn(18.dp, 120.dp)
                val barsW = (size.width - 28.dp).coerceAtLeast(40.dp)
                Image(
                    provider = ImageProvider(TokkipWidgetKit.bars(context, state.perDay, context.px(barsW), context.px(barsH))),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                )
                Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 3.dp)) {
                    Text(context.getString(R.string.dashboard_history_two_weeks), style = dim, maxLines = 1)
                    Spacer(GlanceModifier.defaultWeight())
                    Text(context.getString(R.string.dashboard_history_today), style = dim, maxLines = 1)
                }
            }
        }
    }
}
