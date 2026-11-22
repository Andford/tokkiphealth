package mx.loperstack.tokkiphealth.main.ui.dashboard.cards

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Alarm
import androidx.compose.material.icons.twotone.Apps
import androidx.compose.material.icons.twotone.Cancel
import androidx.compose.material.icons.twotone.Compress
import androidx.compose.material.icons.twotone.DataUsage
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material.icons.twotone.Swipe
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mx.loperstack.tokkiphealth.R
import mx.loperstack.tokkiphealth.common.compose.ToolBadge
import mx.loperstack.tokkiphealth.common.compose.icons.icon
import mx.loperstack.tokkiphealth.common.compose.asComposable
import mx.loperstack.tokkiphealth.main.core.SDMTool
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import mx.loperstack.tokkiphealth.analyzer.R as AnalyzerR
import mx.loperstack.tokkiphealth.appcontrol.R as AppControlR
import mx.loperstack.tokkiphealth.common.R as CommonR
import mx.loperstack.tokkiphealth.scheduler.R as SchedulerR
import mx.loperstack.tokkiphealth.squeezer.R as SqueezerR
import mx.loperstack.tokkiphealth.swiper.R as SwiperR

/*
 * Tokkip Health's home is a bento of tiles, as Healthy PC's: each tool is a square with its
 * Expressive shape, and every tile arrives with Tokkip's bouncy spring, squashes when pressed and
 * counts its numbers up.
 */

/** Tokkip's springs: a little bounce, never stiff. */
internal val TokkipBounce = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

/** A tile that pops in (one after the other, by [order]) and squashes under the finger. */
@Composable
internal fun BentoTile(
    order: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable ColumnScope.() -> Unit,
) {
    val still = LocalInspectionMode.current
    val appear = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (appear.value < 1f) {
            delay(60L * order.coerceIn(0, 10))
            appear.animateTo(1f, TokkipBounce)
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val squash by animateFloatAsState(if (pressed) 0.95f else 1f, TokkipBounce, label = "squash")
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(6.dp)
            .graphicsLayer {
                val s = (0.85f + 0.15f * appear.value) * squash
                scaleX = s
                scaleY = s
                alpha = appear.value.coerceIn(0f, 1f)
                translationY = (1f - appear.value) * 40f
            }
            .clip(MaterialTheme.shapes.extraLarge)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
    ) {
        Column(modifier = Modifier.padding(16.dp).heightIn(min = 132.dp), content = content)
    }
}

