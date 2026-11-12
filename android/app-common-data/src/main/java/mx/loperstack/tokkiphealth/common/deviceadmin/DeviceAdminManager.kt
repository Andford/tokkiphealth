package mx.loperstack.tokkiphealth.common.deviceadmin

import android.app.admin.DevicePolicyManager
import dagger.Reusable
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import javax.inject.Inject


@Reusable class DeviceAdminManager @Inject constructor(
    private val devicePolicyManager: DevicePolicyManager,
) {

    suspend fun getDeviceAdmins(): Set<Pkg.Id> = devicePolicyManager
        .activeAdmins
        ?.map { it.packageName.toPkgId() }
        ?.toSet()
        ?: emptySet()
}