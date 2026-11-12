package mx.loperstack.tokkiphealth.squeezer.core.history

import dagger.Reusable
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.hashing.Hasher
import mx.loperstack.tokkiphealth.common.hashing.hash
import mx.loperstack.tokkiphealth.squeezer.core.ContentId
import mx.loperstack.tokkiphealth.squeezer.core.ContentIdentifier
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Reusable
class ImageContentHasher @Inject constructor(
    private val gatewaySwitch: GatewaySwitch,
    private val dispatcherProvider: DispatcherProvider,
) {

    suspend fun computeHash(path: APath): ContentIdentifier.ImageHash = withContext(dispatcherProvider.IO) {
        val hash = gatewaySwitch.file(path, readWrite = false).source().hash(Hasher.Type.SHA256).format()
        ContentIdentifier.ImageHash(ContentId(hash))
    }
}
