package mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.originos

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.AppControlSpecGenerator
import mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.aosp.AOSPLabels
import mx.loperstack.tokkiphealth.automation.core.common.crawl
import mx.loperstack.tokkiphealth.automation.core.common.findParentOrNull
import mx.loperstack.tokkiphealth.automation.core.common.isClickyButton
import mx.loperstack.tokkiphealth.automation.core.common.pkgId
import mx.loperstack.tokkiphealth.automation.core.common.stepper.AutomationStep
import mx.loperstack.tokkiphealth.automation.core.common.stepper.StepContext
import mx.loperstack.tokkiphealth.automation.core.common.stepper.Stepper
import mx.loperstack.tokkiphealth.automation.core.common.stepper.clickGesture
import mx.loperstack.tokkiphealth.automation.core.common.stepper.clickNormal
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findClickableParent
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findNode
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findNodeByContentDesc
import mx.loperstack.tokkiphealth.automation.core.common.textMatchesAny
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationExplorer
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationSpec
import mx.loperstack.tokkiphealth.automation.core.specs.defaultNodeRecovery
import mx.loperstack.tokkiphealth.automation.core.specs.windowCheck
import mx.loperstack.tokkiphealth.automation.core.specs.windowCheckDefaultSettings
import mx.loperstack.tokkiphealth.automation.core.specs.windowLauncherDefaultSettings
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.device.DeviceDetective
import mx.loperstack.tokkiphealth.common.device.RomType
import mx.loperstack.tokkiphealth.common.funnel.IPCFunnel
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.progress.withProgress
import mx.loperstack.tokkiphealth.main.core.GeneralSettings
import javax.inject.Inject

@Reusable
class OriginOSSpecs @Inject constructor(
    private val ipcFunnel: IPCFunnel,
    private val deviceDetective: DeviceDetective,
    private val aospLabels: AOSPLabels,
    private val generalSettings: GeneralSettings,
    private val stepper: Stepper,
) : AppControlSpecGenerator {

    override val tag: String = TAG

    override suspend fun isResponsible(pkg: Installed): Boolean {
        val romType = generalSettings.romTypeDetection.value()
        if (romType == RomType.ORIGINOS) return true
        if (romType != RomType.AUTO) return false

        return deviceDetective.getROMType() == RomType.ORIGINOS
    }

    override suspend fun getForceStop(pkg: Installed): AutomationSpec = object : AutomationSpec.Explorer {
        override val tag: String = TAG
        override suspend fun createPlan(): suspend AutomationExplorer.Context.() -> Unit = {
            forceStopPlan(pkg)
        }
    }

    override suspend fun getArchive(pkg: Installed): AutomationSpec {
        throw UnsupportedOperationException("Archive automation not yet supported on OriginOS")
    }

    override suspend fun getRestore(pkg: Installed): AutomationSpec {
        throw UnsupportedOperationException("Restore automation not yet supported on OriginOS")
    }

    private val forceStopPlan: suspend AutomationExplorer.Context.(Installed) -> Unit = plan@{ pkg ->
        log(TAG, INFO) { "Executing plan for ${pkg.installId} with context $this" }

        val forceStopLabels = aospLabels.getForceStopButtonDynamic(this)
        var wasDisabled = false

        run {
            val action: suspend StepContext.() -> Boolean = action@{
                // OriginOS action buttons are nested "vbuttons": the semantic Button
                // (id=right_button) carries the label in contentDescription and the real enabled
                // state, but wraps an always-clickable, always-enabled LinearLayout holding the
                // label TextView (id=vbutton_title). Resolve the Button, never the wrapper.
                val target = findNodeByContentDesc(forceStopLabels) { it.isClickyButton() }
                    ?: findNode { it.textMatchesAny(forceStopLabels) }?.let { label ->
                        if (label.isClickyButton()) label
                        else label.findParentOrNull(maxNesting = 4) { it.isClickyButton() }
                    }
                    ?: return@action false

                if (!target.isEnabled) {
                    wasDisabled = true
                    return@action true
                }

                // Tap via gesture instead of ACTION_CLICK: on OriginOS an accessibility click on
                // these vbuttons emits TYPE_VIEW_CLICKED but never triggers the handler, so the
                // confirmation dialog never opens. A dispatched tap at the button reliably fires it.
                clickGesture(node = target)
            }

            val step = AutomationStep(
                source = TAG,
                descriptionInternal = "Force stop button for $pkg",
                label = mx.loperstack.tokkiphealth.appcontrol.R.string.appcontrol_automation_progress_find_force_stop.toCaString(forceStopLabels),
                windowLaunch = windowLauncherDefaultSettings(pkg),
                windowCheck = windowCheckDefaultSettings(SETTINGS_PKG, ipcFunnel, pkg),
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

            val action: suspend StepContext.() -> Boolean = action@{
                // The confirm button may be a plain Button with text, or another vbutton whose
                // label sits in a child TextView. Match the label, exclude the dialog title
                // (which can carry the same text), then click the nearest clickable.
                val labels = when (Bugs.isDryRun) {
                    true -> cancelLbl
                    false -> okLbl + forceStopLabels
                }
                val candidate = findNode { node ->
                    node.textMatchesAny(labels) && !node.textMatchesAny(titleLbl)
                } ?: return@action false
                val mapped = when {
                    candidate.isClickyButton() -> candidate
                    else -> candidate.findParentOrNull(maxNesting = 4) { it.isClickyButton() }
                        ?: findClickableParent(includeSelf = true, node = candidate)
                        ?: return@action false
                }
                clickNormal(node = mapped)
            }

            val step = AutomationStep(
                source = TAG,
                descriptionInternal = "Confirm force stop for $pkg",
                label = mx.loperstack.tokkiphealth.automation.R.string.automation_progress_find_ok_confirmation.toCaString(titleLbl + okLbl + forceStopLabels),
                windowCheck = windowCheck,
                nodeAction = action,
            )
            stepper.withProgress(this) { process(this@plan, step) }
        }
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: OriginOSSpecs): AppControlSpecGenerator
    }

    companion object {
        val SETTINGS_PKG = "com.android.settings".toPkgId()

        val TAG: String = logTag("AppControl", "Automation", "OriginOS", "Specs")
    }

}
