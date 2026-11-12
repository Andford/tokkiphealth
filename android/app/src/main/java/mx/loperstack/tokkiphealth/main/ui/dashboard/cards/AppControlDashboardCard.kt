package mx.loperstack.tokkiphealth.main.ui.dashboard.cards

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Apps
import androidx.compose.material.icons.twotone.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mx.loperstack.tokkiphealth.appcontrol.R as AppControlR
import mx.loperstack.tokkiphealth.common.R as CommonR
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper
import mx.loperstack.tokkiphealth.appcontrol.core.AppControl
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.common.DashboardActionIconSpacing
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.common.DashboardCard
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.common.DashboardFlatActionButton
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.common.SimpleToolCardHeader


data class AppControlDashboardCardItem(
    val data: AppControl.Data?,
    val isInitializing: Boolean,
    val progress: Progress.Data?,
    val onViewDetails: () -> Unit,
) : DashboardItem {
    override val stableId: Long = this.javaClass.hashCode().toLong()
}

@Composable
internal fun AppControlDashboardCard(item: AppControlDashboardCardItem) {
    DashboardCard(onClick = item.onViewDetails.takeIf { !item.isInitializing }) {
        SimpleToolCardHeader(
            icon = Icons.TwoTone.Apps,
            title = stringResource(CommonR.string.appcontrol_tool_name),
            subtitle = stringResource(AppControlR.string.appcontrol_explanation_short),
            isInitializing = item.isInitializing,
        )
        Spacer(modifier = Modifier.height(8.dp))
        DashboardFlatActionButton(
            onClick = item.onViewDetails,
            enabled = !item.isInitializing,
            modifier = Modifier.align(Alignment.End),
        ) {
            Icon(
                imageVector = Icons.TwoTone.Visibility,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(DashboardActionIconSpacing))
            Text(text = stringResource(CommonR.string.general_open_action))
        }
    }
}

@Preview2
@Composable
private fun AppControlDashboardCardPreview() {
    PreviewWrapper {
        AppControlDashboardCard(
            item = AppControlDashboardCardItem(
                data = null,
                isInitializing = false,
                progress = null,
                onViewDetails = {},
            ),
        )
    }
}
