package mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.oxygenos

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.AppControlSpecGenerator
import mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.aosp.AOSPLabels
import mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.aosp.AOSPSpecs
import mx.loperstack.tokkiphealth.automation.core.common.ACSNodeInfo
import mx.loperstack.tokkiphealth.automation.core.common.crawl
import mx.loperstack.tokkiphealth.automation.core.common.idMatches
import mx.loperstack.tokkiphealth.automation.core.common.isClickyButton
import mx.loperstack.tokkiphealth.automation.core.common.isEmpty
import mx.loperstack.tokkiphealth.automation.core.common.pkgId
import mx.loperstack.tokkiphealth.automation.core.common.stepper.AutomationStep
import mx.loperstack.tokkiphealth.automation.core.common.stepper.StepContext
import mx.loperstack.tokkiphealth.automation.core.common.stepper.Stepper
import mx.loperstack.tokkiphealth.automation.core.common.stepper.clickNormal
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findClickableParent
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findColumnAlignedClickable
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findNode
import mx.loperstack.tokkiphealth.automation.core.common.textMatchesAny
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationExplorer
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationSpec
import mx.loperstack.tokkiphealth.automation.core.specs.defaultNodeRecovery
import mx.loperstack.tokkiphealth.automation.core.specs.windowCheck
import mx.loperstack.tokkiphealth.automation.core.specs.windowCheckDefaultSettings
import mx.loperstack.tokkiphealth.automation.core.specs.windowLauncherDefaultSettings
import mx.loperstack.tokkiphealth.automation.core.waitForWindowRoot
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.device.DeviceDetective
import mx.loperstack.tokkiphealth.common.device.RomType
import mx.loperstack.tokkiphealth.common.funnel.IPCFunnel
import mx.loperstack.tokkiphealth.common.hasApiLevel
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.progress.withProgress
import mx.loperstack.tokkiphealth.main.core.GeneralSettings
import javax.inject.Inject

@Reusable
class OxygenOSSpecs @Inject constructor(
    private val ipcFunnel: IPCFunnel,
    private val deviceDetective: DeviceDetective,
    private val aospLabels: AOSPLabels,
    private val aospSpecs: AOSPSpecs,
    private val generalSettings: GeneralSettings,
    private val stepper: Stepper,
) : AppControlSpecGenerator {

    override val tag: String = TAG

    override suspend fun isResponsible(pkg: Installed): Boolean {
        val romType = generalSettings.romTypeDetection.value()
        if (romType == RomType.OXYGENOS) return true
        if (romType != RomType.AUTO) return false

        return deviceDetective.getROMType() == RomType.OXYGENOS
    }

    override suspend fun getForceStop(pkg: Installed): AutomationSpec = object : AutomationSpec.Explorer {
        override val tag: String = TAG
        override suspend fun createPlan(): suspend AutomationExplorer.Context.() -> Unit = {
            forceStopPlan(pkg)
        }
    }

    override suspend fun getArchive(pkg: Installed): AutomationSpec = aospSpecs.getArchive(pkg)

    override suspend fun getRestore(pkg: Installed): AutomationSpec = aospSpecs.getRestore(pkg)

    private val forceStopPlan: suspend AutomationExplorer.Context.(Installed) -> Unit = plan@{ pkg ->
        log(TAG, INFO) { "Executing plan for ${pkg.installId} with context $this" }

        val forceStopLabels = aospLabels.getForceStopButtonDynamic(this)
        var wasDisabled = false

        run {
            val action: suspend StepContext.() -> Boolean = action@{
                val candidate = findNode { node ->
                    node.textMatchesAny(forceStopLabels)
                } ?: return@action false

                var target = findClickableParent(maxNesting = 3, includeSelf = true, node = candidate)

                // Android 15+: the action button's icon and label can be separate, unclickable
                // nodes with the tappable wrapper as a sibling (AOSP action-row layout). Match ONLY
                // a clickable in the SAME column as the label, so we never grab an adjacent action
                // such as Uninstall.
                if (target == null && hasApiLevel(35)) {
                    log(TAG, WARN) { "No clickable parent found for $candidate" }
                    target = findColumnAlignedClickable(candidate)
                    if (target != null) {
                        log(TAG, INFO) { "Column-aligned clickable found: $target" }
                    } else if (!candidate.getScreenBounds().isEmpty()) {
                        // On this action row a button is clickable iff enabled. A well-bounded Force
                        // stop label with no column-aligned clickable therefore means the button is
                        // disabled -> the app is already stopped. Bail cleanly instead of clicking a
                        // neighbouring action.
                        log(TAG, WARN) { "No Force stop button in its column, treating as already-stopped: $candidate" }
                        wasDisabled = true
                        return@action true
                    }
                }

                if (target == null) {
                    log(TAG, WARN) { "No clickable target found for $candidate" }
                    return@action false
                }

                if (!target.isEnabled) {
                    wasDisabled = true
                    return@action true
                }

                clickNormal(node = target)
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
                root.crawl().map { it.node }.any { it.isForceStopConfirmDialogNode(titleLbl, forceStopLabels) }
            }

            val action: suspend StepContext.() -> Boolean = action@{
                val root = host.waitForWindowRoot()
                if (root.pkgId != SETTINGS_PKG) return@action false
                val nodes = root.crawl().map { it.node }.toList()
                // The window check ran against an earlier root: without re-checking here, a dialog
                // that vanished mid-retry would let us click App info's own Force stop button,
                // reopen the dialog and report success while the app is still running.
                if (nodes.none { it.isForceStopConfirmDialogNode(titleLbl, forceStopLabels) }) return@action false

                val labels = when (Bugs.isDryRun) {
                    true -> cancelLbl
                    false -> okLbl + forceStopLabels
                }
                // The dialog title can carry the same verb as the confirm button, so prefer an
                // actual button; COUI's own button classes are covered by the isClickable pass.
                val candidate = nodes.firstOrNull { it.isClickyButton() && it.textMatchesAny(labels) }
                    ?: nodes.firstOrNull { it.isClickable && it.textMatchesAny(labels) }
                    ?: nodes.firstOrNull { it.textMatchesAny(labels) && !it.textMatchesAny(titleLbl) }
                    ?: return@action false
                val mapped = findClickableParent(includeSelf = true, node = candidate) ?: return@action false
                clickNormal(isDryRun = Bugs.isDryRun, node = mapped)
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
        @Binds @IntoSet abstract fun mod(mod: OxygenOSSpecs): AppControlSpecGenerator
    }

    companion object {
        val SETTINGS_PKG = "com.android.settings".toPkgId()

        val TAG: String = logTag("AppControl", "Automation", "OxygenOS", "Specs")
    }

}

private const val DIALOG_CONFIRM_BUTTON_ID = "android:id/button1"

/**
 * The OxygenOS force-stop dialog has no title node, only a message and two buttons. Recognize it
 * either by an AOSP-style title, or by the framework AlertDialog positive button carrying the
 * force-stop verb: the id alone would also match unrelated Settings dialogs, the text alone would
 * also match the App info screen's own Force stop button.
 */
private fun ACSNodeInfo.isForceStopConfirmDialogNode(
    titleLabels: Collection<String>,
    forceStopLabels: Collection<String>,
): Boolean = textMatchesAny(titleLabels) ||
        (isClickable && idMatches(DIALOG_CONFIRM_BUTTON_ID) && textMatchesAny(forceStopLabels))
