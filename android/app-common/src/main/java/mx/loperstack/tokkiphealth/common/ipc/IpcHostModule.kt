package mx.loperstack.tokkiphealth.common.ipc

import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import kotlin.io.encoding.Base64

interface IpcHostModule {

    fun Array<StackTraceElement>.encodeBase64(): String? = try {
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use {
            it.writeObject(this)
        }
        Base64.encode(baos.toByteArray())
    } catch (e: Exception) {
        null
    }


    // Not all exception can be passed through the binder
    // See Parcel.writeException(...)
    fun Throwable.wrapToPropagate(): Exception {
        val msgBuilder = StringBuilder()
        msgBuilder.append("${this.javaClass.name}: $message")
        cause?.let {
            msgBuilder.append("\nCaused by: ")
            msgBuilder.append(it.toString())
        }

        if (Bugs.isDebug) {
            log(TAG, VERBOSE) { "Encoding stacktrace..." }
            // TODO Find better way to pass trace, see IpcClientModule
            val encodedTrace = stackTrace.encodeBase64()
            if (encodedTrace != null) {
                msgBuilder
                    .append("\n\n")
                    .append(STACK_MARKER)
                    .append(encodedTrace)
            }
        }

        return UnsupportedOperationException(msgBuilder.toString())
    }

    companion object {
        const val STACK_MARKER = "#STACK#:"
    }
}


private val TAG = logTag("IPC", "HostModule")
