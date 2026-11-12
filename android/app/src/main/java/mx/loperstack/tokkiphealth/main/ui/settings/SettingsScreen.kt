package mx.loperstack.tokkiphealth.main.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.automirrored.twotone.ViewList
import androidx.compose.material.icons.twotone.Apps
import androidx.compose.material.icons.twotone.Compress
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.DataUsage
import androidx.compose.material.icons.twotone.Recycling
import androidx.compose.material.icons.twotone.Swipe
import androidx.compose.material.icons.automirrored.twotone.ContactSupport
import androidx.compose.material.icons.twotone.Alarm
import androidx.compose.material.icons.twotone.BarChart
import androidx.compose.material.icons.twotone.Favorite
import androidx.compose.material.icons.twotone.PhoneAndroid
import androidx.compose.material.icons.twotone.Policy
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.SettingsBackupRestore
import androidx.compose.material.icons.twotone.Shield
import androidx.compose.material.icons.twotone.Stars
import androidx.compose.material.icons.twotone.Update
import androidx.compose.material3.MaterialTheme
import mx.loperstack.tokkiphealth.common.compose.layout.SdmScaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.loperstack.tokkiphealth.R
import mx.loperstack.tokkiphealth.common.BuildConfigWrap
import mx.loperstack.tokkiphealth.common.R as CommonR
import mx.loperstack.tokkiphealth.common.compose.icons.Ghost
import mx.loperstack.tokkiphealth.common.compose.layout.SdmTooltipIconButton
import mx.loperstack.tokkiphealth.common.compose.icons.SdmIcons
import mx.loperstack.tokkiphealth.common.compose.settings.SettingsCategoryHeader
import mx.loperstack.tokkiphealth.common.compose.settings.SettingsPreferenceItem
import mx.loperstack.tokkiphealth.common.error.ErrorEventHandler
import mx.loperstack.tokkiphealth.common.navigation.NavigationEventHandler
import mx.loperstack.tokkiphealth.common.upgrade.ui.brandTitleText
import mx.loperstack.tokkiphealth.analyzer.ui.AnalyzerSettingsRoute
import mx.loperstack.tokkiphealth.appcleaner.ui.AppCleanerSettingsRoute
import mx.loperstack.tokkiphealth.appcontrol.ui.AppControlSettingsRoute
import mx.loperstack.tokkiphealth.corpsefinder.ui.CorpseFinderSettingsRoute
import mx.loperstack.tokkiphealth.deduplicator.ui.DeduplicatorSettingsRoute
import mx.loperstack.tokkiphealth.exclusion.ui.ExclusionsListRoute
import mx.loperstack.tokkiphealth.main.ui.navigation.AcknowledgementsRoute
import mx.loperstack.tokkiphealth.main.ui.navigation.BackupRestoreRoute
import mx.loperstack.tokkiphealth.common.navigation.routes.UpgradeRoute
import mx.loperstack.tokkiphealth.main.ui.navigation.GeneralSettingsRoute
import mx.loperstack.tokkiphealth.main.ui.navigation.SupportRoute
import mx.loperstack.tokkiphealth.scheduler.ui.SchedulerSettingsRoute
import mx.loperstack.tokkiphealth.setup.SetupRoute
import mx.loperstack.tokkiphealth.squeezer.ui.SqueezerSettingsRoute
import mx.loperstack.tokkiphealth.stats.ui.StatsSettingsRoute
import mx.loperstack.tokkiphealth.swiper.ui.SwiperSettingsRoute
import mx.loperstack.tokkiphealth.systemcleaner.ui.SystemCleanerSettingsRoute
import mx.loperstack.tokkiphealth.setup.SetupScreenOptions

