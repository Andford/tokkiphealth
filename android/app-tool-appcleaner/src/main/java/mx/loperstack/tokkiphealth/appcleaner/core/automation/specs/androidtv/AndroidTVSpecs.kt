package mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.androidtv

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.appcleaner.R
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.AppCleanerSpecGenerator
import mx.loperstack.tokkiphealth.appcleaner.core.automation.specs.clickClearCache
import mx.loperstack.tokkiphealth.automation.core.common.crawl
import mx.loperstack.tokkiphealth.automation.core.common.idMatches
import mx.loperstack.tokkiphealth.automation.core.common.pkgId
import mx.loperstack.tokkiphealth.automation.core.common.scrollNode
import mx.loperstack.tokkiphealth.automation.core.common.stepper.AutomationStep
import mx.loperstack.tokkiphealth.automation.core.common.stepper.StepContext
import mx.loperstack.tokkiphealth.automation.core.common.stepper.Stepper
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findClickableParent
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findNode
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findNodeByLabel
import mx.loperstack.tokkiphealth.automation.core.common.textMatchesAny
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationExplorer
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationSpec
import mx.loperstack.tokkiphealth.automation.core.specs.defaultFindAndClick
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
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.progress.withProgress
import mx.loperstack.tokkiphealth.common.device.RomTypeProvider
import javax.inject.Inject

@Reusable
open class AndroidTVSpecs @Inject constructor(
    private val ipcFunnel: IPCFunnel,
    @ApplicationContext private val context: Context,
    private val deviceDetective: DeviceDetective,
    private val androidTVLabels: AndroidTVLabels,
    private val romTypeProvider: RomTypeProvider,
    private val stepper: Stepper,
) : AppCleanerSpecGenerator {

    override val tag: String = TAG

    override suspend fun isResponsible(pkg: Installed): Boolean {
        val romType = romTypeProvider.getRomType()
        if (romType == RomType.ANDROID_TV) return true
        if (romType != RomType.AUTO) return false

        return deviceDetective.getROMType() == RomType.ANDROID_TV
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
            val clearCacheButtonLabels = androidTVLabels.getClearCacheLabels(this)
            log(TAG) { "clearCacheButtonLabels=${clearCacheButtonLabels.toVisualStrings()}" }

            if (clearCacheButtonLabels.isEmpty()) {
                log(TAG, WARN) { "clearCacheButtonLabels was empty" }
                throw UnsupportedOperationException("This system language is not supported")
            }

            val action: suspend StepContext.() -> Boolean = action@{
                val target = findNodeByLabel(clearCacheButtonLabels) {
                    it.idMatches("android:id/title")
                } ?: return@action false

                val mapped = findClickableParent(node = target) ?: return@action false

                clickClearCache(isDryRun = false, pkg, node = mapped)
            }
            val step = AutomationStep(
                source = TAG,
                descriptionInternal = "Clear cache button for $pkg",
                label = R.string.appcleaner_automation_progress_find_clear_cache.toCaString(clearCacheButtonLabels),
                windowLaunch = windowLauncherDefaultSettings(pkg),
                windowCheck = windowCheckDefaultSettings(SETTINGS_PKG, ipcFunnel, pkg),
                nodeRecovery = { it.scrollNode() },
                nodeAction = action,
            )
            stepper.withProgress(this) { process(this@plan, step) }
        }

        run {
            val clearCacheTexts = androidTVLabels.getClearCacheLabels(this)
            log(TAG) { "clearCacheTexts=${clearCacheTexts.toVisualStrings()}" }

            val windowCheck = windowCheck { _, root ->
                if (root.pkgId != SETTINGS_PKG) return@windowCheck false
                root.crawl().map { it.node }.any { subNode ->
                    when {
                        subNode.idMatches("com.android.tv.settings:id/guidance_title") -> when {
                            subNode.textMatchesAny(clearCacheTexts) -> true
                            subNode.textMatchesAny(clearCacheTexts.map { it.replace("?", "") }) -> true
                            subNode.textMatchesAny(clearCacheTexts.map { "$it?" }) -> true
                            else -> false
                        }

                        subNode.idMatches("com.android.tv.settings:id/guidance_container") -> true
                        else -> false
                    }
                }
            }

            val buttonLabels = setOf(context.getString(android.R.string.ok))

            val action = defaultFindAndClick(
                isDryRun = Bugs.isDryRun,
                finder = {
                    findNode { node ->
                        when {
                            node.idMatches("com.android.tv.settings:id/guidedactions_item_content") -> true
                            node.idMatches("com.android.tv.settings:id/guidedactions_item_title") -> {
                                node.textMatchesAny(buttonLabels)
                            }

                            else -> false
                        }
                    }
                }
            )

            val step = AutomationStep(
                source = TAG,
                descriptionInternal = "Confirm action for $pkg",
                label = mx.loperstack.tokkiphealth.automation.R.string.automation_progress_find_ok_confirmation.toCaString(buttonLabels),
                windowCheck = windowCheck,
                nodeAction = action,
            )
            stepper.withProgress(this) { process(this@plan, step) }
        }
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: AndroidTVSpecs): AppCleanerSpecGenerator
    }

    companion object {
        private val TAG: String = logTag("AppCleaner", "Automation", "AndroidTV", "Specs")
        val SETTINGS_PKG = "com.android.tv.settings".toPkgId()

    }
}
