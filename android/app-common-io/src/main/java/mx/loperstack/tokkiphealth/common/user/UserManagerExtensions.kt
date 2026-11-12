package mx.loperstack.tokkiphealth.common.user

import android.os.UserHandle
import mx.loperstack.tokkiphealth.common.BuildConfigWrap
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.user.UserManager2.Companion.TAG


suspend fun UserManager2.ourInstall() = InstallId(
    pkgId = BuildConfigWrap.APPLICATION_ID.toPkgId(),
    userHandle = currentUser().handle
)


internal fun UserHandle.getIdentifier(): Int? {
    return try {
        val getIdentifier = this.javaClass.getMethod("getIdentifier")
        getIdentifier.invoke(this) as Int
    } catch (e: Exception) {
        log(TAG, WARN) { "UserHandle.getIdentifier(): Failed to use reflective access on getIdentifier: ${e.asLog()} " }
        null
    }
}