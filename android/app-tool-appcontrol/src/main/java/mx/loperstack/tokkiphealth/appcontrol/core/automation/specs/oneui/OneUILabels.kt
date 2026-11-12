package mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.oneui

import dagger.Reusable
import mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.AppControlLabelSource
import mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.aosp.AOSPLabels
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationExplorer
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import javax.inject.Inject

@Reusable
open class OneUILabels @Inject constructor() : AppControlLabelSource {
    fun getForceStopButtonDynamic(
        acsContext: AutomationExplorer.Context
    ): Set<String> = acsContext.getStrings(AOSPLabels.SETTINGS_PKG, setOf("force_stop"))

    fun getForceStopDialogTitleDynamic(
        acsContext: AutomationExplorer.Context
    ): Set<String> = acsContext.getStrings(AOSPLabels.SETTINGS_PKG, setOf("force_stop_dlg_title"))

    fun getForceStopDialogOkDynamic(
        acsContext: AutomationExplorer.Context
    ): Set<String> = acsContext.getStrings(AOSPLabels.SETTINGS_PKG, setOf("okay", "dlg_ok"))

    fun getForceStopDialogCancelDynamic(
        acsContext: AutomationExplorer.Context
    ): Set<String> = acsContext.getStrings(AOSPLabels.SETTINGS_PKG, setOf("cancel", "dlg_cancel"))

    companion object {
        val TAG: String = logTag("AppControl", "Automation", "OneUI", "Labels")
    }
}