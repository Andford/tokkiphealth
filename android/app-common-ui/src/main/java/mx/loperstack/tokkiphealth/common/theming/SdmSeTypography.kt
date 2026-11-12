package mx.loperstack.tokkiphealth.common.theming

import android.graphics.Typeface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Tokkip's type, as in the Tokkip app: Google Sans, the Pixel's own typeface, when the phone has
 * it (every Pixel does); elsewhere the system font. A touch heavier than Material's defaults.
 */
private fun pixelFont(): Typeface {
    for (family in listOf("google-sans-flex", "google-sans", "google-sans-text")) {
        val base = Typeface.create(family, Typeface.NORMAL)
        if (base != Typeface.DEFAULT) return base
    }
    return Typeface.DEFAULT
}

private val PixelSans = FontFamily(pixelFont())

val SdmSeTypography = Typography().let { t ->
    fun TextStyle.p(w: Int) = copy(fontFamily = PixelSans, fontWeight = FontWeight(w))
    t.copy(
        displayLarge = t.displayLarge.p(700), displayMedium = t.displayMedium.p(700), displaySmall = t.displaySmall.p(700),
        headlineLarge = t.headlineLarge.p(700), headlineMedium = t.headlineMedium.p(700), headlineSmall = t.headlineSmall.p(700),
        titleLarge = t.titleLarge.p(700), titleMedium = t.titleMedium.p(600), titleSmall = t.titleSmall.p(600),
        bodyLarge = t.bodyLarge.p(500), bodyMedium = t.bodyMedium.p(500), bodySmall = t.bodySmall.p(500),
        labelLarge = t.labelLarge.p(600), labelMedium = t.labelMedium.p(600), labelSmall = t.labelSmall.p(600),
    )
}

/** Material 3 Expressive: generous, friendly corners, as in Tokkip. */
val TokkipShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
