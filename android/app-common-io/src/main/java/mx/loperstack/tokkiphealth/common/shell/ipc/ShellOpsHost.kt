package mx.loperstack.tokkiphealth.common.shell.ipc

import eu.darken.flowshell.core.cmd.FlowCmd
import eu.darken.flowshell.core.cmd.execute
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.ipc.IpcHostModule
import mx.loperstack.tokkiphealth.common.ipc.RemoteInputStream
import mx.loperstack.tokkiphealth.common.shell.SharedShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


class ShellOpsHost @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
) : ShellOpsConnection.Stub(), IpcHostModule {

    // IO, not Default: SharedShell's teardown blocks its thread for up to 5s (runBlocking session
    // close + waitFor), which would starve the host process's small Default pool. Mirrors the
    // RootModule/AdbModule SharedShells and the app-side privileged clients (see PR #2543).
    private val sharedShell = SharedShell(TAG, appScope + dispatcherProvider.IO)

    override fun execute(cmd: ShellOpsCmd): ShellOpsResult = try {
        runBlocking {
            val result = sharedShell.useRes {
                FlowCmd(cmd.cmds).execute(it)
            }
            ShellOpsResult(
                exitCode = result.exitCode.value,
                output = result.output,
                errors = result.errors
            )
        }
    } catch (e: Exception) {
        log(TAG, ERROR) { "execute(cmd=$cmd) failed." }
        throw e.wrapToPropagate()
    }

    override fun executeStream(cmd: ShellOpsCmd): RemoteInputStream {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "executeStream(cmd=$cmd)" }
        val eventFlow: Flow<ShellOpsStreamEvent> = flow {
            try {
                val result = sharedShell.useRes {
                    FlowCmd(cmd.cmds).execute(it)
                }
                result.output.forEach { emit(ShellOpsStreamEvent.Stdout(it)) }
                result.errors.forEach { emit(ShellOpsStreamEvent.Stderr(it)) }
                emit(ShellOpsStreamEvent.Exit(result.exitCode.value))
            } catch (e: Exception) {
                log(TAG, ERROR) { "executeStream(cmd=$cmd) failed: ${e.asLog()}" }
                val wrapped = e.wrapToPropagate()
                emit(ShellOpsStreamEvent.Error(wrapped.message ?: e.toString()))
            }
        }
        return eventFlow.toRemoteInputStream(appScope + dispatcherProvider.Default)
    }

    companion object {
        val TAG = logTag("ShellOps", "Service", "Host", Bugs.processTag)
    }

}
