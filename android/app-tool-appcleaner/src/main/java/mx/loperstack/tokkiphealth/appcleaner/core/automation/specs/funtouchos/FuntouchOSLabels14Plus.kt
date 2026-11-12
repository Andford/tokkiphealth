package mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.funtouchos

import dagger.Reusable
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.aosp.AOSPLabels14Plus
import mx.loperstack.tokkiphealth.automation.core.common.AutomationLabelSource
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationExplorer
import mx.loperstack.tokkiphealth.automation.core.specs.getLocales
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import javax.inject.Inject

@Reusable
class FuntouchOSLabels14Plus @Inject constructor(
    private val labels14Plus: AOSPLabels14Plus,
) : AutomationLabelSource {

    fun getStorageEntryDynamic(
        acsContext: AutomationExplorer.Context
    ): Set<String> = labels14Plus.getStorageEntryDynamic(acsContext)

    fun getStorageEntryStatic(
        acsContext: AutomationExplorer.Context
    ): Set<String> = labels14Plus.getStorageEntryStatic(acsContext)

    fun getClearCacheDynamic(
        acsContext: AutomationExplorer.Context
    ): Set<String> = labels14Plus.getClearCacheDynamic(acsContext)

    fun getClearCacheStatic(
        acsContext: AutomationExplorer.Context
    ): Set<String> = acsContext.getLocales()
        .map { it.language to it.script }
        .mapNotNull { (lang, _) ->
            when {
                // vivo/1808/1808:8.1.0/O11019/1592484538:user/release-keys
                // https://github.com/d4rken/sdmaid-public/issues/3670
                "in".toLang() == lang -> setOf("Bersihkan cache")
                else -> null
            }
        }
        .flatten()
        .append { labels14Plus.getClearCacheStatic(acsContext) }
        .toSet()

    companion object {
        private val TAG: String = logTag("AppCleaner", "Automation", "FuntouchOS", "Labels", "14Plus")
    }
}