package mx.loperstack.tokkiphealth.common.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material3.MaterialTheme
import mx.loperstack.tokkiphealth.common.compose.layout.SdmScaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mx.loperstack.tokkiphealth.common.R as CommonR
import mx.loperstack.tokkiphealth.common.ui.R
import mx.loperstack.tokkiphealth.common.compose.layout.SdmTooltipIconButton
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper

/**
 * Fallback shown when the NavDisplay's entryProvider receives a route that no [NavigationEntry]
 * has registered. Without it NavDisplay throws `IllegalStateException: Unknown screen …` and kills
 * the app, so this guards against navigating to a route that was added without a matching entry.
 */
@Composable
fun UnknownDestinationScreen(
    routeLabel: String,
    onNavigateUp: () -> Unit,
) {
    SdmScaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.navigation_unknown_destination_title)) },
                navigationIcon = {
                    SdmTooltipIconButton(
                        icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                        label = stringResource(CommonR.string.general_navigate_up_action),
                        onClick = onNavigateUp,
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.navigation_unknown_destination_message),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = routeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Preview2
@Composable
private fun UnknownDestinationScreenPreview() {
    PreviewWrapper {
        UnknownDestinationScreen(
            routeLabel = "CustomFilterListRoute",
            onNavigateUp = {},
        )
    }
}
