package mx.loperstack.tokkiphealth.common.debug

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.automation.core.AutomationManager
import mx.loperstack.tokkiphealth.automation.core.debug.DebugTask
import mx.loperstack.tokkiphealth.common.adb.AdbSettings
import mx.loperstack.tokkiphealth.common.adb.service.AdbServiceClient
import mx.loperstack.tokkiphealth.common.adb.shizuku.AdbBackend
import mx.loperstack.tokkiphealth.common.adb.shizuku.ShizukuManager
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.DataAreaManager
import mx.loperstack.tokkiphealth.common.areas.currentAreas
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.datastore.valueBlocking
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.*
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.isDirectory
import mx.loperstack.tokkiphealth.common.flow.combine
import mx.loperstack.tokkiphealth.common.forensics.FileForensics
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.root.RootSettings
import mx.loperstack.tokkiphealth.common.root.service.RootServiceClient
import mx.loperstack.tokkiphealth.common.sharedresource.runSessionAction
import mx.loperstack.tokkiphealth.common.shell.ShellOps
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsCmd
import mx.loperstack.tokkiphealth.common.uix.ViewModel2
import mx.loperstack.tokkiphealth.main.ui.dashboard.DashboardEvents
import mx.loperstack.tokkiphealth.main.ui.dashboard.cards.DebugDashboardCardItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject

