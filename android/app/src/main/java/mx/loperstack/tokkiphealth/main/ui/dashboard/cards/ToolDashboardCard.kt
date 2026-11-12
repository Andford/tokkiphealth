package mx.loperstack.tokkiphealth.main.ui.dashboard.cards

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.MenuBook
import androidx.compose.material.icons.twotone.Cancel
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Stars
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mx.loperstack.tokkiphealth.common.R as CommonR
import mx.loperstack.tokkiphealth.common.compose.asComposable
import mx.loperstack.tokkiphealth.common.compose.icons.FolderSearch
import mx.loperstack.tokkiphealth.common.compose.icons.SdmIcons
import mx.loperstack.tokkiphealth.common.compose.icons.icon
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper
import mx.loperstack.tokkiphealth.corpsefinder.core.tasks.CorpseFinderDeleteTask
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.main.core.SDMTool

import mx.loperstack.tokkiphealth.main.ui.dashboard.MainActionItem
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.common.DashboardActionIconSpacing
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.common.DashboardCard
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.common.DashboardFilledActionButton
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.common.DashboardIconActionButton
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.common.DashboardOutlinedActionButton
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.common.ProgressContainer

data class ToolDashboardCardItem(
    val toolType: SDMTool.Type,
    val isInitializing: Boolean,
    val result: SDMTool.Task.Result?,
    val progress: Progress.Data?,
    val showProRequirement: Boolean,
    val onScan: () -> Unit,
    val onDelete: (() -> Unit)?,
    val onViewTool: () -> Unit,
    val onViewDetails: () -> Unit,
    val onCancel: () -> Unit,
    /** Clears a frozen post-cleanup result; null while one is not on screen. See DashboardCardFlows. */
    val onDismissResult: (() -> Unit)?,
) : DashboardItem, MainActionItem {
    override val stableId: Long = toolType.hashCode().toLong()
}

@Composable
internal fun ToolDashboardCard(
    modifier: Modifier = Modifier,
    item: ToolDashboardCardItem,
) {
    val toolName = stringResource(toolNameRes(item.toolType))
    val toolDescription = stringResource(toolDescriptionRes(item.toolType))
    // The whole card opens the tool's live list. Gate on onDelete != null: after a clean the
    // result text persists but the live data is empty, so navigating would open a list that
    // immediately closes itself (flicker). onDelete goes null in that state, disabling the click.
    val clickable = item.progress == null && item.onDelete != null

    DashboardCard(
        modifier = modifier,
        onClick = item.onViewTool.takeIf { clickable },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = item.toolType.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = toolName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (item.isInitializing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 3.dp,
                )
            }
        }

        if (item.progress == null && item.result == null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = toolDescription,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (item.progress != null || item.result != null) {
            Spacer(modifier = Modifier.height(16.dp))
            ProgressContainer(
                modifier = Modifier.fillMaxWidth(),
                progress = item.progress,
                resultPrimary = item.result?.primaryInfo?.asComposable(),
                resultSecondary = item.result?.secondaryInfo?.asComposable()?.takeUnless { it.isBlank() },
                onDismissResult = item.onDismissResult,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.progress != null) {
                Spacer(modifier = Modifier.weight(1f))
                DashboardOutlinedActionButton(onClick = item.onCancel) {
                    Icon(
                        imageVector = Icons.TwoTone.Cancel,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(DashboardActionIconSpacing))
                    Text(text = stringResource(CommonR.string.general_cancel_action))
                }
            } else {
                if (item.onDelete != null) {
                    DashboardIconActionButton(onClick = item.onViewDetails) {
                        Icon(
                            imageVector = Icons.AutoMirrored.TwoTone.MenuBook,
                            contentDescription = stringResource(CommonR.string.general_show_details_action),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    DashboardFilledActionButton(
                        onClick = { item.onDelete.invoke() },
                        enabled = !item.isInitializing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Icon(
                            imageVector = if (item.showProRequirement) Icons.TwoTone.Stars else Icons.TwoTone.Delete,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(DashboardActionIconSpacing))
                        Text(text = stringResource(CommonR.string.general_delete_action))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    DashboardFilledActionButton(
                        onClick = item.onScan,
                        enabled = !item.isInitializing,
                    ) {
                        Icon(
                            imageVector = SdmIcons.FolderSearch,
                            contentDescription = stringResource(CommonR.string.general_scan_action),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    DashboardFilledActionButton(
                        onClick = item.onScan,
                        enabled = !item.isInitializing,
                    ) {
                        Icon(
                            imageVector = SdmIcons.FolderSearch,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(DashboardActionIconSpacing))
                        Text(text = stringResource(CommonR.string.general_scan_action))
                    }
                }
            }
        }
    }
}

@Preview2
@Composable
private fun ToolDashboardCardPreview() {
    PreviewWrapper {
        ToolDashboardCard(
            item = ToolDashboardCardItem(
                toolType = SDMTool.Type.CORPSEFINDER,
                isInitializing = false,
                result = null,
                progress = Progress.Data(),
                showProRequirement = false,
                onScan = {},
                onDelete = {},
                onViewTool = {},
                onViewDetails = {},
                onCancel = {},
                onDismissResult = null,
            ),
        )
    }
}

@Preview2
@Composable
private fun ToolDashboardCardDismissableResultPreview() {
    PreviewWrapper {
        ToolDashboardCard(
            item = ToolDashboardCardItem(
                toolType = SDMTool.Type.CORPSEFINDER,
                isInitializing = false,
                result = CorpseFinderDeleteTask.Success(
                    affectedSpace = 2_100_000_000L,
                    affectedPaths = emptySet(),
                ),
                progress = null,
                showProRequirement = false,
                onScan = {},
                onDelete = null,
                onViewTool = {},
                onViewDetails = {},
                onCancel = {},
                onDismissResult = {},
            ),
        )
    }
}
