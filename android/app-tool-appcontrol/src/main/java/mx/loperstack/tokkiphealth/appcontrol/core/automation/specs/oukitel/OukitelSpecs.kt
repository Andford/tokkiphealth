package mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.oukitel

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.automation.core.errors.NoSettingsWindowException
import mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.AppControlSpecGenerator
import mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.aosp.AOSPLabels
import mx.loperstack.tokkiphealth.automation.core.common.ACSNodeInfo
import mx.loperstack.tokkiphealth.automation.core.common.crawl
import mx.loperstack.tokkiphealth.automation.core.common.pkgId
import mx.loperstack.tokkiphealth.automation.core.common.stepper.AutomationStep
import mx.loperstack.tokkiphealth.automation.core.common.stepper.StepContext
import mx.loperstack.tokkiphealth.automation.core.common.stepper.Stepper
import mx.loperstack.tokkiphealth.automation.core.common.stepper.clickNormal
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findClickableParent
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findNode
import mx.loperstack.tokkiphealth.automation.core.common.textMatchesAny
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationExplorer
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationSpec
import mx.loperstack.tokkiphealth.automation.core.specs.defaultFindAndClick
import mx.loperstack.tokkiphealth.automation.core.specs.defaultNodeRecovery
import mx.loperstack.tokkiphealth.automation.core.specs.windowCheck
import mx.loperstack.tokkiphealth.automation.core.specs.windowLauncherDefaultSettings
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.device.DeviceDetective
import mx.loperstack.tokkiphealth.common.device.RomType
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.progress.withProgress
import mx.loperstack.tokkiphealth.main.core.GeneralSettings
import javax.inject.Inject

@Reusable
class OukitelSpecs @Inject constructor(
    private val deviceDetective: DeviceDetective,
    private val aospLabels: AOSPLabels,
    private val generalSettings: GeneralSettings,
    private val stepper: Stepper,
) : AppControlSpecGenerator {

    override val tag: String = TAG

    override suspend fun isResponsible(pkg: Installed): Boolean {
        val romType = generalSettings.romTypeDetection.value()
        if (romType == RomType.OUKITEL) return true
        if (romType != RomType.AUTO) return false

        return deviceDetective.getROMType() == RomType.OUKITEL
    }

    override suspend fun getForceStop(pkg: Installed): AutomationSpec = object : AutomationSpec.Explorer {
        override val tag: String = TAG
        override suspend fun createPlan(): suspend AutomationExplorer.Context.() -> Unit = {
            mainPlan(pkg)
        }
    }

    override suspend fun getArchive(pkg: Installed): AutomationSpec {
        throw UnsupportedOperationException("Archive automation not yet supported on Oukitel")
    }

    override suspend fun getRestore(pkg: Installed): AutomationSpec {
        throw UnsupportedOperationException("Restore automation not yet supported on Oukitel")
    }

    private val mainPlan: suspend AutomationExplorer.Context.(Installed) -> Unit = plan@{ pkg ->
        log(TAG, INFO) { "Executing plan for ${pkg.installId} with context $this" }

        val forceStopLabels = aospLabels.getForceStopButtonDynamic(this)
        var wasDisabled = false

        val windowCheck: suspend StepContext.() -> ACSNodeInfo = {
            if (stepAttempts >= 1 && pkg.hasNoSettings) {
                throw NoSettingsWindowException("${pkg.packageName} has no settings window.")
            }
            val candidates = aospLabels.getSettingsTitleDynamic(hostContext)
            windowCheck { _, root ->
                if (root.pkgId != SETTINGS_PKG) return@windowCheck false
                root.crawl()
                    .map { it.node }
                    .any { toTest -> candidates.any { candidate -> toTest.text == candidate } }
            }()
        }

        run {
            val action: suspend StepContext.() -> Boolean = action@{
                val target = findNode { node ->
                    node.textMatchesAny(forceStopLabels)
                } ?: return@action false

                val mapped = findClickableParent(node = target) ?: return@action false

                if (mapped.isEnabled) {
                    clickNormal(node = mapped)
                } else {
                    wasDisabled = true
                    true
                }
            }

            val step = AutomationStep(
                source = TAG,
                descriptionInternal = "Force stop button for $pkg",
                label = mx.loperstack.tokkiphealth.appcontrol.R.string.appcontrol_automation_progress_find_force_stop.toCaString(forceStopLabels),
                windowLaunch = windowLauncherDefaultSettings(pkg),
                windowCheck = windowCheck,
                nodeRecovery = defaultNodeRecovery(pkg),
                nodeAction = action,
            )
            stepper.withProgress(this) { process(this@plan, step) }
        }

        if (wasDisabled) {
            log(TAG) { "Force stop button was disabled, app is already stopped." }
            return@plan
        }

        run {
            val titleLbl = aospLabels.getForceStopDialogTitleDynamic(this) + forceStopLabels.map { "$it?" }
            val okLbl = aospLabels.getForceStopDialogOkDynamic(this)
            val cancelLbl = aospLabels.getForceStopDialogCancelDynamic(this)

            val windowCheck = windowCheck { _, root ->
                if (root.pkgId != SETTINGS_PKG) return@windowCheck false
                root.crawl().map { it.node }.any { subNode -> subNode.textMatchesAny(titleLbl) }
            }

            val action = defaultFindAndClick(
                finder = {
                    findNode { node ->
                        when (Bugs.isDryRun) {
                            true -> node.textMatchesAny(cancelLbl)
                            false -> node.textMatchesAny(okLbl)
                        }
                    }
                }
            )

            val step = AutomationStep(
                source = TAG,
                descriptionInternal = "Confirm force stop button for $pkg",
                label = mx.loperstack.tokkiphealth.automation.R.string.automation_progress_find_ok_confirmation.toCaString(titleLbl + okLbl),
                windowCheck = windowCheck,
                nodeAction = action,
            )
            stepper.withProgress(this) { process(this@plan, step) }
        }
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: OukitelSpecs): AppControlSpecGenerator
    }

    companion object {
        val SETTINGS_PKG = "com.android.settings".toPkgId()

        val TAG: String = logTag("AppControl", "Automation", "OUKITEL", "Specs")
    }

}
