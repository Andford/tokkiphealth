package mx.loperstack.tokkiphealth.common.adb.service

import mx.loperstack.tokkiphealth.common.adb.isAdbConnectTimeout
import mx.loperstack.tokkiphealth.common.adb.AdbServiceConnection
import mx.loperstack.tokkiphealth.common.adb.AdbSettings
import mx.loperstack.tokkiphealth.common.adb.AdbUnavailableException
import mx.loperstack.tokkiphealth.common.adb.service.internal.AdbConnection
import mx.loperstack.tokkiphealth.common.adb.service.internal.AdbHostLauncher
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.DebugSettings
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.local.ipc.FileOpsClient
import mx.loperstack.tokkiphealth.common.flow.setupCommonEventHandlers
import mx.loperstack.tokkiphealth.common.ipc.IpcClientModule
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.ipc.PkgOpsClient
import mx.loperstack.tokkiphealth.common.sharedresource.SharedResource
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.plus
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbServiceClient @Inject constructor(
    serviceLauncher: AdbHostLauncher,
    @AppScope coroutineScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
    private val adbSettings: AdbSettings,
    private val debugSettings: DebugSettings,
    private val fileOpsClientFactory: FileOpsClient.Factory,
    private val pkgOpsClientFactory: PkgOpsClient.Factory,
    private val shellOpsClientFactory: ShellOpsClient.Factory,
) : SharedResource<AdbServiceClient.Connection>(
    tag = TAG,
    // Run source lifecycle + teardown on IO, not the @AppScope Default pool. On low-core devices the
    // slow host-disconnect IPC during keep-alive expiry would otherwise starve Dispatchers.Default —
    // the same pool the UI's vmScope uses — wedging the dashboard. Mirrors ShellOps/PkgOps.
    parentScope = coroutineScope + dispatcherProvider.IO,
    stopTimeout = ADB_HOST_KEEPALIVE,
    verboseLifecycle = true,
    // A connect timeout means the Shizuku handshake burned its entire budget without an answer, so
    // the default "the starter owns this failure, retry fresh" is a bad trade here: each retry is
    // another full budget. On a device where Shizuku's user service never comes up (the MediaTek/
    // HyperOS defect) that turned one 15s stall into up to five for every concurrent probe. The
    // caller that started the generation always got the real error; this gives it to the others too.
    isRetryableStartupFailure = { error -> !error.isAdbConnectTimeout() },
    source = callbackFlow {
        log(TAG) { "Instantiating ADB launcher..." }

        if (adbSettings.useAdb.value() != true) throw AdbUnavailableException("ADB is not enabled")

        // Mirror App.kt: trace and dry-run only apply while debug mode is on. Without this the host
        // keeps dry-running after debug mode is switched off, while the toggle that turns it off is
        // hidden with the debug card.
        val isDebugInitial = debugSettings.isDebugMode.value()
        val optionsInitial = AdbHostOptions(
            isDebug = isDebugInitial,
            isTrace = isDebugInitial && debugSettings.isTraceMode.value(),
            isDryRun = isDebugInitial && debugSettings.isDryRunMode.value(),
            recorderPath = debugSettings.recorderPath.value(),
        )

        val lastInternal = MutableStateFlow<AdbConnection?>(null)
        serviceLauncher
            .createServiceHostConnection(optionsInitial)
            .onEach { wrapper ->
                lastInternal.value = wrapper.host
                send(wrapper.service)
            }
            .launchIn(this)

        combine(
            debugSettings.isDebugMode.flow,
            debugSettings.isTraceMode.flow,
            debugSettings.isDryRunMode.flow,
            debugSettings.recorderPath.flow,
            lastInternal.filterNotNull(),
        ) { isDebug, isTrace, isDryRun, recorderPath, lastConnection ->
            val optionsDynamic = AdbHostOptions(
                isDebug = isDebug,
                isTrace = isDebug && isTrace,
                isDryRun = isDebug && isDryRun,
                recorderPath = recorderPath,
            )
            log(TAG) { "Updating debug settings: $optionsDynamic" }
            lastConnection.updateHostOptions(optionsDynamic)
        }
            .setupCommonEventHandlers(TAG) { "dynamic-debug-settings" }
            .launchIn(this)

        log(TAG) { "awaitClose()..." }
        awaitClose {
            log(TAG) { "awaitClose() CLOSING" }
        }
    }
        .map {
            Connection(
                ipc = it,
                clientModules = listOf(
                    fileOpsClientFactory.create(it.fileOps),
                    pkgOpsClientFactory.create(it.pkgOps),
                    shellOpsClientFactory.create(it.shellOps),
                )
            )
        }
) {

    data class Connection(
        val ipc: AdbServiceConnection,
        val clientModules: List<IpcClientModule>
    )

    companion object {

        // Keep the privileged ADB host bound briefly after the last lease is released so in-session
        // re-acquisition (screen-bouncing between dashboard and tools, back-to-back privileged ops)
        // reuses the warm host instead of paying the multi-second Shizuku/ADB cold-start. Bounded on
        // purpose: a uid-2000 host should not linger in the background for long. Teardown still happens
        // (and is prompt/safe); this only delays its start.
        private val ADB_HOST_KEEPALIVE: Duration = Duration.ofSeconds(30)


        fun AdbHostLauncher.createServiceHostConnection(
            options: AdbHostOptions,
        ) = this
            .createConnection(
                serviceClass = AdbServiceConnection::class,
                hostClass = AdbHost::class,
                options = options,
            )
            .onStart { log(TAG) { "Initiating connection to host." } }
            .onEach { log(TAG) { "Connection available: $it" } }
            .catch {
                log(TAG, ERROR) { "Failed to establish connection: ${it.asLog()}" }
                throw AdbUnavailableException("Failed to establish connection", cause = it)
            }
            .onCompletion { log(TAG) { "Connection closed" } }

        internal val TAG = logTag("ADB", "Service", "Client")
    }
}