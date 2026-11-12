package mx.loperstack.tokkiphealth.swiper.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.HelpOutline
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.SwapHoriz
import androidx.compose.material.icons.twotone.Vibration
import mx.loperstack.tokkiphealth.common.compose.layout.SdmScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.loperstack.tokkiphealth.common.compose.layout.SdmTooltipIconButton
import mx.loperstack.tokkiphealth.common.compose.layout.SdmTopAppBar
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper
import mx.loperstack.tokkiphealth.common.compose.settings.SettingsSwitchItem
import mx.loperstack.tokkiphealth.common.error.ErrorEventHandler
import mx.loperstack.tokkiphealth.common.navigation.NavigationEventHandler
import mx.loperstack.tokkiphealth.swiper.R
import mx.loperstack.tokkiphealth.common.R as CommonR

@Composable
fun SwiperSettingsScreenHost(
    vm: SwiperSettingsViewModel = hiltViewModel(),
) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)
    val state by vm.state.collectAsStateWithLifecycle()

    SwiperSettingsScreen(
        state = state,
        onNavigateUp = vm::navUp,
        onSwapDirectionsChanged = vm::setSwapSwipeDirections,
        onShowDetailsChanged = vm::setShowFileDetailsOverlay,
        onHapticFeedbackChanged = vm::setHapticFeedbackEnabled,
        onHelp = vm::onHelpClick,
    )
}

@Composable
internal fun SwiperSettingsScreen(
    state: SwiperSettingsViewModel.State = SwiperSettingsViewModel.State(),
    onNavigateUp: () -> Unit = {},
    onSwapDirectionsChanged: (Boolean) -> Unit = {},
    onShowDetailsChanged: (Boolean) -> Unit = {},
    onHapticFeedbackChanged: (Boolean) -> Unit = {},
    onHelp: () -> Unit = {},
) {
    SdmScaffold(
        topBar = {
            SdmTopAppBar(
                title = stringResource(R.string.swiper_label),
                onNavigateUp = onNavigateUp,
                actions = {
                    SdmTooltipIconButton(
                        icon = Icons.AutoMirrored.TwoTone.HelpOutline,
                        label = stringResource(CommonR.string.general_help_action),
                        onClick = onHelp,
                    )
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            item {
                SettingsSwitchItem(
                    icon = Icons.TwoTone.SwapHoriz,
                    title = stringResource(R.string.swiper_settings_swap_directions_title),
                    subtitle = stringResource(R.string.swiper_settings_swap_directions_summary),
                    checked = state.swapSwipeDirections,
                    onCheckedChange = onSwapDirectionsChanged,
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.TwoTone.Info,
                    title = stringResource(R.string.swiper_settings_show_details_title),
                    subtitle = stringResource(R.string.swiper_settings_show_details_summary),
                    checked = state.showFileDetailsOverlay,
                    onCheckedChange = onShowDetailsChanged,
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.TwoTone.Vibration,
                    title = stringResource(R.string.swiper_settings_haptic_feedback_title),
                    subtitle = stringResource(R.string.swiper_settings_haptic_feedback_summary),
                    checked = state.hapticFeedbackEnabled,
                    onCheckedChange = onHapticFeedbackChanged,
                )
            }
        }
    }
}

@Preview2
@Composable
private fun SwiperSettingsScreenPreview() {
    PreviewWrapper {
        SwiperSettingsScreen(
            state = SwiperSettingsViewModel.State(
                swapSwipeDirections = false,
                showFileDetailsOverlay = true,
                hapticFeedbackEnabled = true,
            ),
        )
    }
}
