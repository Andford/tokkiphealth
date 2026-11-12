package mx.loperstack.tokkiphealth.common.files

import mx.loperstack.tokkiphealth.common.ipc.RemoteInputStream
import mx.loperstack.tokkiphealth.common.ipc.remoteInputStream
import okio.Source
import okio.buffer
import java.io.InputStream

fun Source.inputStream(): InputStream {
    return buffer().inputStream()
}

fun Source.remoteInputStream(): RemoteInputStream {
    return inputStream().remoteInputStream()
}