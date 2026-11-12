package mx.loperstack.tokkiphealth.common.root.service

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
import mx.loperstack.tokkiphealth.common.root.RootSettings
import mx.loperstack.tokkiphealth.common.root.RootUnavailableException
import mx.loperstack.tokkiphealth.common.root.service.internal.RootConnection
import mx.loperstack.tokkiphealth.common.root.service.internal.RootHostLauncher
import mx.loperstack.tokkiphealth.common.root.service.internal.RootHostOptions
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
class RootServiceClient @Inject constructor(
    @AppScope coroutineScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
    private val rootHostLauncher: RootHostLauncher,
    private val rootSettings: RootSettings,
    private val debugSettings: DebugSettings,
    private val fileOpsClientFactory: FileOpsClient.Factory,
    private val pkgOpsClientFactory: PkgOpsClient.Factory,
    private val shellOpsClientFactory: ShellOpsClient.Factory,
) : SharedResource<RootServiceClient.Connection>(
    tag = TAG,
    // Run source lifecycle + teardown on IO, not the @AppScope Default pool. On low-core devices the
    // slow host-disconnect IPC during teardown would otherwise starve Dispatchers.Default — the same
    // pool the UI's vmScope uses — wedging the dashboard. Mirrors ShellOps/PkgOps.
    parentScope = coroutineScope + dispatcherProvider.IO,
    stopTimeout = ROOT_HOST_KEEPALIVE,
    verboseLifecycle = true,
    source = callbackFlow {
        log(TAG) { "Instantiating Root launcher..." }
        if (rootSettings.useRoot.value() != true) throw RootUnavailableException("Root is not enabled")

        // Mirror App.kt: trace and dry-run only apply while debug mode is on. Without this the host
        // keeps dry-running after debug mode is switched off, while the toggle that turns it off is
        // hidden with the debug card.
        val isDebugInitial = debugSettings.isDebugMode.value()
        val initialOptions = RootHostOptions(
            isDebug = isDebugInitial,
            isTrace = isDebugInitial && debugSettings.isTraceMode.value(),
            isDryRun = isDebugInitial && debugSettings.isDryRunMode.value(),
            recorderPath = debugSettings.recorderPath.value(),
        )

        val lastInternal = MutableStateFlow<RootConnection?>(null)
        rootHostLauncher
            .createHostConnection(options = initialOptions)
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
            val dynamicOptions = RootHostOptions(
                isDebug = isDebug,
                isTrace = isDebug && isTrace,
                isDryRun = isDebug && isDryRun,
                recorderPath = recorderPath,
            )
            log(TAG) { "Updating debug settings: $dynamicOptions" }
            lastConnection.updateHostOptions(dynamicOptions)
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
        val ipc: RootServiceConnection,
        val clientModules: List<IpcClientModule>
    )

    companion object {

        // Keep the su host bound briefly after the last lease is released so back-to-back privileged
        // operations and dashboard-to-tool navigation reuse the warm host instead of paying the su
        // cold start again. Bounded on purpose: a root host should not linger in the background for
        // long. Teardown still happens (and is prompt/safe); this only delays its start. It also
        // replaces the incidental warm window that the removed RootManager.binder lease used to
        // provide for as long as the dashboard was subscribed.
        private val ROOT_HOST_KEEPALIVE: Duration = Duration.ofSeconds(30)

        fun RootHostLauncher.createHostConnection(
            /**
             * TODO Keep this false, but evaluate more types of rooted devices
             * Not needed unless [DataAreaManager] fails to get the altered paths or the rest of IO can't cope.
             * Being able to work without mount-master is more reliable.
             */
            useMountMaster: Boolean = false,
            options: RootHostOptions,
        ) = this
            .createConnection(
                serviceClass = RootServiceConnection::class,
                hostClass = RootHost::class,
                useMountMaster = useMountMaster,
                options = options,
            )
            .onStart { log(TAG) { "Initiating connection to host." } }
            .onEach { log(TAG) { "Connection available: $it" } }
            .catch {
                log(TAG, ERROR) { "Failed to establish connection: ${it.asLog()}" }
                throw RootUnavailableException("Failed to establish connection", cause = it)
            }
            .onCompletion { log(TAG) { "Connection unavailable." } }

        internal val TAG = logTag("Root", "Service", "Client")
    }
}