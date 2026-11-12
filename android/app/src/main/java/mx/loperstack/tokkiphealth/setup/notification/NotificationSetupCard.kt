package mx.loperstack.tokkiphealth.setup.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mx.loperstack.tokkiphealth.R
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper
import mx.loperstack.tokkiphealth.common.permissions.Permission
import mx.loperstack.tokkiphealth.setup.SetupCardContainer
import mx.loperstack.tokkiphealth.setup.SetupCardItem
import mx.loperstack.tokkiphealth.common.R as CommonR

data class NotificationSetupCardItem(
    override val state: NotificationSetupModule.Result,
    val onGrantAction: () -> Unit,
    val onHelp: () -> Unit,
) : SetupCardItem

@Composable
internal fun NotificationSetupCard(
    item: NotificationSetupCardItem,
    modifier: Modifier = Modifier,
) {
    SetupCardContainer(
        icon = Icons.TwoTone.Notifications,
        title = stringResource(R.string.setup_notification_title),
        modifier = modifier,
        onHelp = item.onHelp,
    ) {
        Text(
            text = stringResource(R.string.setup_notification_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        if (item.state.missingPermission.isEmpty()) {
            GrantStateLabel(
                text = stringResource(R.string.setup_permission_granted_label),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        if (!item.state.isComplete) {
            Button(
                onClick = item.onGrantAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(stringResource(CommonR.string.general_grant_access_action))
            }
        }
        Text(
            text = stringResource(R.string.setup_notification_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}

@Composable
internal fun GrantStateLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.TwoTone.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview2
@Composable
private fun NotificationSetupCardPreview() {
    PreviewWrapper {
        NotificationSetupCard(
            item = NotificationSetupCardItem(
                state = NotificationSetupModule.Result(
                    missingPermission = setOf(Permission.POST_NOTIFICATIONS),
                ),
                onGrantAction = {},
                onHelp = {},
            ),
        )
    }
}
