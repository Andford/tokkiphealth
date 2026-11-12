package mx.loperstack.tokkiphealth.setup.tour

import mx.loperstack.tokkiphealth.R
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.compose.tour.GuidedTour
import mx.loperstack.tokkiphealth.common.compose.tour.TourDefinition
import mx.loperstack.tokkiphealth.common.compose.tour.TourId
import mx.loperstack.tokkiphealth.common.compose.tour.TourStep

object SetupTour : GuidedTour {

    override val id: TourId = TourId("tour.setup")

    fun definition(): TourDefinition = TourDefinition(
        id = id,
        clickProtection = true,
        steps = listOf(
            TourStep(
                stepId = "intro",
                targetId = null,
                body = R.string.tour_setup_intro_body.toCaString(),
            ),
        ),
    )
}
