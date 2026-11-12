package mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.realme

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.appcleaner.R
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.AppCleanerSpecGenerator
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.StorageEntryFinder
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.clickClearCache
import mx.loperstack.tokkiphealth.automation.core.common.isClickyButton
import mx.loperstack.tokkiphealth.automation.core.common.isEmpty
import mx.loperstack.tokkiphealth.automation.core.common.pkgId
import mx.loperstack.tokkiphealth.automation.core.common.scrollNodeIntoView
import mx.loperstack.tokkiphealth.automation.core.common.stepper.AutomationStep
import mx.loperstack.tokkiphealth.automation.core.common.stepper.StepContext
import mx.loperstack.tokkiphealth.automation.core.common.stepper.Stepper
import mx.loperstack.tokkiphealth.automation.core.common.stepper.clickGesture
import mx.loperstack.tokkiphealth.automation.core.common.stepper.clickNormal
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findClickableParent
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findNodeByLabel
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationExplorer
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationSpec
import mx.loperstack.tokkiphealth.automation.core.specs.defaultNodeRecovery
import mx.loperstack.tokkiphealth.automation.core.specs.interferenceAware
import mx.loperstack.tokkiphealth.automation.core.specs.windowCheck
import mx.loperstack.tokkiphealth.automation.core.specs.windowCheckDefaultSettings
import mx.loperstack.tokkiphealth.automation.core.specs.windowLauncherDefaultSettings
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.debug.toVisualStrings
import mx.loperstack.tokkiphealth.common.device.DeviceDetective
import mx.loperstack.tokkiphealth.common.device.RomType
import mx.loperstack.tokkiphealth.common.funnel.IPCFunnel
import mx.loperstack.tokkiphealth.common.hasApiLevel
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.progress.withProgress
import mx.loperstack.tokkiphealth.common.device.RomTypeProvider
import kotlinx.coroutines.delay
import javax.inject.Inject

@Reusable
class RealmeSpecs @Inject constructor(
    ipcFunnel: IPCFunnel,
    private val deviceDetective: DeviceDetective,
    private val realmeLabels: RealmeLabels,
    private val storageEntryFinder: StorageEntryFinder,
    private val romTypeProvider: RomTypeProvider,
    private val stepper: Stepper,
) : AppCleanerSpecGenerator {

    override val tag: String = TAG

    // https://github.com/d4rken/sdmaid-public/issues/3040
    override suspend fun isResponsible(pkg: Installed): Boolean {
        val romType = romTypeProvider.getRomType()
        if (romType == RomType.REALMEUI) return true
        if (romType != RomType.AUTO) return false

        return deviceDetective.getROMType() == RomType.REALMEUI
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
            val storageEntryLabels =
                realmeLabels.getStorageEntryDynamic(this) + realmeLabels.getStorageEntryLabels(this)
            log(TAG) { "storageEntryLabels=${storageEntryLabels.toVisualStrings()}" }

            val storageFinder = storageEntryFinder.storageFinderAOSP(storageEntryLabels, pkg)

            val action: suspend StepContext.() -> Boolean = action@{
                val target = storageFinder(this) ?: return@action false
                log(TAG) { "Found target $target" }
                when {
                    hasApiLevel(35) -> {
                        val mapped = findClickableParent(maxNesting = 3, node = target)
                        if (mapped != null) {
                            clickNormal(node = mapped)
                        } else {
                            // No clickable parent (e.g. Compose-based settings): we must tap by
                            // coordinates. If the row is clipped behind a system bar its bounds are
                            // degenerate and a tap would hit the navigation bar.
                            log(TAG, WARN) { "Target has no clickable parent, trying gesture..." }
                            if (target.getScreenBounds().isEmpty()) {
                                // Bring it on-screen, then re-find on a fresh tree. If it's still
                                // clipped, fail so node recovery scrolls and the step retries —
                                // never tap degenerate bounds (that lands on the navigation bar).
                                log(TAG, WARN) { "Target bounds are clipped, scrolling into view: $target" }
                                target.scrollNodeIntoView()
                                delay(250)
                                val refreshed = storageFinder(this) ?: return@action false
                                if (refreshed.getScreenBounds().isEmpty()) {
                                    log(TAG, WARN) { "Target still clipped after scroll, retrying: $refreshed" }
                                    return@action false
                                }
                                clickGesture(node = refreshed)
                            } else {
                                clickGesture(node = target)
                            }
                        }
                    }

                    else -> {
                        val mapped = findClickableParent(maxNesting = 6, node = target) ?: return@action false
                        clickNormal(isDryRun = false, mapped)
                    }
                }
            }

            val step = AutomationStep(
                source = TAG,
                descriptionInternal = "Storage entry for $pkg",
                label = R.string.appcleaner_automation_progress_find_storage.toCaString(storageEntryLabels),
                windowLaunch = windowLauncherDefaultSettings(pkg),
                windowCheck = windowCheckDefaultSettings(SETTINGS_PKG, ipcFunnel, pkg),
                nodeRecovery = defaultNodeRecovery(pkg),
                nodeAction = action,
            )
            stepper.withProgress(this) { process(this@plan, step) }
        }

        run {
            val clearCacheButtonLabels =
                realmeLabels.getClearCacheDynamic(this) + realmeLabels.getClearCacheLabels(this)
            log(TAG) { "clearCacheButtonLabels=${clearCacheButtonLabels.toVisualStrings()}" }

            val action: suspend StepContext.() -> Boolean = action@{
                val target = when {
                    hasApiLevel(35) -> findNodeByLabel(clearCacheButtonLabels)
                    else -> findNodeByLabel(clearCacheButtonLabels) { it.isClickyButton() }
                } ?: return@action false

                // On API 35+, button text may be nested in non-clickable wrapper, find clickable parent.
                // If no clickable parent exists (e.g., disabled button when cache=0), fall back to target
                // and let clickClearCache() handle the disabled state.
                val mapped = when {
                    hasApiLevel(35) && !target.isClickyButton() -> findClickableParent(node = target) ?: target
                    else -> target
                }

                clickClearCache(isDryRun = Bugs.isDryRun, pkg, node = mapped)
            }


            val step = AutomationStep(
                source = TAG,
                descriptionInternal = "Clear cache for $pkg",
                label = R.string.appcleaner_automation_progress_find_clear_cache.toCaString(clearCacheButtonLabels),
                windowCheck = windowCheck(
                    interferenceAware(
                        expectedPkgs = setOf(SETTINGS_PKG),
                        targetPkg = pkg.id,
                        ipcFunnel = ipcFunnel,
                    ) { _, root -> root.pkgId == SETTINGS_PKG }
                ),
                nodeAction = action,
            )
            stepper.withProgress(this) { process(this@plan, step) }
        }
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: RealmeSpecs): AppCleanerSpecGenerator
    }

    companion object {
        private val TAG: String = logTag("AppCleaner", "Automation", "Realme", "Specs")
        val SETTINGS_PKG = "com.android.settings".toPkgId()
    }
}