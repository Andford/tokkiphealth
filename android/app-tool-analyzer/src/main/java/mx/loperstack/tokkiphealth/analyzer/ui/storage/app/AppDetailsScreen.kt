package mx.loperstack.tokkiphealth.analyzer.ui.storage.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material.icons.twotone.FolderShared
import androidx.compose.material.icons.twotone.Inventory
import androidx.compose.material.icons.twotone.PermMedia
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.MaterialTheme
import mx.loperstack.tokkiphealth.common.compose.layout.SdmListDefaults
import mx.loperstack.tokkiphealth.common.compose.layout.SdmScaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.loperstack.tokkiphealth.analyzer.R
import mx.loperstack.tokkiphealth.analyzer.ui.AppDetailsRoute
import mx.loperstack.tokkiphealth.analyzer.ui.storage.app.items.AppDetailsGroupRow
import mx.loperstack.tokkiphealth.analyzer.ui.storage.app.items.AppDetailsHeaderCard
import mx.loperstack.tokkiphealth.common.coil.AppIconExtraSlot
import mx.loperstack.tokkiphealth.common.compose.layout.SdmTooltipIconButton
import mx.loperstack.tokkiphealth.common.compose.layout.plusBottom
import mx.loperstack.tokkiphealth.common.compose.preview.Preview2
import mx.loperstack.tokkiphealth.common.compose.preview.PreviewWrapper
import mx.loperstack.tokkiphealth.common.compose.progress.ProgressOverlay
import mx.loperstack.tokkiphealth.common.error.ErrorEventHandler
import mx.loperstack.tokkiphealth.common.navigation.NavigationEventHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun AppDetailsScreenHost(
    route: AppDetailsRoute,
    vm: AppDetailsViewModel = hiltViewModel(),
) {
    ErrorEventHandler(vm)
    NavigationEventHandler(vm)

    LaunchedEffect(route) { vm.bindRoute(route) }

    AppDetailsScreen(
        stateSource = vm.state,
        onSettingsClick = vm::onSettingsClick,
        onGroupClick = vm::onGroupClick,
        onRefresh = vm::refresh,
        onNavigateUp = vm::navUp,
    )
}

@Composable
internal fun AppDetailsScreen(
    stateSource: Flow<AppDetailsViewModel.State> = MutableStateFlow(AppDetailsViewModel.State.Loading),
    onSettingsClick: (mx.loperstack.tokkiphealth.analyzer.core.storage.categories.AppCategory.PkgStat) -> Unit = {},
    onGroupClick: (mx.loperstack.tokkiphealth.analyzer.core.content.ContentGroup, mx.loperstack.tokkiphealth.analyzer.core.storage.categories.AppCategory.PkgStat) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
) {
    val state by stateSource.collectAsStateWithLifecycle(initialValue = AppDetailsViewModel.State.Loading)
    val context = LocalContext.current

    when (val s = state) {
        AppDetailsViewModel.State.NotFound -> {
            LaunchedEffect(Unit) { onNavigateUp() }
            SdmScaffold(
                topBar = {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            SdmTooltipIconButton(
                                icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                                label = stringResource(mx.loperstack.tokkiphealth.common.R.string.general_navigate_up_action),
                                onClick = onNavigateUp,
                            )
                        },
                    )
                },
            ) { _ -> }
        }

        AppDetailsViewModel.State.Loading -> {
            SdmScaffold(
                topBar = {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            SdmTooltipIconButton(
                                icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                                label = stringResource(mx.loperstack.tokkiphealth.common.R.string.general_navigate_up_action),
                                onClick = onNavigateUp,
                            )
                        },
                    )
                },
            ) { paddingValues ->
                ProgressOverlay(
                    data = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) { }
            }
        }

        is AppDetailsViewModel.State.Ready -> {
            SdmScaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(s.pkgStat.label.get(context))
                                Text(
                                    text = s.storage.label.get(context),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        },
                        navigationIcon = {
                            SdmTooltipIconButton(
                                icon = Icons.AutoMirrored.TwoTone.ArrowBack,
                                label = stringResource(mx.loperstack.tokkiphealth.common.R.string.general_navigate_up_action),
                                onClick = onNavigateUp,
                            )
                        },
                        actions = {
                            if (s.progress == null) {
                                SdmTooltipIconButton(
                                    icon = Icons.TwoTone.Refresh,
                                    label = stringResource(mx.loperstack.tokkiphealth.common.R.string.general_refresh_action),
                                    onClick = onRefresh,
                                )
                            }
                        },
                    )
                },
            ) { paddingValues ->
                ProgressOverlay(
                    data = s.progress,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    extraSlot = AppIconExtraSlot,
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            .plusBottom(SdmListDefaults.ContentBottomSpacing),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item("header") {
                            AppDetailsHeaderCard(
                                storage = s.storage,
                                pkgStat = s.pkgStat,
                                onSettingsClick = { onSettingsClick(s.pkgStat) },
                            )
                        }
                        s.appCode?.let { group ->
                            item("appcode") {
                                AppDetailsGroupRow(
                                    group = group,
                                    labelRes = R.string.analyzer_storage_content_app_code_label,
                                    descRes = R.string.analyzer_storage_content_app_code_description,
                                    icon = Icons.TwoTone.Inventory,
                                    onClick = { onGroupClick(group, s.pkgStat) },
                                )
                            }
                        }
                        s.appData?.let { group ->
                            item("appdata") {
                                AppDetailsGroupRow(
                                    group = group,
                                    labelRes = R.string.analyzer_storage_content_app_data_label,
                                    descRes = R.string.analyzer_storage_content_app_data_description,
                                    icon = Icons.TwoTone.Folder,
                                    onClick = { onGroupClick(group, s.pkgStat) },
                                )
                            }
                        }
                        s.appMedia?.let { group ->
                            item("appmedia") {
                                AppDetailsGroupRow(
                                    group = group,
                                    labelRes = R.string.analyzer_storage_content_app_media_label,
                                    descRes = R.string.analyzer_storage_content_app_media_description,
                                    icon = Icons.TwoTone.PermMedia,
                                    onClick = { onGroupClick(group, s.pkgStat) },
                                )
                            }
                        }
                        s.extraData?.let { group ->
                            item("extradata") {
                                AppDetailsGroupRow(
                                    group = group,
                                    labelRes = R.string.analyzer_storage_content_app_extra_label,
                                    descRes = R.string.analyzer_storage_content_app_extra_description,
                                    icon = Icons.TwoTone.FolderShared,
                                    onClick = { onGroupClick(group, s.pkgStat) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview2
@Composable
private fun AppDetailsScreenLoadingPreview() {
    PreviewWrapper {
        AppDetailsScreen()
    }
}