/** A cleaning tool as a tile: what it found (or freed), and one round button to scan, delete or stop. */
@Composable
internal fun ToolTile(item: ToolDashboardCardItem, order: Int) {
    val name = stringResource(toolNameRes(item.toolType))
    val primary = item.result?.primaryInfo?.asComposable()
    val canOpen = item.progress == null && item.onDelete != null
    BentoTile(order = order, onClick = if (canOpen) item.onViewDetails else item.onViewTool) {
        Row(verticalAlignment = Alignment.Top) {
            ToolBadge(icon = item.toolType.icon, index = item.toolType.ordinal)
            Spacer(Modifier.weight(1f))
            val (icon, onClick, danger) = when {
                item.progress != null -> Triple(Icons.TwoTone.Cancel, item.onCancel, false)
                item.onDelete != null -> Triple(Icons.TwoTone.Delete, item.onDelete, true)
                else -> Triple(Icons.TwoTone.Search, item.onScan, false)
            }
            FilledIconButton(
                onClick = onClick,
                enabled = !item.isInitializing,
                colors = if (danger) {
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    IconButtonDefaults.filledIconButtonColors()
                },
            ) {
                AnimatedContent(targetState = icon, transitionSpec = { (scaleIn(TokkipBounce) + fadeIn()) togetherWith fadeOut() }, label = "action") {
                    Icon(imageVector = it, contentDescription = stringResource(if (it == Icons.TwoTone.Delete) CommonR.string.general_delete_action else if (it == Icons.TwoTone.Cancel) CommonR.string.general_cancel_action else CommonR.string.general_scan_action))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        when {
            item.progress != null -> {
                Text(
                    text = item.progress.primary.asComposable(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(CircleShape))
            }
            primary != null -> AnimatedContent(targetState = primary, transitionSpec = { (scaleIn(TokkipBounce) + fadeIn()) togetherWith fadeOut() }, label = "result") {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            else -> Text(
                text = stringResource(toolDescriptionRes(item.toolType)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A tool that opens a screen of its own (storage, apps, compress, swipe, schedule). */
@Composable
internal fun LauncherTile(
    order: Int,
    icon: ImageVector,
    badge: Int,
    title: String,
    subtitle: String,
    busy: Boolean,
    onClick: () -> Unit,
) {
    BentoTile(order = order, onClick = onClick) {
        ToolBadge(icon = icon, index = badge)
        Spacer(Modifier.height(12.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (busy) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(CircleShape))
        }
    }
}

/** Which dashboard items are tiles (half a row) instead of full-width cards. */
internal fun DashboardItem.isTile(): Boolean = this is ToolDashboardCardItem ||
    this is AnalyzerDashboardCardItem ||
    this is AppControlDashboardCardItem ||
    this is SqueezerDashboardCardItem ||
    this is SwiperDashboardCardItem ||
    this is SchedulerDashboardCardItem

@Composable
internal fun DashboardTile(item: DashboardItem, order: Int) {
    when (item) {
        is ToolDashboardCardItem -> ToolTile(item, order)
        is AnalyzerDashboardCardItem -> LauncherTile(
            order, Icons.TwoTone.DataUsage, 4, stringResource(CommonR.string.analyzer_tool_name),
            stringResource(AnalyzerR.string.analyzer_explanation_short), item.progress != null, item.onViewDetails,
        )
        is AppControlDashboardCardItem -> LauncherTile(
            order, Icons.TwoTone.Apps, 5, stringResource(CommonR.string.appcontrol_tool_name),
            item.data?.apps?.size?.let { stringResource(R.string.dashboard_tile_apps_count, it) }
                ?: stringResource(AppControlR.string.appcontrol_explanation_short),
            item.progress != null, item.onViewDetails,
        )
        is SqueezerDashboardCardItem -> LauncherTile(
            order, Icons.TwoTone.Compress, 6, stringResource(CommonR.string.squeezer_tool_name),
            stringResource(SqueezerR.string.squeezer_explanation_short), item.progress != null, item.onViewDetails,
        )
        is SwiperDashboardCardItem -> LauncherTile(
            order, Icons.TwoTone.Swipe, 7, stringResource(SwiperR.string.swiper_label),
            stringResource(SwiperR.string.swiper_dashcard_description), item.progress != null, item.onViewDetails,
        )
        is SchedulerDashboardCardItem -> LauncherTile(
            order, Icons.TwoTone.Alarm, 8, stringResource(SchedulerR.string.scheduler_label),
            stringResource(R.string.dashboard_tile_scheduler_subtitle), false, item.onManageClicked,
        )
        else -> Unit
    }
}

// ── Cleaning history ──────────────────────────────────────────────────────

/** One clean: which tool, how much, when. */
data class CleanEntry(val tool: SDMTool.Type, val bytes: Long, val at: Instant)

/** Tokkip's cleaning history: freed per day over two weeks, and the last cleans. */
data class HistoryDashboardCardItem(
    val perDay: List<Long>,
    val recent: List<CleanEntry>,
    val totalFreed: Long,
    val cleans: Int,
    val onOpen: () -> Unit,
) : DashboardItem {
    override val stableId: Long = this.javaClass.hashCode().toLong()

    companion object {
        const val DAYS = 14

        /** Pure, for the tests: the reports that freed something, grouped by local day. */
        fun build(cleans: List<CleanEntry>, totalFreed: Long, today: LocalDate, zone: ZoneId, onOpen: () -> Unit): HistoryDashboardCardItem {
            val perDay = LongArray(DAYS)
            for (c in cleans) {
                val day = c.at.atZone(zone).toLocalDate()
                val ago = java.time.temporal.ChronoUnit.DAYS.between(day, today).toInt()
                if (ago in 0 until DAYS) perDay[DAYS - 1 - ago] += c.bytes
            }
            return HistoryDashboardCardItem(
                perDay = perDay.toList(),
                recent = cleans.sortedByDescending { it.at }.take(3),
                totalFreed = totalFreed,
                cleans = cleans.size,
                onOpen = onOpen,
            )
        }
    }
}

@Composable
internal fun HistoryDashboardCard(item: HistoryDashboardCardItem, order: Int) {
    val context = LocalContext.current
    val still = LocalInspectionMode.current
    val grow = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(item.perDay) {
        grow.snapTo(0f)
        grow.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow))
    }
    val counted by animateFloatAsState(item.totalFreed.toFloat(), spring(stiffness = Spring.StiffnessVeryLow), label = "freed")
    BentoTile(order = order, onClick = item.onOpen, container = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = stringResource(R.string.dashboard_history_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = Formatter.formatShortFileSize(context, counted.toLong()),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = pluralStringResource(R.plurals.dashboard_history_freed_in_x, item.cleans, item.cleans),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        val bar = MaterialTheme.colorScheme.primary
        val today = MaterialTheme.colorScheme.tertiary
        val track = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.10f)
        val max = (item.perDay.maxOrNull() ?: 0L).coerceAtLeast(1L)
        Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            val n = item.perDay.size
            val gap = 6.dp.toPx()
            val w = (size.width - gap * (n - 1)) / n
            val r = CornerRadius(w / 2, w / 2)
            item.perDay.forEachIndexed { i, v ->
                val x = i * (w + gap)
                drawRoundRect(track, Offset(x, 0f), Size(w, size.height), r)
                // Each bar grows a moment after the one before it.
                val local = ((grow.value * (n + 4) - i) / 5f).coerceIn(0f, 1.2f)
                val h = if (v > 0) (size.height * (v.toFloat() / max)).coerceAtLeast(w) * local.coerceAtMost(1.08f) else 0f
                if (h > 0f) drawRoundRect(if (i == n - 1) today else bar, Offset(x, size.height - h.coerceAtMost(size.height)), Size(w, h.coerceAtMost(size.height)), r)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dashboard_history_two_weeks), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.dashboard_history_today), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        if (item.recent.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.dashboard_history_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        } else {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val now = System.currentTimeMillis()
                item.recent.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerLowest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(c.tool.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(toolNameOrNull(c.tool) ?: CommonR.string.app_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${Formatter.formatShortFileSize(context, c.bytes)} · ${DateUtils.getRelativeTimeSpanString(c.at.toEpochMilli(), now, DateUtils.MINUTE_IN_MILLIS)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

private fun toolNameOrNull(type: SDMTool.Type): Int? = when (type) {
    SDMTool.Type.CORPSEFINDER, SDMTool.Type.SYSTEMCLEANER, SDMTool.Type.APPCLEANER, SDMTool.Type.DEDUPLICATOR -> toolNameRes(type)
    SDMTool.Type.SQUEEZER -> CommonR.string.squeezer_tool_name
    SDMTool.Type.SWIPER -> CommonR.string.swiper_tool_name
    SDMTool.Type.APPCONTROL -> CommonR.string.appcontrol_tool_name
    SDMTool.Type.ANALYZER -> CommonR.string.analyzer_tool_name
    else -> null
}

/** Full-width cards arrive the same way as the tiles. */
@Composable
internal fun Appear(order: Int, content: @Composable () -> Unit) {
    val still = LocalInspectionMode.current
    val appear = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (appear.value < 1f) {
            delay(60L * order.coerceIn(0, 10))
            appear.animateTo(1f, TokkipBounce)
        }
    }
    Box(
        modifier = Modifier.graphicsLayer {
            val s = 0.92f + 0.08f * appear.value
            scaleX = s
            scaleY = s
            alpha = appear.value.coerceIn(0f, 1f)
            translationY = (1f - appear.value) * 40f
        },
    ) { content() }
}
