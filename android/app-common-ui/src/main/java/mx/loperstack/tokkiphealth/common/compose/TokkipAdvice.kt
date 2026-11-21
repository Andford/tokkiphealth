package mx.loperstack.tokkiphealth.common.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.loperstack.tokkip.character.Mood
import mx.loperstack.tokkip.character.Tokkip
import mx.loperstack.tokkiphealth.common.ui.R

/**
 * Tokkip's advice at the top of a tool's results, as in Healthy PC: he says in one or two
 * sentences what he found and what is safe to let go.
 */
@Composable
fun TokkipAdviceCard(
    text: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.tokkip_advice_title),
    mood: Mood = Mood.HAPPY,
) {
    val still = LocalInspectionMode.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(width = 72.dp, height = 60.dp), contentAlignment = Alignment.Center) {
                Tokkip(modifier = Modifier.height(60.dp), mood = mood, interactive = !still)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** How freely Tokkip says something can go, as Healthy PC's chips. */
enum class AdviceVerdict { SAFE, REVIEW, YOURS }

@Composable
fun AdviceVerdict.label(): String = stringResource(
    when (this) {
        AdviceVerdict.SAFE -> R.string.tokkip_advice_verdict_safe
        AdviceVerdict.REVIEW -> R.string.tokkip_advice_verdict_review
        AdviceVerdict.YOURS -> R.string.tokkip_advice_verdict_yours
    },
)

@Composable
fun AdviceVerdict.color() = when (this) {
    AdviceVerdict.SAFE -> MaterialTheme.colorScheme.primary
    AdviceVerdict.REVIEW -> MaterialTheme.colorScheme.tertiary
    AdviceVerdict.YOURS -> MaterialTheme.colorScheme.secondary
}
