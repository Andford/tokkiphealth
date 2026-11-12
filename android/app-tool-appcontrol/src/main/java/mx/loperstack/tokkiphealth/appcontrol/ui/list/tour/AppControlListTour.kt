package mx.loperstack.tokkiphealth.appcontrol.ui.list.tour

import mx.loperstack.tokkiphealth.appcontrol.R
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.compose.tour.GuidedTour
import mx.loperstack.tokkiphealth.common.compose.tour.TourDefinition
import mx.loperstack.tokkiphealth.common.compose.tour.TourId
import mx.loperstack.tokkiphealth.common.compose.tour.TourStep

object AppControlListTour : GuidedTour {

    override val id: TourId = TourId("tour.appcontrol.list")

    const val SEARCH_TARGET = "appcontrol.search"
    const val FILTER_TARGET = "appcontrol.filter"
    const val SORT_TARGET = "appcontrol.sort"
    const val APP_ROW_TARGET = "appcontrol.appRow"

    fun definition(): TourDefinition = TourDefinition(
        id = id,
        clickProtection = true,
        steps = listOf(
            TourStep(
                stepId = "search",
                targetId = SEARCH_TARGET,
                body = R.string.tour_appcontrol_search_body.toCaString(),
            ),
            TourStep(
                stepId = "filter",
                targetId = FILTER_TARGET,
                body = R.string.tour_appcontrol_filter_body.toCaString(),
            ),
            TourStep(
                stepId = "sort",
                targetId = SORT_TARGET,
                body = R.string.tour_appcontrol_sort_body.toCaString(),
            ),
            TourStep(
                stepId = "appRow",
                targetId = APP_ROW_TARGET,
                body = R.string.tour_appcontrol_app_row_body.toCaString(),
            ),
        ),
    )
}
