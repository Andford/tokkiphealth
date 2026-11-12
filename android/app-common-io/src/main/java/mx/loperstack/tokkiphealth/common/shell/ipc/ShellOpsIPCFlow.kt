package mx.loperstack.tokkiphealth.common.shell.ipc

import android.os.Parcel
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.ipc.RemoteInputStream
import mx.loperstack.tokkiphealth.common.ipc.inputStream
import mx.loperstack.tokkiphealth.common.ipc.remoteInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream

private val TAG = logTag("ShellOps", "IPC", "Flow")

// Conservatively well below the ~1 MB Android binder buffer.
// Base64 expands payload by ~33%, so an encoded chunk is ~85 KB max per AIDL read.
private const val CHUNK_BYTE_LIMIT = 64 * 1024
private const val PIPE_BUFFER_SIZE = 2 * CHUNK_BYTE_LIMIT
private const val WRITER_BUFFER_SIZE = CHUNK_BYTE_LIMIT

fun RemoteInputStream.toShellOpsEventFlow(): Flow<ShellOpsStreamEvent> = flow {
    if (Bugs.isTrace) log(TAG, VERBOSE) { "toShellOpsEventFlow() starting..." }

    val reader = this@toShellOpsEventFlow.inputStream().reader().buffered(WRITER_BUFFER_SIZE)
    var sawTerminal = false
    try {
        while (currentCoroutineContext().isActive) {
            val line = reader.readLine() ?: break

            val decoded = line.decodeBase64()
                ?: throw IOException("ShellOps stream: invalid base64 frame")
            val parcel = Parcel.obtain().apply {
                unmarshall(decoded.toByteArray(), 0, decoded.size)
                setDataPosition(0)
            }
            val wrapper = try {
                ShellOpsEventsIPCWrapper.createFromParcel(parcel)
            } finally {
                parcel.recycle()
            }
            if (Bugs.isTrace) {
                log(TAG, VERBOSE) { "READCHUNK: ${decoded.size}B to ${wrapper.payload.size} events" }
            }
            for (event in wrapper.payload) {
                if (event is ShellOpsStreamEvent.Exit || event is ShellOpsStreamEvent.Error) {
                    sawTerminal = true
                }
                emit(event)
            }
        }
        if (!sawTerminal) {
            throw IOException("ShellOps stream ended without Exit or Error event")
        }
    } finally {
        try {
            this@toShellOpsEventFlow.close()
        } catch (e: Exception) {
            log(TAG, ERROR) { "Failed to close RemoteInputStream: ${e.asLog()}" }
        }
    }
}

fun Flow<ShellOpsStreamEvent>.toRemoteInputStream(scope: CoroutineScope): RemoteInputStream {
    if (Bugs.isTrace) log(TAG, VERBOSE) { "Flow<ShellOpsStreamEvent>.toRemoteInputStream()..." }

    val inputStream = PipedInputStream(PIPE_BUFFER_SIZE)
    val outputStream = PipedOutputStream()
    inputStream.connect(outputStream)

    val writer = outputStream.writer().buffered(WRITER_BUFFER_SIZE)

    val pending = mutableListOf<ShellOpsStreamEvent>()
    var pendingBytes = 0

    fun flushChunk() {
        if (pending.isEmpty()) return
        val parcel = Parcel.obtain().apply {
            ShellOpsEventsIPCWrapper(pending.toList()).writeToParcel(this, 0)
        }
        val encoded = parcel.marshall().toByteString().base64()
        parcel.recycle()
        writer.apply {
            write(encoded)
            write('\n'.code)
            flush()
        }
        if (Bugs.isTrace) {
            log(TAG, VERBOSE) { "WRITECHUNK: ${pending.size} events to ${encoded.length}B" }
        }
        pending.clear()
        pendingBytes = 0
    }

    this@toRemoteInputStream
        .onEach { event ->
            pending.add(event)
            pendingBytes += event.estimatedParcelSize
            if (pendingBytes >= CHUNK_BYTE_LIMIT) flushChunk()
        }
        .catch { error ->
            log(TAG, ERROR) { "Source flow failed before terminal event: ${error.asLog()}" }
            pending.add(ShellOpsStreamEvent.Error(error.toString()))
            pendingBytes += pending.last().estimatedParcelSize
        }
        .onCompletion {
            try {
                flushChunk()
            } catch (e: Exception) {
                log(TAG, ERROR) { "Flush on completion failed: ${e.asLog()}" }
            } finally {
                try {
                    writer.close()
                } catch (_: Exception) {
                }
            }
        }
        .launchIn(scope)

    return inputStream.remoteInputStream()
}
