package mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.oxygenos

import dagger.Reusable
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.AppCleanerLabelSource
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.aosp.AOSPLabels14Plus
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationExplorer
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import javax.inject.Inject

@Reusable
class OxygenOSLabels14Plus @Inject constructor(
    private val aospLabels14Plus: AOSPLabels14Plus,
) : AppCleanerLabelSource {

    fun getStorageEntryDynamic(
        acsContext: AutomationExplorer.Context
    ): Set<String> = acsContext.getStrings(SETTINGS_PKG, setOf("storage_settings"))

    fun getStorageEntryLabels(
        acsContext: AutomationExplorer.Context
    ): Set<String> = aospLabels14Plus.getStorageEntryStatic(acsContext)

    fun getClearCacheDynamic(
        acsContext: AutomationExplorer.Context
    ): Set<String> = aospLabels14Plus.getClearCacheDynamic(acsContext)

    fun getClearCacheStatic(
        acsContext: AutomationExplorer.Context
    ): Set<String> = aospLabels14Plus.getClearCacheStatic(acsContext)

    companion object {
        private val TAG: String = logTag("AppCleaner", "Automation", "OnePlus", "Labels", "14Plus")
        val SETTINGS_PKG = "com.android.settings".toPkgId()
    }

}
