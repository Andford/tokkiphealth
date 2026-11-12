package mx.loperstack.tokkiphealth.deduplicator.ui.details.tour

import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.compose.tour.GuidedTour
import mx.loperstack.tokkiphealth.common.compose.tour.TourDefinition
import mx.loperstack.tokkiphealth.common.compose.tour.TourId
import mx.loperstack.tokkiphealth.common.compose.tour.TourStep
import mx.loperstack.tokkiphealth.deduplicator.R

object DeduplicatorDetailsTour : GuidedTour {

    override val id: TourId = TourId("tour.deduplicator.details")

    const val CLUSTER_HEADER_TARGET = "deduplicator.details.clusterHeader"
    const val ROW_DELETE_MARK_TARGET = "deduplicator.details.rowDeleteMark"

    fun definition(
        prepareDeleteMark: (suspend () -> Unit)? = null,
    ): TourDefinition = TourDefinition(
        id = id,
        clickProtection = true,
        steps = listOf(
            TourStep(
                stepId = "cluster",
                targetId = CLUSTER_HEADER_TARGET,
                body = R.string.tour_deduplicator_details_cluster_body.toCaString(),
            ),
            TourStep(
                stepId = "deleteMark",
                targetId = ROW_DELETE_MARK_TARGET,
                body = R.string.tour_deduplicator_details_delete_mark_body.toCaString(),
                prepareTarget = prepareDeleteMark,
            ),
        ),
    )
}
