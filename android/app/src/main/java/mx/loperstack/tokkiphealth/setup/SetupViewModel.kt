package mx.loperstack.tokkiphealth.setup

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.adb.shizuku.AdbBackend
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.device.DeviceDetective
import mx.loperstack.tokkiphealth.common.device.RomType
import mx.loperstack.tokkiphealth.common.flow.SingleEventFlow
import mx.loperstack.tokkiphealth.common.flow.setupCommonEventHandlers
import mx.loperstack.tokkiphealth.common.navigation.routes.DashboardRoute
import mx.loperstack.tokkiphealth.common.navigation.routes.DataAreasRoute
import mx.loperstack.tokkiphealth.common.permissions.Permission
import mx.loperstack.tokkiphealth.common.permissions.RuntimePermission
import mx.loperstack.tokkiphealth.common.permissions.Specialpermission
import mx.loperstack.tokkiphealth.common.pkgs.getLaunchIntent
import mx.loperstack.tokkiphealth.common.pkgs.getSettingsIntent
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.setup.automation.AutomationSetupCardItem
import mx.loperstack.tokkiphealth.setup.automation.AutomationSetupModule
import mx.loperstack.tokkiphealth.setup.inventory.InventorySetupCardItem
import mx.loperstack.tokkiphealth.setup.inventory.InventorySetupModule
import mx.loperstack.tokkiphealth.setup.notification.NotificationSetupCardItem
import mx.loperstack.tokkiphealth.setup.notification.NotificationSetupModule
import mx.loperstack.tokkiphealth.setup.root.RootSetupCardItem
import mx.loperstack.tokkiphealth.setup.root.RootSetupModule
import mx.loperstack.tokkiphealth.setup.saf.SAFSetupCardItem
import mx.loperstack.tokkiphealth.setup.saf.SAFSetupModule
import mx.loperstack.tokkiphealth.setup.shizuku.AdbManagerInstallGuide
import mx.loperstack.tokkiphealth.setup.shizuku.ShizukuSetupCardItem
import mx.loperstack.tokkiphealth.setup.shizuku.ShizukuSetupModule
import mx.loperstack.tokkiphealth.setup.storage.StorageSetupCardItem
import mx.loperstack.tokkiphealth.setup.storage.StorageSetupModule
import mx.loperstack.tokkiphealth.setup.usagestats.UsageStatsSetupCardItem
import mx.loperstack.tokkiphealth.setup.usagestats.UsageStatsSetupModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    @Suppress("unused") private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @Suppress("StaticFieldLeak") @ApplicationContext private val context: Context,
    private val setupManager: SetupManager,
    private val storageSetupModule: StorageSetupModule,
    private val safSetupModule: SAFSetupModule,
    private val automationSetupModule: AutomationSetupModule,
    private val webpageTool: WebpageTool,
    private val rootSetupModule: RootSetupModule,
    private val shizukuSetupModule: ShizukuSetupModule,
    private val inventorySetupModule: InventorySetupModule,
    private val deviceDetective: DeviceDetective,
    private val adbManagerInstallGuide: AdbManagerInstallGuide,
) : ViewModel4(dispatcherProvider, TAG) {

    // Options are driven from the Host via setScreenOptions() (the SetupRoute entry forwards the
    // route object). Do NOT read SavedStateHandle.toRoute<>() here — it crashes under Nav3.
    private val screenOptionsFlow: MutableStateFlow<SetupScreenOptions> =
        MutableStateFlow(SetupScreenOptions())
    val screenOptions: SetupScreenOptions get() = screenOptionsFlow.value

    fun setScreenOptions(options: SetupScreenOptions) {
        log(TAG) { "setScreenOptions($options)" }
        screenOptionsFlow.value = options
    }

    init {
        log(TAG) { "Setup init: options=${screenOptionsFlow.value}" }
        setupManager.setDismissed(false)
    }

    /**
     * Does this device match the hardware/ROM pattern behind the known upstream Shizuku problem
     * (RikkaApps/Shizuku#1198), where Shizuku's helper process dies before it can call back?
     *
     * Resolved here rather than in the card because [DeviceDetective.getROMType] hits the package
     * manager, which must not run during recomposition. Lazy + cached: the answer cannot change
     * while the process lives, and the flow that reads it does not run on the main thread.
     *
     * The card only RENDERS the hint after a connection has actually failed (this value is read on
     * every emission, it is the display that is gated), so a false positive costs one imprecise
     * sentence rather than a wrong diagnosis.
     */
    private val hasKnownShizukuIssueRisk: Boolean by lazy {
        deviceDetective.isMediatekSoc() || deviceDetective.getROMType() in KNOWN_SHIZUKU_ISSUE_ROMS
    }

    val events = SingleEventFlow<SetupEvents>()

    private fun emitPermissionEvent(perm: Permission) {
        when (perm) {
            is Specialpermission -> events.tryEmit(
                SetupEvents.SpecialPermissionRequest(
                    item = perm,
                    intent = perm.createIntent(context, deviceDetective),
                    fallbackIntent = perm.createIntentFallback(context),
                )
            )

            is RuntimePermission -> events.tryEmit(SetupEvents.RuntimePermissionRequests(perm))
            else -> log(TAG, WARN) { "Unknown permission type for $perm, cannot launch" }
        }
    }

    private val itemsStateFlow: StateFlow<List<SetupCardItem>?> = combine(
        setupManager.state,
        screenOptionsFlow,
    ) { setupState, options ->
        val items = mutableListOf<SetupCardItem>()

        val typeFilter = options.typeFilter
        setupState.moduleStates
            .filter { typeFilter == null || typeFilter.contains(it.type) }
            .filter { it !is SetupModule.State.Current || !it.isComplete || options.showCompleted }
                .mapNotNull { state ->
                    when (state.type) {
                        SetupModule.Type.SAF -> when (state) {
                            is SetupModule.State.Current -> SAFSetupCardItem(
                                state = state as SAFSetupModule.Result,
                                onPathClicked = {
                                    if (!it.hasAccess) {
                                        events.tryEmit(SetupEvents.SafRequestAccess(it))
                                    }
                                },
                                onHelp = {
                                    webpageTool.open("https://sdmse.darken.eu/setup#storage-access-framework")
                                },
                            ).takeIf { it.state.paths.isNotEmpty() }

                            is SetupModule.State.Loading -> SetupLoadingCardItem(state)
                        }

                        SetupModule.Type.STORAGE -> when (state) {
                            is SetupModule.State.Current -> StorageSetupCardItem(
                                state = state as StorageSetupModule.Result,
                                onPathClicked = {
                                    state.missingPermission.firstOrNull()?.let { perm ->
                                        emitPermissionEvent(perm)
                                    }
                                },
                                onHelp = {
                                    webpageTool.open("https://sdmse.darken.eu/setup#manage-storage")
                                },
                            )

                            is SetupModule.State.Loading -> SetupLoadingCardItem(state)
                        }

                        SetupModule.Type.ROOT -> when (state) {
                            is SetupModule.State.Current -> RootSetupCardItem(
                                state = state as RootSetupModule.Result,
                                onToggleUseRoot = {
                                    launch {
                                        rootSetupModule.toggleUseRoot(it)
                                        rootSetupModule.refresh()
                                    }
                                },
                                onHelp = {
                                    webpageTool.open("https://sdmse.darken.eu/setup#root-access")
                                },
                                onRetry = { launch { rootSetupModule.refresh() } },
                            )

                            is SetupModule.State.Loading -> SetupLoadingCardItem(state)
                        }

                        SetupModule.Type.USAGE_STATS -> when (state) {
                            is SetupModule.State.Current -> UsageStatsSetupCardItem(
                                state = state as UsageStatsSetupModule.Result,
                                onGrantAction = {
                                    state.missingPermission.firstOrNull()?.let { perm ->
                                        emitPermissionEvent(perm)
                                    }
                                },
                                onHelp = {
                                    webpageTool.open("https://sdmse.darken.eu/setup#usage-statistics")
                                }
                            )

                            is SetupModule.State.Loading -> SetupLoadingCardItem(state)
                        }

                        SetupModule.Type.AUTOMATION -> when (state) {
                            is SetupModule.State.Current -> AutomationSetupCardItem(
                                state = state as AutomationSetupModule.Result,
                                onGrantAction = {
                                    launch {
                                        automationSetupModule.setAllow(true)
                                        automationSetupModule.refresh()
                                        if (!state.canSelfEnable) {
                                            events.tryEmit(SetupEvents.ConfigureAccessibilityService(state))
                                        }
                                    }
                                },
                                onDismiss = {
                                    launch {
                                        automationSetupModule.setAllow(false)
                                        automationSetupModule.refresh()
                                    }
                                },
                                onHelp = {
                                    webpageTool.open("https://sdmse.darken.eu/setup#accessibility-service")
                                },
                                onRestrictionsShow = {
                                    events.tryEmit(SetupEvents.ShowOurDetailsPage(state.liftRestrictionsIntent))
                                },
                                onRestrictionsHelp = {
                                    webpageTool.open("https://sdmse.darken.eu/setup#acs-appops-restrictions")
                                },
                                onAdvancedProtectionHelp = {
                                    webpageTool.open("https://sdmse.darken.eu/setup#acs-advanced-protection")
                                },
                            )

                            is SetupModule.State.Loading -> SetupLoadingCardItem(state)
                        }

                        SetupModule.Type.NOTIFICATION -> when (state) {
                            is SetupModule.State.Current -> NotificationSetupCardItem(
                                state = state as NotificationSetupModule.Result,
                                onGrantAction = {
                                    state.missingPermission.firstOrNull()?.let { perm ->
                                        emitPermissionEvent(perm)
                                    }
                                },
                                onHelp = {
                                    webpageTool.open("https://sdmse.darken.eu/setup#notifications")
                                }
                            )

                            is SetupModule.State.Loading -> SetupLoadingCardItem(state)
                        }

                        SetupModule.Type.SHIZUKU -> when (state) {
                            is SetupModule.State.Current -> ShizukuSetupCardItem(
                                state = state as ShizukuSetupModule.Result,
                                onToggleUseShizuku = {
                                    launch { shizukuSetupModule.toggleUseShizuku(it) }
                                },
                                onHelp = {
                                    // Follows the active backend: a Porter user reading "ADB access
                                    // failed" must not land on Shizuku's docs section. Which page a
                                    // Porter user gets is the flavor's call, because Porter's setup
                                    // guide doubles as its install instructions.
                                    webpageTool.open(
                                        when (state.backend) {
                                            AdbBackend.PORTER -> adbManagerInstallGuide.porterHelpUrl
                                            AdbBackend.SHIZUKU ->
                                                "https://sdmse.darken.eu/setup#shizuku"
                                        }
                                    )
                                },
                                onOpen = {
                                    // Not every manager exports a launcher entry, e.g. Shizuku+'s Compat
                                    // Hub. Its app info page is still somewhere the user can act, and a
                                    // button that does nothing at all is worse than the wrong screen.
                                    val intent = state.pkg.getLaunchIntent(context)
                                        ?: state.pkg.getSettingsIntent(context)
                                    try {
                                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    } catch (e: ActivityNotFoundException) {
                                        errorEvents.tryEmit(e)
                                    } catch (e: SecurityException) {
                                        // An exported-but-guarded launcher activity rejects us at
                                        // startActivity() rather than failing to resolve.
                                        errorEvents.tryEmit(e)
                                    }
                                },
                                onRetry = { launch { shizukuSetupModule.refresh() } },
                                onGrantAccess = { launch { shizukuSetupModule.grantAccess() } },
                                showKnownIssueHint = hasKnownShizukuIssueRisk,
                                installLabelRes = adbManagerInstallGuide.labelRes,
                                onInstall = { webpageTool.open(adbManagerInstallGuide.url) },
                            )

                            is SetupModule.State.Loading -> SetupLoadingCardItem(state)
                        }

                        SetupModule.Type.INVENTORY -> when (state) {
                            is SetupModule.State.Current -> InventorySetupCardItem(
                                state = state as InventorySetupModule.Result,
                                onGrantAction = {
                                    val result = state
                                    val runtimePerm = result.missingPermission.firstOrNull { it is RuntimePermission }
                                    if (runtimePerm != null) {
                                        emitPermissionEvent(runtimePerm)
                                    } else {
                                        events.tryEmit(SetupEvents.ShowOurDetailsPage(result.settingsIntent))
                                    }
                                },
                                onHelp = {
                                    webpageTool.open(InventorySetupModule.INFO_URL)
                                },
                                onRetry = { launch { inventorySetupModule.refresh() } },
                            )

                            is SetupModule.State.Loading -> SetupLoadingCardItem(state)
                        }
                    }
                }
                // A card changing between incomplete, loading and complete must not move under the
                // user's finger. Completion is conveyed by each card's content; position is stable.
                .sortedInSetupDisplayOrder()
                .run { items.addAll(this) }

        items
    }
        .setupCommonEventHandlers(TAG) { "listItems" }
        .stateIn(vmScope, SharingStarted.Eagerly, null)

    internal val uiState: StateFlow<SetupUiState> = combine(
        itemsStateFlow,
        screenOptionsFlow,
    ) { items, options ->
            when {
                items == null -> SetupUiState.Loading
                items.isEmpty() && !options.showCompleted -> SetupUiState.Complete
                else -> SetupUiState.Cards(items)
            }
        }
        .distinctUntilChanged()
        .safeStateIn(
            initialValue = SetupUiState.Loading,
            onError = { SetupUiState.Loading },
        )

    fun onSafAccessGranted(uri: Uri?) = launch {
        log(TAG) { "onSafAccessGranted(uri=$uri)" }
        if (uri == null) return@launch
        try {
            safSetupModule.takePermission(uri)
        } catch (e: IllegalArgumentException) {
            events.tryEmit(SetupEvents.SafWrongPathError(e))
        }
    }

    fun onRuntimePermissionsGranted(permission: Permission?, granted: Boolean) = launch {
        log(TAG) { "onRuntimePermissionGranted(result=$permission,granted=$granted)" }
        if (granted) {
            when (permission) {
                Permission.MANAGE_EXTERNAL_STORAGE,
                Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE -> {
                    storageSetupModule.onPermissionChanged(permission, granted)
                }

                Permission.PACKAGE_USAGE_STATS -> {}
                Permission.POST_NOTIFICATIONS -> {}
                Permission.WRITE_SECURE_SETTINGS -> {}
                Permission.QUERY_ALL_PACKAGES -> {}
                Permission.INTERACT_ACROSS_USERS -> {}
                Permission.GET_INSTALLED_APPS -> {}
                null -> {}
            }
            setupManager.refresh()
        } else if (permission == Permission.GET_INSTALLED_APPS) {
            // Runtime dialog may not work on some OEMs — fall back to app settings
            val settingsIntent = context.packageName.toPkgId().getSettingsIntent(context)
            events.tryEmit(SetupEvents.ShowOurDetailsPage(settingsIntent))
        }
    }

    fun onAccessibilityReturn() = launch {
        log(TAG) { "onAccessibilityReturn" }
        setupManager.refresh()
    }

    fun openSetupHelp() {
        webpageTool.open("https://sdmse.darken.eu/setup")
    }

    fun openSafMissingAppHelpUrl() {
        webpageTool.open("https://sdmse.darken.eu/setup#saf-open_document_tree-activitynotfoundexception")
    }

    fun openSafWrongPathHelpUrl() {
        webpageTool.open("https://sdmse.darken.eu/setup#storage-access-framework")
    }

    fun navToDataAreas() {
        navTo(DataAreasRoute)
    }

    fun navback() {
        if (screenOptions.isOnboarding) {
            navTo(
                DashboardRoute,
                popUpTo = DashboardRoute,
                inclusive = true,
            )
        } else {
            navUp()
        }
    }

    companion object {
        /** ROMs with the framework modification behind RikkaApps/Shizuku#1198. */
        private val KNOWN_SHIZUKU_ISSUE_ROMS = setOf(RomType.HYPEROS, RomType.MIUI)

        private val TAG = logTag("Setup", "ViewModel")
    }
}

private val SETUP_DISPLAY_ORDER = listOf(
    SetupModule.Type.INVENTORY,
    SetupModule.Type.NOTIFICATION,
    SetupModule.Type.STORAGE,
    SetupModule.Type.SAF,
    SetupModule.Type.SHIZUKU,
    SetupModule.Type.ROOT,
    SetupModule.Type.USAGE_STATS,
    SetupModule.Type.AUTOMATION,
)

internal fun List<SetupCardItem>.sortedInSetupDisplayOrder(): List<SetupCardItem> =
    sortedBy { item -> SETUP_DISPLAY_ORDER.indexOf(item.state.type) }
