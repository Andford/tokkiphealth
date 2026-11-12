package mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.funtouchos

import dagger.Reusable
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.AppCleanerLabelSource
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationExplorer
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.hasApiLevel
import javax.inject.Inject

@Reusable
open class FuntouchOSLabels @Inject constructor(
    private val funtouchOSLabels14Plus: FuntouchOSLabels14Plus,
    private val funtouchOSLabels29Plus: FuntouchOSLabels29Plus,
) : AppCleanerLabelSource {

    fun getStorageEntryDynamic(acsContext: AutomationExplorer.Context): Set<String> = when {
        hasApiLevel(29) -> funtouchOSLabels29Plus.getStorageEntryDynamic(acsContext)
        else -> funtouchOSLabels14Plus.getStorageEntryDynamic(acsContext)
    }

    fun getStorageEntryStatic(acsContext: AutomationExplorer.Context): Set<String> = when {
        hasApiLevel(29) -> funtouchOSLabels29Plus.getStorageEntryStatic(acsContext)
        else -> funtouchOSLabels14Plus.getStorageEntryStatic(acsContext)
    }

    fun getClearCacheDynamic(acsContext: AutomationExplorer.Context): Set<String> = when {
        hasApiLevel(29) -> funtouchOSLabels29Plus.getClearCacheDynamic(acsContext)
        else -> funtouchOSLabels14Plus.getClearCacheDynamic(acsContext)
    }

    fun getClearCacheStatic(acsContext: AutomationExplorer.Context): Set<String> = when {
        hasApiLevel(29) -> funtouchOSLabels29Plus.getClearCacheStatic(acsContext)
        else -> funtouchOSLabels14Plus.getClearCacheStatic(acsContext)
    }

    companion object {
        private val TAG: String = logTag("AppCleaner", "Automation", "FuntouchOS", "Labels")
    }
}