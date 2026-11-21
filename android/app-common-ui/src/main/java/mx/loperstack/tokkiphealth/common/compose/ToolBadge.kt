package mx.loperstack.tokkiphealth.common.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.loperstack.tokkip.character.MShapes
import mx.loperstack.tokkip.character.PolygonShape

/**
 * A tool's icon on one of Tokkip's Expressive shapes (cookie, clover, sunny…), as Healthy PC and
 * Tokkip PC show their sections. Each [index] gets its own shape and container colour.
 */
@Composable
fun ToolBadge(
    icon: ImageVector,
    index: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val shape = remember(index) { PolygonShape(MShapes.ALL[Math.floorMod(index, MShapes.ALL.size)]) }
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (Math.floorMod(index, 3)) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(size * 0.5f))
    }
}
