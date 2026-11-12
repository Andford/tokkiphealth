package mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.oukitel

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.appcleaner.R
import mx.loperstack.tokkiphealth.automation.core.errors.NoSettingsWindowException
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.AppCleanerSpecGenerator
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.StorageEntryFinder
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.aosp.AOSPLabels
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.clickClearCache
import mx.loperstack.tokkiphealth.automation.core.common.ACSNodeInfo
import mx.loperstack.tokkiphealth.automation.core.common.crawl
import mx.loperstack.tokkiphealth.automation.core.common.isClickyButton
import mx.loperstack.tokkiphealth.automation.core.common.pkgId
import mx.loperstack.tokkiphealth.automation.core.common.stepper.AutomationStep
import mx.loperstack.tokkiphealth.automation.core.common.stepper.StepContext
import mx.loperstack.tokkiphealth.automation.core.common.stepper.Stepper
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findNodeByLabel
import mx.loperstack.tokkiphealth.automation.core.common.textMatchesAny
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationExplorer
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationSpec
import mx.loperstack.tokkiphealth.automation.core.specs.defaultFindAndClick
import mx.loperstack.tokkiphealth.automation.core.specs.defaultNodeRecovery
import mx.loperstack.tokkiphealth.automation.core.specs.windowCheck
import mx.loperstack.tokkiphealth.automation.core.specs.windowLauncherDefaultSettings
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.debug.toVisualStrings
import mx.loperstack.tokkiphealth.common.device.DeviceDetective
import mx.loperstack.tokkiphealth.common.device.RomType
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.progress.withProgress
import mx.loperstack.tokkiphealth.common.device.RomTypeProvider
import javax.inject.Inject

@Reusable
class OukitelSpecs @Inject constructor(
    private val deviceDetective: DeviceDetective,
    private val aospLabels: AOSPLabels,
    private val storageEntryFinder: StorageEntryFinder,
    private val romTypeProvider: RomTypeProvider,
    private val stepper: Stepper,
) : AppCleanerSpecGenerator {

    override val tag: String = TAG

    override suspend fun isResponsible(pkg: Installed): Boolean {
        val romType = romTypeProvider.getRomType()
        if (romType == RomType.OUKITEL) return true
        if (romType != RomType.AUTO) return false

        return deviceDetective.getROMType() == RomType.OUKITEL
    }

    override suspend fun getClearCache(pkg: Installed): AutomationSpec = object : AutomationSpec.Explorer {
        override val tag: String = TAG
        override suspend fun createPlan(): suspend AutomationExplorer.Context.() -> Unit = {
            mainPlan(pkg)
        }
    }

    private val mainPlan: suspend AutomationExplorer.Context.(Installed) -> Unit = plan@{ pkg ->
        log(TAG, INFO) { "Executing plan for ${pkg.installId} with context $this" }

        run {
            val windowCheck: suspend StepContext.() -> ACSNodeInfo = {
                if (stepAttempts >= 1 && pkg.hasNoSettings) {
                    throw NoSettingsWindowException("${pkg.packageName} has no settings window.")
                }
                val candidates = aospLabels.getSettingsTitleDynamic(hostContext)
                windowCheck { _, root ->
                    if (root.pkgId != SETTINGS_PKG) return@windowCheck false
                    root.crawl().map { it.node }.any { it.textMatchesAny(candidates) }
                }()
            }

            val storageEntryLabels =
                aospLabels.getStorageEntryDynamic(this) + aospLabels.getStorageEntryStatic(this)
            log(TAG) { "storageEntryLabels=${storageEntryLabels.toVisualStrings()}" }

            val storageFinder = storageEntryFinder.storageFinderAOSP(storageEntryLabels, pkg)

            val step = AutomationStep(
                source = tag,
                descriptionInternal = "Storage entry for $pkg",
                label = R.string.appcleaner_automation_progress_find_storage.toCaString(storageEntryLabels),
                windowLaunch = windowLauncherDefaultSettings(pkg),
                windowCheck = windowCheck,
                nodeRecovery = defaultNodeRecovery(pkg),
                nodeAction = defaultFindAndClick(finder = storageFinder),
            )
            stepper.withProgress(this) { process(this@plan, step) }
        }

        run {
            val clearCacheButtonLabels =
                aospLabels.getClearCacheDynamic(this) + aospLabels.getClearCacheStatic(this)
            log(TAG) { "clearCacheButtonLabels=${clearCacheButtonLabels.toVisualStrings()}" }

            val windowCheck: suspend StepContext.() -> ACSNodeInfo = {
                windowCheck { _, root ->
                    if (root.pkgId != SETTINGS_PKG) return@windowCheck false
                    root.crawl().map { it.node }.any { it.textMatchesAny(clearCacheButtonLabels) }
                }()
            }

            val action: suspend StepContext.() -> Boolean = action@{
                val target = findNodeByLabel(clearCacheButtonLabels) { it.isClickyButton() } ?: return@action false
                clickClearCache(isDryRun = Bugs.isDryRun, pkg, node = target)
            }

            val step = AutomationStep(
                source = tag,
                descriptionInternal = "Clear cache button for $pkg",
                label = R.string.appcleaner_automation_progress_find_clear_cache.toCaString(clearCacheButtonLabels),
                windowCheck = windowCheck,
                nodeAction = action,
            )
            stepper.withProgress(this) { process(this@plan, step) }
        }
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: OukitelSpecs): AppCleanerSpecGenerator
    }

    companion object {
        val SETTINGS_PKG = "com.android.settings".toPkgId()
        private val TAG: String = logTag("AppCleaner", "Automation", "OUKITEL", "Specs")
    }

}
