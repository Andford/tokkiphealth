package mx.loperstack.tokkiphealth.common.shell

import eu.darken.flowshell.core.cmd.FlowCmd
import eu.darken.flowshell.core.cmd.execute
import mx.loperstack.tokkiphealth.common.adb.AdbManager
import mx.loperstack.tokkiphealth.common.adb.AdbUnavailableException
import mx.loperstack.tokkiphealth.common.adb.canUseAdbNow
import mx.loperstack.tokkiphealth.common.adb.service.runModuleAction
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.root.RootUnavailableException
import mx.loperstack.tokkiphealth.common.root.canUseRootNow
import mx.loperstack.tokkiphealth.common.root.service.runModuleAction
import mx.loperstack.tokkiphealth.common.sharedresource.HasSharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.SharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.adoptChildResource
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsClient
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsCmd
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsResult
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsStreamEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShellOps @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    private val rootManager: RootManager,
    private val adbManager: AdbManager,
) : HasSharedResource<Any> {

    override val sharedResource = SharedResource.createKeepAlive(TAG, appScope + dispatcherProvider.IO)

    private suspend fun <T> adbOps(action: suspend (ShellOpsClient) -> T): T {
        if (!adbManager.canUseAdbNow()) throw AdbUnavailableException()
        adoptChildResource(adbManager.serviceClient)
        return adbManager.serviceClient.runModuleAction(ShellOpsClient::class.java) { action(it) }
    }

    private suspend fun <T> rootOps(action: suspend (ShellOpsClient) -> T): T {
        if (!rootManager.canUseRootNow()) throw RootUnavailableException()
        adoptChildResource(rootManager.serviceClient)
        return rootManager.serviceClient.runModuleAction(ShellOpsClient::class.java) { action(it) }
    }

    suspend fun execute(cmd: ShellOpsCmd, mode: Mode): ShellOpsResult = withContext(dispatcherProvider.IO) {
        try {
            var result: ShellOpsResult? = null
            if (mode == Mode.NORMAL) {
                log(TAG, VERBOSE) { "execute(mode->NORMAL): $cmd" }
                result = cmd.toFlowCmd().execute().toShellOpsResult()
            }

            if (result == null && rootManager.canUseRootNow() && mode == Mode.ROOT) {
                log(TAG, VERBOSE) { "execute(mode->ROOT): $cmd" }
                result = rootOps { it.execute(cmd) }
            }

            if (result == null && adbManager.canUseAdbNow() && mode == Mode.ADB) {
                log(TAG, VERBOSE) { "execute(mode->ADB): $cmd" }
                result = adbOps { it.execute(cmd) }
            }

            if (Bugs.isTrace) {
                log(TAG, VERBOSE) { "execute($cmd, $mode): $result" }
            }

            if (result == null) throw ShellOpsException("No matching mode", cmd)

            result
        } catch (e: IOException) {
            log(TAG, WARN) { "execute($cmd) failed: ${e.asLog()}" }
            throw ShellOpsException(cmd = cmd, cause = e)
        }
    }

    fun executeStream(cmd: ShellOpsCmd, mode: Mode): Flow<ShellOpsStreamEvent> = flow {
        when (mode) {
            Mode.NORMAL -> {
                log(TAG, VERBOSE) { "executeStream(mode->NORMAL): $cmd" }
                val result = cmd.toFlowCmd().execute()
                result.output.forEach { emit(ShellOpsStreamEvent.Stdout(it)) }
                result.errors.forEach { emit(ShellOpsStreamEvent.Stderr(it)) }
                emit(ShellOpsStreamEvent.Exit(result.exitCode.value))
            }
            Mode.ROOT -> {
                if (!rootManager.canUseRootNow()) throw RootUnavailableException()
                log(TAG, VERBOSE) { "executeStream(mode->ROOT): $cmd" }
                rootOps { client -> emitAll(client.executeStream(cmd)) }
            }
            Mode.ADB -> {
                if (!adbManager.canUseAdbNow()) throw AdbUnavailableException()
                log(TAG, VERBOSE) { "executeStream(mode->ADB): $cmd" }
                adbOps { client -> emitAll(client.executeStream(cmd)) }
            }
        }
    }.catch { cause ->
        log(TAG, WARN) { "executeStream($cmd, $mode) failed: ${cause.asLog()}" }
        if (cause is IOException) throw ShellOpsException(cmd = cmd, cause = cause)
        throw cause
    }.flowOn(dispatcherProvider.IO)

    private fun ShellOpsCmd.toFlowCmd() = FlowCmd(cmds)

    private fun FlowCmd.Result.toShellOpsResult() = ShellOpsResult(
        exitCode = exitCode.value,
        output = output,
        errors = errors
    )

    enum class Mode {
        NORMAL, ROOT, ADB
    }

    companion object {
        val TAG = logTag("ShellOps")
    }
}