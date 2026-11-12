package mx.loperstack.tokkiphealth.common.files.local.ipc

import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.files.remoteInputStream
import mx.loperstack.tokkiphealth.common.ipc.RemoteInputStream
import okio.Source

data class DetailedInputSourceWrap(
    val path: LocalPath,
    val input: Source,
    val length: Long = -1
) : DetailedInputSource.Stub() {

    override fun path(): LocalPath = path

    override fun input(): RemoteInputStream = input.remoteInputStream()

    override fun length(): Long = length

}