@Composable
fun SettingsScreenHost(
    vm: SettingsViewModel = hiltViewModel(),
) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is SettingEvents.ShowVersionInfo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.info,
                        actionLabel = context.getString(CommonR.string.general_copy_action),
                        duration = SnackbarDuration.Indefinite,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        vm.copyVersionInfos()
                    }
                }
            }
        }
    }

    SettingsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateUp = vm::navUp,
        onExclusionsClick = { vm.navTo(ExclusionsListRoute) },
        onGeneralSettingsClick = { vm.navTo(GeneralSettingsRoute) },
        onSetupClick = { vm.navTo(SetupRoute(options = SetupScreenOptions(showCompleted = true))) },
        onSupportClick = { vm.navTo(SupportRoute) },
        onAcknowledgementsClick = { vm.navTo(AcknowledgementsRoute) },
        onPrivacyClick = vm::openPrivacyPolicy,
        onChangelogClick = { vm.openWebsite("https://sdmse.darken.eu/changelog") },
        onChangelogLongClick = vm::showVersionInfos,
        onToolSettingsClick = { tool ->
            val route = when (tool) {
                "CorpseFinder" -> CorpseFinderSettingsRoute
                "SystemCleaner" -> SystemCleanerSettingsRoute
                "AppCleaner" -> AppCleanerSettingsRoute
                "Deduplicator" -> DeduplicatorSettingsRoute
                "AppControl" -> AppControlSettingsRoute
                "Squeezer" -> SqueezerSettingsRoute
                "Swiper" -> SwiperSettingsRoute
                "Analyzer" -> AnalyzerSettingsRoute
                else -> return@SettingsScreen
            }
            vm.navTo(route)
        },
        onStatsClick = { vm.navTo(StatsSettingsRoute) },
        onSchedulerClick = { vm.navTo(SchedulerSettingsRoute) },
        onBackupRestoreClick = { vm.navTo(BackupRestoreRoute) },
        onProClick = { vm.navTo(UpgradeRoute(manage = true)) },
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsViewModel.State = SettingsViewModel.State(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateUp: () -> Unit = {},
    onExclusionsClick: () -> Unit = {},
    onGeneralSettingsClick: () -> Unit = {},
    onSetupClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onAcknowledgementsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onChangelogClick: () -> Unit = {},
    onChangelogLongClick: () -> Unit = {},
    onToolSettingsClick: (String) -> Unit = {},
    onStatsClick: () -> Unit = {},
    onSchedulerClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = {},
    onProClick: () -> Unit = {},
) {
    SdmScaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(CommonR.string.general_settings_title)) },
                navigationIcon = {
                    SdmTooltipIconButton(
                        icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                        label = stringResource(CommonR.string.general_navigate_up_action),
                        onClick = onNavigateUp,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            // Tools category
            item { SettingsCategoryHeader(text = stringResource(CommonR.string.settings_category_tools_label)) }

            item {
                SettingsPreferenceItem(
                    icon = SdmIcons.Ghost,
                    title = stringResource(CommonR.string.corpsefinder_tool_name),
                    subtitle = stringResource(mx.loperstack.tokkiphealth.corpsefinder.R.string.corpsefinder_settings_summary),
                    onClick = { onToolSettingsClick("CorpseFinder") },
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.AutoMirrored.TwoTone.ViewList,
                    title = stringResource(CommonR.string.systemcleaner_tool_name),
                    subtitle = stringResource(mx.loperstack.tokkiphealth.systemcleaner.R.string.systemcleaner_explanation_short),
                    onClick = { onToolSettingsClick("SystemCleaner") },
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.Recycling,
                    title = stringResource(CommonR.string.appcleaner_tool_name),
                    subtitle = stringResource(mx.loperstack.tokkiphealth.appcleaner.R.string.appcleaner_explanation_short),
                    onClick = { onToolSettingsClick("AppCleaner") },
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.ContentCopy,
                    title = stringResource(CommonR.string.deduplicator_tool_name),
                    subtitle = stringResource(mx.loperstack.tokkiphealth.deduplicator.R.string.deduplicator_explanation_short),
                    onClick = { onToolSettingsClick("Deduplicator") },
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.Apps,
                    title = stringResource(CommonR.string.appcontrol_tool_name),
                    subtitle = stringResource(mx.loperstack.tokkiphealth.appcontrol.R.string.appcontrol_explanation_short),
                    onClick = { onToolSettingsClick("AppControl") },
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.Compress,
                    title = stringResource(CommonR.string.squeezer_tool_name),
                    subtitle = stringResource(mx.loperstack.tokkiphealth.squeezer.R.string.squeezer_explanation_short),
                    onClick = { onToolSettingsClick("Squeezer") },
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.Swipe,
                    title = stringResource(CommonR.string.swiper_tool_name),
                    subtitle = stringResource(mx.loperstack.tokkiphealth.swiper.R.string.swiper_tool_description),
                    onClick = { onToolSettingsClick("Swiper") },
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.DataUsage,
                    title = stringResource(CommonR.string.analyzer_tool_name),
                    subtitle = stringResource(mx.loperstack.tokkiphealth.analyzer.R.string.analyzer_explanation_short),
                    onClick = { onToolSettingsClick("Analyzer") },
                )
            }

            // Device category
            item { SettingsCategoryHeader(text = stringResource(CommonR.string.general_device_label)) }

            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.Settings,
                    title = stringResource(R.string.general_settings_label),
                    subtitle = stringResource(R.string.general_settings_desc),
                    onClick = onGeneralSettingsClick,
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.Shield,
                    title = stringResource(mx.loperstack.tokkiphealth.common.exclusion.R.string.exclusion_manager_title),
                    subtitle = stringResource(mx.loperstack.tokkiphealth.common.exclusion.R.string.exclusion_manager_desc),
                    onClick = onExclusionsClick,
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.BarChart,
                    title = stringResource(CommonR.string.stats_label),
                    subtitle = stringResource(mx.loperstack.tokkiphealth.common.stats.R.string.stats_settings_desc),
                    onClick = onStatsClick,
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.Alarm,
                    title = stringResource(mx.loperstack.tokkiphealth.scheduler.R.string.scheduler_settings_label),
                    subtitle = stringResource(mx.loperstack.tokkiphealth.scheduler.R.string.scheduler_settings_summary),
                    onClick = onSchedulerClick,
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.SettingsBackupRestore,
                    title = stringResource(R.string.backup_restore_title),
                    subtitle = stringResource(R.string.backup_restore_desc),
                    onClick = onBackupRestoreClick,
                )
            }
            item {
                val setupTint: Color? = if (state.setupDone == false) MaterialTheme.colorScheme.tertiary else null
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.PhoneAndroid,
                    iconTint = setupTint,
                    title = stringResource(CommonR.string.setup_title),
                    subtitle = stringResource(R.string.setup_forcedshow_summary),
                    onClick = onSetupClick,
                )
            }

            // Other category
            item { SettingsCategoryHeader(text = stringResource(R.string.settings_category_other_label)) }

            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.Stars,
                    // Composed like the dashboard title card: the flavor's title template carries
                    // the upgrade branding ("Pro" on GPLAY, "FOSS" on FOSS). Plain String because
                    // that is what the settings item takes.
                    title = brandTitleText(includeQualifier = true),
                    subtitle = stringResource(R.string.settings_upgrade_description),
                    onClick = onProClick,
                )
            }

            item {
                SettingsPreferenceItem(
                    icon = Icons.AutoMirrored.TwoTone.ContactSupport,
                    title = stringResource(R.string.settings_support_label),
                    subtitle = stringResource(R.string.settings_support_description),
                    onClick = onSupportClick,
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.Update,
                    title = stringResource(R.string.changelog_label),
                    subtitle = BuildConfigWrap.VERSION_DESCRIPTION,
                    onClick = onChangelogClick,
                    onLongClick = onChangelogLongClick,
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.Favorite,
                    title = stringResource(R.string.settings_acknowledgements_label),
                    subtitle = stringResource(CommonR.string.general_thank_you_label),
                    onClick = onAcknowledgementsClick,
                )
            }
            item {
                SettingsPreferenceItem(
                    icon = Icons.TwoTone.Policy,
                    title = stringResource(R.string.settings_privacy_policy_label),
                    subtitle = stringResource(R.string.settings_privacy_policy_desc),
                    onClick = onPrivacyClick,
                )
            }
        }
    }
}

@Preview2
@Composable
private fun SettingsScreenPreview() {
    PreviewWrapper {
        SettingsScreen()
    }
}
