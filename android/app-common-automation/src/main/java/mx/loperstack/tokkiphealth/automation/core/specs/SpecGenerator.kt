package mx.loperstack.tokkiphealth.automation.core.specs

import android.content.Context
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed

interface SpecGenerator {

    val tag: String

    suspend fun isResponsible(pkg: Installed): Boolean

    fun Context.get3rdPartyString(pkgId: Pkg.Id, stringIdName: String): String? = try {
        val appResources = packageManager.getResourcesForApplication(pkgId.name)

        @Suppress("DiscouragedApi")
        val identifier = appResources.getIdentifier(stringIdName, "string", pkgId.name).takeIf { it != 0 }
        identifier?.let { appResources.getString(it) }.also {
            if (it != null) {
                log(TAG) { "Read ${pkgId.name}:${stringIdName} from settings APK: $it" }
            } else {
                log(TAG, WARN) { "Failed to read ${pkgId.name}:${stringIdName} from settings APK." }
            }
        }
    } catch (e: Exception) {
        log(TAG, ERROR) { "get3rdPartyString(${pkgId.name}, $stringIdName) failed: ${e.asLog()}" }
        null
    }
}


private val TAG = logTag("Automation", "SpecGenerator")
