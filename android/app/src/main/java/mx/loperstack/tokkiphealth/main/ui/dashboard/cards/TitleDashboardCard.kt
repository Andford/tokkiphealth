package mx.loperstack.tokkiphealth.main.ui.dashboard.cards

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mx.loperstack.tokkip.character.Mood
import mx.loperstack.tokkip.character.Tokkip
import mx.loperstack.tokkip.character.TokkipEngine
import mx.loperstack.tokkiphealth.R
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper
import mx.loperstack.tokkiphealth.common.upgrade.UpgradeRepo
import mx.loperstack.tokkiphealth.main.core.health.PhoneHealth
import java.time.Duration
import java.time.Instant

/**
 * The top of the home screen, as in Healthy PC: Tokkip shows how the phone feels, says why in a
 * sentence, and offers one button. Below it, the phone's free space, its last clean and what
 * Tokkip has freed so far.
 */
data class TitleDashboardCardItem(
    val upgradeInfo: UpgradeRepo.Info?,
    val isWorking: Boolean,
    val onRibbonClicked: () -> Unit,
    val webpageTool: WebpageTool,
    val onMascotTriggered: (Boolean) -> Unit,
    val health: PhoneHealth? = null,
    val onClean: () -> Unit = {},
) : DashboardItem {
    override val stableId: Long = this.javaClass.hashCode().toLong()
}

@Composable
internal fun TitleDashboardCard(
    item: TitleDashboardCardItem,
    mascotTopBump: Int = 0,
) {
    val h = item.health
    val still = LocalInspectionMode.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(width = 120.dp, height = 100.dp), contentAlignment = Alignment.Center) {
                    Tokkip(
                        modifier = Modifier.height(100.dp),
                        mood = when {
                            item.isWorking -> Mood.FOCUS
                            h?.mood == PhoneHealth.Mood.HAPPY -> Mood.HAPPY
                            h?.mood == PhoneHealth.Mood.TIRED -> Mood.SLEEPY
                            else -> Mood.IDLE
                        },
                        face = if (h?.mood == PhoneHealth.Mood.DIZZY && !item.isWorking) TokkipEngine.Eye.SPIRAL else null,
                        wear = true,
                        jump = mascotTopBump,
                        interactive = !still,
                        greet = !still,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dashboard_health_eyebrow),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(h?.let { moodTitle(it.mood) } ?: R.string.dashboard_health_looking),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (h != null) {
                    Spacer(Modifier.width(8.dp))
                    ScoreRing(h.score)
                }
            }

            if (h != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(summary(h)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Stats(h)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = item.onClean,
                enabled = !item.isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    text = stringResource(if (item.isWorking) R.string.dashboard_health_working else R.string.dashboard_health_action),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun Stats(h: PhoneHealth) {
    val context = LocalContext.current
    val now = Instant.now()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.dashboard_health_stat_free_label),
            value = "${(h.freeRatio * 100).toInt()}%",
            note = stringResource(
                R.string.dashboard_health_stat_free_value,
                Formatter.formatShortFileSize(context, h.storageFree),
                Formatter.formatShortFileSize(context, h.storageTotal),
            ),
            level = h.disk,
            progress = 1f - h.freeRatio,
        )
        StatTile(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.dashboard_health_stat_clean_label),
            value = h.lastCleanAt?.let {
                if (Duration.between(it, now).toMinutes() < 1) stringResource(R.string.dashboard_health_stat_clean_now)
                else DateUtils.getRelativeTimeSpanString(it.toEpochMilli(), now.toEpochMilli(), DateUtils.MINUTE_IN_MILLIS).toString()
            } ?: stringResource(R.string.dashboard_health_stat_clean_never),
            note = null,
            level = h.clean,
        )
        StatTile(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.dashboard_health_stat_freed_label),
            value = Formatter.formatShortFileSize(context, h.totalFreed),
            note = null,
            level = PhoneHealth.Level.OK,
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier,
    label: String,
    value: String,
    note: String?,
    level: PhoneHealth.Level,
    progress: Float? = null,
) {
    val tone = levelColor(level)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = if (level == PhoneHealth.Level.OK) MaterialTheme.colorScheme.onSurface else tone, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (progress != null) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = tone,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
            if (note != null) {
                Spacer(Modifier.height(4.dp))
                Text(text = note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

@Composable
private fun levelColor(level: PhoneHealth.Level): Color = when (level) {
    PhoneHealth.Level.OK -> MaterialTheme.colorScheme.primary
    PhoneHealth.Level.WARN -> MaterialTheme.colorScheme.tertiary
    PhoneHealth.Level.BAD -> MaterialTheme.colorScheme.error
}

@Composable
private fun ScoreRing(score: Int) {
    val sweep by animateFloatAsState(targetValue = score / 100f, animationSpec = tween(900), label = "score")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val fill = levelColor(
        when {
            score >= 65 -> PhoneHealth.Level.OK
            score >= 45 -> PhoneHealth.Level.WARN
            else -> PhoneHealth.Level.BAD
        },
    )
    val desc = stringResource(R.string.dashboard_health_score_desc, score)
    Box(
        modifier = Modifier
            .size(64.dp)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(64.dp)) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(fill, -90f, 360f * sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(text = "$score", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

private fun moodTitle(m: PhoneHealth.Mood): Int = when (m) {
    PhoneHealth.Mood.HAPPY -> R.string.dashboard_health_mood_happy
    PhoneHealth.Mood.CALM -> R.string.dashboard_health_mood_calm
    PhoneHealth.Mood.TIRED -> R.string.dashboard_health_mood_tired
    PhoneHealth.Mood.DIZZY -> R.string.dashboard_health_mood_dizzy
}

/** Tokkip's sentence: the same reasoning as Healthy PC's home. */
private fun summary(h: PhoneHealth): Int = when {
    h.disk == PhoneHealth.Level.OK && h.clean == PhoneHealth.Level.OK -> R.string.dashboard_health_summary_ok
    h.disk == PhoneHealth.Level.BAD -> R.string.dashboard_health_summary_full
    h.clean != PhoneHealth.Level.OK -> R.string.dashboard_health_summary_clean
    else -> R.string.dashboard_health_summary_filling
}

@Preview2
@Composable
private fun TitleDashboardCardPreview() {
    PreviewWrapper {
        TitleDashboardCard(
            item = TitleDashboardCardItem(
                upgradeInfo = null,
                isWorking = false,
                onRibbonClicked = {},
                webpageTool = WebpageTool(LocalContext.current),
                onMascotTriggered = {},
                health = PhoneHealth.assess(128_000_000_000, 18_000_000_000, Instant.now().minus(Duration.ofDays(20)), 6_400_000_000, Instant.now()),
            ),
        )
    }
}
