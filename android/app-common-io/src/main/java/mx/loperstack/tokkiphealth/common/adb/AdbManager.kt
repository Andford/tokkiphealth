package mx.loperstack.tokkiphealth.common.adb

import mx.loperstack.tokkiphealth.common.adb.service.AdbServiceClient
import mx.loperstack.tokkiphealth.common.adb.shizuku.ShizukuManager
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbManager @Inject constructor(
    private val shizukuManager: ShizukuManager,
    val serviceClient: AdbServiceClient,
) {

    suspend fun managerIds(): Set<Pkg.Id> = shizukuManager.managerIds()

    val useAdb = shizukuManager.useShizuku

    companion object {
        private val TAG = logTag("ADB", "Manager")
    }
}