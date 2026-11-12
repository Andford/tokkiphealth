package mx.loperstack.tokkiphealth.appcontrol.core.toggle

import dagger.Reusable
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.sharedresource.HasSharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.SharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.adoptChildResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import javax.inject.Inject

@Reusable
class ComponentToggler @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
    private val pkgOps: PkgOps,
) : HasSharedResource<Any> {

    override val sharedResource = SharedResource.createKeepAlive(TAG, appScope + dispatcherProvider.IO)

    suspend fun changePackageState(installId: InstallId, enabled: Boolean) {
        log(TAG, INFO) { "changePackageState($installId,$enabled)" }
        adoptChildResource(pkgOps)
        pkgOps.changePackageState(installId, enabled)
    }

    companion object {
        private val TAG = logTag("AppControl", "ComponentToggler")
    }
}