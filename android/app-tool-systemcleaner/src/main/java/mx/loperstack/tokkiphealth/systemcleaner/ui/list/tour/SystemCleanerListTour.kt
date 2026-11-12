package mx.loperstack.tokkiphealth.systemcleaner.ui.list.tour

import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.compose.tour.GuidedTour
import mx.loperstack.tokkiphealth.common.compose.tour.TourDefinition
import mx.loperstack.tokkiphealth.common.compose.tour.TourId
import mx.loperstack.tokkiphealth.common.compose.tour.TourStep
import mx.loperstack.tokkiphealth.systemcleaner.R

object SystemCleanerListTour : GuidedTour {

    override val id: TourId = TourId("tour.systemcleaner.list")

    const val FILTER_ROW_TARGET = "systemcleaner.filterRow"

    /**
     * A centerless intro explains the non-obvious filter concept (ready-made rules), then the first
     * filter row is anchored to teach the "open details to see matching files" affordance. A
     * populated screen always has at least one row, so the anchored step won't grace-skip.
     */
    fun definition(): TourDefinition = TourDefinition(
        id = id,
        clickProtection = true,
        steps = listOf(
            TourStep(
                stepId = "intro",
                targetId = null,
                body = R.string.tour_systemcleaner_intro_body.toCaString(),
            ),
            TourStep(
                stepId = "filter",
                targetId = FILTER_ROW_TARGET,
                body = R.string.tour_systemcleaner_filter_body.toCaString(),
            ),
        ),
    )
}