class DebugCardProvider @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
    private val debugSettings: DebugSettings,
    private val rootSettings: RootSettings,
    private val rootManager: RootManager,
    private val adbSettings: AdbSettings,
    private val shizukuManager: ShizukuManager,
    private val rootClient: RootServiceClient,
    private val pkgRepo: PkgRepo,
    private val dataAreaManager: DataAreaManager,
    private val shellOps: ShellOps,
    private val shizukuClient: AdbServiceClient,
    private val automation: AutomationManager,
    private val fileForensics: FileForensics,
    private val gatewaySwitch: GatewaySwitch,
) {

    private val rootTestState = MutableStateFlow<RootTestResult?>(null)
    private val shizukuTestState = MutableStateFlow<ShizukuTestResult?>(null)
    private val isCheckingFolders = MutableStateFlow(false)

    fun create(
        vm: ViewModel2,
        onNavigate: (Any) -> Unit = {},
        onError: (Throwable) -> Unit = {},
        onShowEvent: (DashboardEvents) -> Unit = {},
    ) = combine(
        debugSettings.isDebugMode.flow.distinctUntilChanged(),
        debugSettings.isTraceMode.flow.distinctUntilChanged(),
        debugSettings.isDryRunMode.flow.distinctUntilChanged(),
        debugSettings.floatingLogVisible.flow.distinctUntilChanged(),
        rootTestState,
        shizukuTestState,
        automation.currentTask,
        isCheckingFolders,
    ) { isDebug, isTrace, isDryRun, isLogVisible, rootState, shizukuState, acsTask, checkingFolders ->
        if (!isDebug) return@combine null
        DebugDashboardCardItem(
            isDryRunEnabled = isDryRun,
            onDryRunEnabled = {
                debugSettings.isDryRunMode.valueBlocking = it
                log(TAG, INFO) { "DryRun mode enabled: $it" }
            },
            isTraceEnabled = isTrace,
            onTraceEnabled = { debugSettings.isTraceMode.valueBlocking = it },
            onReloadAreas = {
                vm.launch { dataAreaManager.reload() }
            },
            onReloadPkgs = {
                vm.launch {
                    pkgRepo.refresh()
                }
            },
            isLogPanelVisible = isLogVisible,
            onToggleLogPanel = { enabled ->
                vm.launch { debugSettings.floatingLogVisible.value(enabled) }
            },
            rootTestResult = rootState,
            onTestRoot = {
                vm.launch {
                    rootTestState.value = RootTestResult(
                        testId = UUID.randomUUID().toString(),
                        hasUserConsent = rootSettings.useRoot.value(),
                        magiskGranted = rootManager.isRooted(),
                        serviceLaunched = withTimeoutOrNull(10 * 1000) {
                            val sb = StringBuilder()

                            val base = try {
                                rootClient.runSessionAction { it.ipc.checkBase() }
                            } catch (e: Exception) {
                                e.message
                            }
                            sb.append("BaseCheck:\n$base")

                            var opsError: String? = null
                            val opsResult = try {
                                shellOps.execute(ShellOpsCmd("whoami"), ShellOps.Mode.ROOT)
                            } catch (e: Exception) {
                                opsError = e.message
                                null
                            }
                            sb.append("ShellOps 'whoami':\n${opsResult ?: opsError}")

                            sb.toString()
                        }
                    )
                }
            },
            shizukuTestResult = shizukuState,
            onTestShizuku = {
                vm.launch {
                    shizukuTestState.value = ShizukuTestResult(
                        testId = UUID.randomUUID().toString(),
                        hasUserConsent = adbSettings.useShizuku.value(),
                        isInstalled = shizukuManager.isInstalled(),
                        isGranted = shizukuManager.isGranted(),
                        backend = shizukuManager.activeBackend(),
                        serverUid = shizukuManager.serverUid(),
                        serviceLaunched = withTimeoutOrNull(10 * 1000) {
                            val sb = StringBuilder()

                            val base = try {
                                shizukuClient.runSessionAction { it.ipc.checkBase() }
                            } catch (e: Exception) {
                                e
                            }
                            sb.append("BaseCheck:\n$base\n")

                            var opsError: String? = null
                            val opsResult = try {
                                shellOps.execute(ShellOpsCmd("whoami"), ShellOps.Mode.ADB)
                            } catch (e: Exception) {
                                opsError = e.message
                                null
                            }
                            sb.append("ShellOps 'whoami':\n${opsResult ?: opsError}")

                            sb.toString()
                        }
                    )
                }
            },
            onRunTest = {
                vm.launch {

                }
            },
            onAcsDebug = {
                if (acsTask != null) {
                    automation.cancelTask()
                } else {
                    appScope.launch {
                        try {
                            automation.submit(DebugTask())
                        } catch (e: Exception) {
                            if (e !is CancellationException) {
                                onError(e)
                            }
                        }
                    }
                }
            },
            acsTask = acsTask,
            isCheckingUnknownFolders = checkingFolders,
            onCheckUnknownFolders = {
                if (checkingFolders) return@DebugDashboardCardItem
                vm.launch {
                    checkUnknownFolders(vm, onError, onShowEvent)
                }
            },
        )
    }

    private suspend fun checkUnknownFolders(
        vm: ViewModel2,
        onError: (Throwable) -> Unit,
        onShowEvent: (DashboardEvents) -> Unit
    ) {
        isCheckingFolders.value = true
        try {
            val unknownPaths = mutableListOf<String>()
            var scannedCount = 0
            var skippedCount = 0

            gatewaySwitch.sharedResource.get().use {
                val areas = dataAreaManager.currentAreas().filter { it.type == DataArea.Type.SDCARD }
                val multipleAreas = areas.size > 1

                val skipSubpaths = setOf("Android/data", "Android/media", "Android/obb")

                for (area in areas) {
                    val depth1Dirs = try {
                        gatewaySwitch.lookupFiles(area.path).toList().filter { it.isDirectory }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log(TAG, WARN) { "Failed to list ${area.path}: ${e.asLog()}" }
                        skippedCount++
                        continue
                    }

                    val candidates = mutableListOf<Pair<String, mx.loperstack.tokkiphealth.common.files.APath>>()

                    for (dir in depth1Dirs) {
                        val dirName = dir.lookedUp.path.substringAfterLast('/')

                        // Add depth-1 candidate
                        candidates.add(dirName to dir.lookedUp)

                        // Get depth-2 children
                        val depth2Dirs = try {
                            gatewaySwitch.lookupFiles(dir.lookedUp).toList().filter { it.isDirectory }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            log(TAG, WARN) { "Failed to list ${dir.lookedUp}: ${e.asLog()}" }
                            skippedCount++
                            continue
                        }

                        for (child in depth2Dirs) {
                            val subpath = "$dirName/${child.lookedUp.path.substringAfterLast('/')}"
                            if (subpath in skipSubpaths) continue
                            candidates.add(subpath to child.lookedUp)
                        }
                    }

                    for ((displaySubpath, path) in candidates) {
                        scannedCount++
                        try {
                            val ownerInfo = fileForensics.findOwners(path)
                            if (ownerInfo == null || ownerInfo.owners.isEmpty()) {
                                val displayPath = if (multipleAreas) {
                                    "${area.path.path}/$displaySubpath"
                                } else {
                                    displaySubpath
                                }
                                unknownPaths.add(displayPath)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            log(TAG, WARN) { "Failed findOwners for $path: ${e.asLog()}" }
                            skippedCount++
                        }
                    }
                }
            }

            log(
                TAG,
                INFO
            ) { "Unknown folders check: scanned=$scannedCount, skipped=$skippedCount, unknown=${unknownPaths.size}" }
            if (unknownPaths.size > 200) {
                log(TAG, WARN) { "Full unknown list (${unknownPaths.size}):\n${unknownPaths.joinToString("\n")}" }
            }

            withContext(dispatcherProvider.Main) {
                onShowEvent(
                    DashboardEvents.ShowUnknownFolders(
                        unknownPaths = unknownPaths.take(200),
                        scannedCount = scannedCount,
                        skippedCount = skippedCount,
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log(TAG, ERROR) { "checkUnknownFolders failed: ${e.asLog()}" }
            onError(e)
        } finally {
            isCheckingFolders.value = false
        }
    }

    data class RootTestResult(
        val testId: String,
        val hasUserConsent: Boolean?,
        val magiskGranted: Boolean,
        val serviceLaunched: String?,
    )

    data class ShizukuTestResult(
        val testId: String,
        val hasUserConsent: Boolean?,
        val isInstalled: Boolean,
        val isGranted: Boolean?,
        /** Without this a Porter failure and a Shizuku failure look identical in a debug log. */
        val backend: AdbBackend,
        /** 2000 when the helper really runs at shell privilege. */
        val serverUid: Int?,
        val serviceLaunched: String?,
    )

    companion object {
        private val TAG = logTag("Debug", "Card", "Provider")
    }
}