package mx.loperstack.tokkiphealth.appcontrol.core.automation.specs

import android.content.Context
import android.content.res.Resources
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.automation.core.common.AutomationLabelSource
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.isInstalled
import mx.loperstack.tokkiphealth.common.locale.toList
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import javax.inject.Inject

class AppControlLabelDebugger @Inject constructor(
    @ApplicationContext private val context: Context,
) : AutomationLabelSource {

    suspend fun logAllLabels() {
        log(TAG) { "logAllLabels()" }
        SETTINGS_PKGS
            .filter { context.isInstalled(it.name) }
            .forEach { pkgId ->
                ALL_RES_IDS.forEach { resId ->
                    Resources.getSystem().configuration.locales.toList().forEach { locale ->
                        val label = context.get3rdPartyString(pkgId, resId, locale)
                        log(TAG) { "$pkgId: '$resId' -> '$label'" }
                    }
                }
            }
    }

    companion object {
        private val SETTINGS_PKGS = setOf(
            "com.android.settings",
            "com.android.tv.settings",
            "com.miui.securitycenter",
        ).map { it.toPkgId() }
        private val RES_IDS_FORCE_STOP = setOf(
            "menu_item_force_stop",
            "force_stop",
            "force_stop_dlg_title",
            "okay",
            "dlg_ok",
            "cancel",
            "dlg_cancel",
            "device_apps_app_management_force_stop",
        )
        private val RES_IDS_ARCHIVE = setOf(
            "archive",
        )
        private val ALL_RES_IDS = RES_IDS_FORCE_STOP + RES_IDS_ARCHIVE
        private val TAG = logTag("Automation", "AppControlLabelDebugger")
    }
}