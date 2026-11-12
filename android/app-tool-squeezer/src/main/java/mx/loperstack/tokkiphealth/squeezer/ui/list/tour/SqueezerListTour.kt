package mx.loperstack.tokkiphealth.squeezer.ui.list.tour

import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.compose.tour.GuidedTour
import mx.loperstack.tokkiphealth.common.compose.tour.TourDefinition
import mx.loperstack.tokkiphealth.common.compose.tour.TourId
import mx.loperstack.tokkiphealth.common.compose.tour.TourStep
import mx.loperstack.tokkiphealth.squeezer.R

object SqueezerListTour : GuidedTour {

    override val id: TourId = TourId("tour.squeezer.list")

    const val COMPRESS_ALL_TARGET = "squeezer.compressAll"

    /**
     * A centerless intro frames the (non-obvious, lossy) "re-compress media to save space" idea,
     * then the compress-all FAB is anchored. The per-item tap action is described in copy rather
     * than anchored, to avoid threading targets through both the grid and linear layouts; the
     * quality slider lives in a modal dialog and is out of scope for anchoring.
     */
    fun definition(): TourDefinition = TourDefinition(
        id = id,
        clickProtection = true,
        steps = listOf(
            TourStep(
                stepId = "intro",
                targetId = null,
                body = R.string.tour_squeezer_intro_body.toCaString(),
            ),
            TourStep(
                stepId = "compress",
                targetId = COMPRESS_ALL_TARGET,
                body = R.string.tour_squeezer_compress_body.toCaString(),
            ),
        ),
    )
}
