package mx.loperstack.tokkiphealth.main.ui.dashboard.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
internal fun DashboardListCard(
    modifier: Modifier = Modifier,
    item: DashboardItem,
    mascotTopBump: Int = 0,
    order: Int = 0,
) {
    // Tools are bento tiles (BentoTiles.kt); everything else is a full-width card that pops in too.
    if (item.isTile()) {
        DashboardTile(item, order)
        return
    }
    if (item is HistoryDashboardCardItem) {
        HistoryDashboardCard(item, order)
        return
    }
    Appear(order) { FullCard(modifier, item, mascotTopBump) }
}

@Composable
private fun FullCard(modifier: Modifier, item: DashboardItem, mascotTopBump: Int) {
    when (item) {
        is HistoryDashboardCardItem -> Unit
        is TitleDashboardCardItem -> TitleDashboardCard(
            item = item,
            mascotTopBump = mascotTopBump,
        )
        is SetupDashboardCardItem -> SetupDashboardCard(modifier = modifier, item = item)
        is UpdateDashboardCardItem -> UpdateDashboardCard(item)
        is UpgradeDashboardCardItem -> UpgradeDashboardCard(item)
        is ToolDashboardCardItem -> ToolDashboardCard(modifier = modifier, item = item)
        is AppControlDashboardCardItem -> AppControlDashboardCard(item)
        is AnalyzerDashboardCardItem -> AnalyzerDashboardCard(item)
        is SqueezerDashboardCardItem -> SqueezerDashboardCard(item)
        is SchedulerDashboardCardItem -> SchedulerDashboardCard(item)
        is DebugRecorderDashboardCardItem -> DebugRecorderDashboardCard(item)
        is MotdDashboardCardItem -> MotdDashboardCard(item)
        is ReviewDashboardCardItem -> ReviewDashboardCard(item)
        is AnniversaryDashboardCardItem -> AnniversaryDashboardCard(item)
        is StatsDashboardCardItem -> StatsDashboardCard(item)
        is SwiperDashboardCardItem -> SwiperDashboardCard(modifier = modifier, item = item)
        is DebugDashboardCardItem -> DebugDashboardCard(item)
        is DryRunWarningDashboardCardItem -> DryRunWarningDashboardCard(item)
        is ErrorDataAreaDashboardCardItem -> ErrorDataAreaDashboardCard(item)
    }
}
