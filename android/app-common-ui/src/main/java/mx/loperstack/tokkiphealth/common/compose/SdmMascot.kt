package mx.loperstack.tokkiphealth.common.compose

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import mx.loperstack.tokkip.character.Mood
import mx.loperstack.tokkip.character.Tokkip
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper

/** How Tokkip shows up. He dresses for the season himself (his own wardrobe). */
sealed interface SdmMascotMode {
    data object Animated : SdmMascotMode
    data object Halloween : SdmMascotMode
    data object Christmas : SdmMascotMode
    data object NewYear : SdmMascotMode
    data object Party : SdmMascotMode
}

/**
 * Tokkip, Tokkip Health's companion, where SD Maid had its mascot. Drawn in code (the same
 * character as Tokkip and Healthy PC), about 1.2 : 1; give it a height or a width.
 */
@Composable
fun SdmMascot(
    modifier: Modifier = Modifier,
    mode: SdmMascotMode = SdmMascotMode.Animated,
) {
    val still = LocalInspectionMode.current
    Tokkip(
        modifier = modifier.aspectRatio(1.2f),
        mood = if (mode == SdmMascotMode.Party) Mood.HAPPY else Mood.IDLE,
        dancing = mode == SdmMascotMode.Party && !still,
        interactive = !still,
    )
}

@Preview2
@Composable
private fun SdmMascotPreview() {
    PreviewWrapper {
        SdmMascot()
    }
}
