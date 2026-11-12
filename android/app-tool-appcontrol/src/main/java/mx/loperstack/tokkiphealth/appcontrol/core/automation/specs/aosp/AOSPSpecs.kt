package mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.aosp

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.appcontrol.core.archive.ArchiveUnavailableException
import mx.loperstack.tokkiphealth.appcontrol.core.automation.specs.AppControlSpecGenerator
import mx.loperstack.tokkiphealth.appcontrol.core.restore.RestoreUnavailableException
import mx.loperstack.tokkiphealth.automation.core.common.ACSNodeInfo
import mx.loperstack.tokkiphealth.automation.core.common.crawl
import mx.loperstack.tokkiphealth.automation.core.common.isEmpty
import mx.loperstack.tokkiphealth.automation.core.common.pkgId
import mx.loperstack.tokkiphealth.automation.core.common.stepper.AutomationStep
import mx.loperstack.tokkiphealth.automation.core.common.stepper.StepContext
import mx.loperstack.tokkiphealth.automation.core.common.stepper.Stepper
import mx.loperstack.tokkiphealth.automation.core.common.stepper.awaitEnabled
import mx.loperstack.tokkiphealth.automation.core.common.stepper.clickNormal
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findClickableParent
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findColumnAlignedClickable
import mx.loperstack.tokkiphealth.automation.core.common.stepper.findNode
import mx.loperstack.tokkiphealth.automation.core.common.textMatchesAny
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationExplorer
import mx.loperstack.tokkiphealth.automation.core.specs.AutomationSpec
import mx.loperstack.tokkiphealth.automation.core.specs.defaultFindAndClick
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
class AOSPSpecs @Inject constructor(
    private val ipcFunnel: IPCFunnel,
    private val deviceDetective: DeviceDetective,
    private val aospLabels: AOSPLabels,
    private val generalSettings: GeneralSettings,
    private val stepper: Stepper,
) : AppControlSpecGenerator {

    override val tag: String = TAG

    override suspend fun isResponsible(pkg: Installed): Boolean {
        val romType = generalSettings.romTypeDetection.value()
        if (romType == RomType.AOSP) return true
        if (romType != RomType.AUTO) return false

        return deviceDetective.getROMType() == RomType.AOSP
    }

    override suspend fun getForceStop(pkg: Installed): AutomationSpec = object : AutomationSpec.Explorer {
        override val tag: String = TAG
        override suspend fun createPlan(): suspend AutomationExplorer.Context.() -> Unit = {
            forceStopPlan(pkg)
        }
    }

    override suspend fun getArchive(pkg: Installed): AutomationSpec = object : AutomationSpec.Explorer {
        override val tag: String = TAG
        override suspend fun createPlan(): suspend AutomationExplorer.Context.() -> Unit = {
            archivePlan(pkg)
        }
    }

    override suspend fun getRestore(pkg: Installed): AutomationSpec = object : AutomationSpec.Explorer {
        override val tag: String = TAG
        override suspend fun createPlan(): suspend AutomationExplorer.Context.() -> Unit = {
            restorePlan(pkg)
        }
    }

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

                // Android 15+: the action button's icon and label are separate, unclickable nodes;
                // the tappable wrapper is a sibling (seen on Pixel 8 / Android 16 beta). Match ONLY
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

    private val archivePlan: suspend AutomationExplorer.Context.(Installed) -> Unit = plan@{ pkg ->
        log(TAG, INFO) { "Executing archive plan for ${pkg.installId} with context $this" }

        val archiveLabels = aospLabels.getArchiveButtonDynamic(this)
        var wasDisabled = false

        suspend fun StepContext.findArchiveTarget(root: ACSNodeInfo): ACSNodeInfo? {
            val candidate = root.crawl().map { it.node }.firstOrNull { it.textMatchesAny(archiveLabels) }
                ?: return null

            var target = findClickableParent(maxNesting = 3, includeSelf = true, node = candidate)

            // Android 15+: icon and label are separate, unclickable nodes; the tappable wrapper
            // is a sibling. Match only a clickable in the label's own column so we never grab an
            // adjacent action.
            if (target == null && hasApiLevel(35)) {
                log(TAG, WARN) { "No clickable parent found for $candidate" }
                target = findColumnAlignedClickable(candidate)
                if (target != null) log(TAG, INFO) { "Column-aligned clickable found: $target" }
            }

            if (target == null && hasApiLevel(35) && !candidate.getScreenBounds().isEmpty()) {
                log(TAG, WARN) { "No Archive button in its column, treating it as disabled: $candidate" }
                return candidate
            }

            if (target == null) log(TAG, WARN) { "No clickable target found for $candidate" }
            return target
        }

        // The label stand-in from findArchiveTarget is not clickable, so it never counts as ready.
        val isArchiveReady: (ACSNodeInfo) -> Boolean = { it.isClickable && it.isEnabled }

        run {
            val action: suspend StepContext.() -> Boolean = action@{
                // Unlike force-stop, a missing Archive button is not a success state. Return false
                // so the step retries and ultimately fails honestly rather than reporting a no-op
                // success (which the caller would only discover after the archive-verify timeout).
                var target = findArchiveTarget(host.waitForWindowRoot()) ?: return@action false

                if (!isArchiveReady(target)) {
                    // App info renders Archive disabled until its async eligibility checks finish,
                    // so a first disabled read does not yet mean the app can't be archived.
                    log(TAG, INFO) { "Archive button disabled, waiting for it to settle: $target" }
                    // Button or window gone during the wait: retry the step, don't claim it is disabled.
                    target = awaitEnabled(isReady = isArchiveReady) { root -> findArchiveTarget(root) }
                        ?: return@action false
                    if (!isArchiveReady(target)) {
                        wasDisabled = true
                        return@action true
                    }
                }

                clickNormal(node = target)
            }

            val step = AutomationStep(
                source = TAG,
                descriptionInternal = "Archive button for $pkg",
                label = mx.loperstack.tokkiphealth.appcontrol.R.string.appcontrol_automation_progress_find_archive.toCaString(archiveLabels),
                windowLaunch = windowLauncherDefaultSettings(pkg),
                windowCheck = windowCheckDefaultSettings(SETTINGS_PKG, ipcFunnel, pkg),
                nodeRecovery = defaultNodeRecovery(pkg),
                nodeAction = action,
            )
            stepper.withProgress(this) { process(this@plan, step) }
        }

        if (wasDisabled) {
            log(TAG, WARN) { "Archive button was disabled, app cannot be archived." }
            throw ArchiveUnavailableException("Archive button is disabled for ${pkg.installId}")
        }
        // Note: Android 15+ doesn't show a confirmation dialog - archive happens immediately when the button is clicked
    }

    private val restorePlan: suspend AutomationExplorer.Context.(Installed) -> Unit = plan@{ pkg ->
        log(TAG, INFO) { "Executing restore plan for ${pkg.installId} with context $this" }

        val restoreLabels = aospLabels.getRestoreButtonDynamic(this)
        var wasDisabled = false

        run {
            val action: suspend StepContext.() -> Boolean = action@{
                val candidate = findNode { node ->
                    node.textMatchesAny(restoreLabels)
                } ?: return@action false

                var target = findClickableParent(maxNesting = 3, includeSelf = true, node = candidate)

                // Android 15+: icon and label are separate, unclickable nodes; the tappable wrapper
                // is a sibling. Match only a clickable in the label's own column so we never grab an
                // adjacent action.
                if (target == null && hasApiLevel(35)) {
                    log(TAG, WARN) { "No clickable parent found for $candidate" }
                    target = findColumnAlignedClickable(candidate)
                    if (target != null) log(TAG, INFO) { "Column-aligned clickable found: $target" }
                }

                if (target == null) {
                    // Unlike force-stop, a missing Restore button is not a success state. Return false
                    // so the step retries and ultimately fails honestly rather than reporting a no-op
                    // success (which the caller would only discover after the restore-verify timeout).
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
                descriptionInternal = "Restore button for $pkg",
                label = mx.loperstack.tokkiphealth.appcontrol.R.string.appcontrol_automation_progress_find_restore.toCaString(restoreLabels),
                windowLaunch = windowLauncherDefaultSettings(pkg),
                windowCheck = windowCheckDefaultSettings(SETTINGS_PKG, ipcFunnel, pkg),
                nodeRecovery = defaultNodeRecovery(pkg),
                nodeAction = action,
            )
            stepper.withProgress(this) { process(this@plan, step) }
        }

        if (wasDisabled) {
            log(TAG, WARN) { "Restore button was disabled, app cannot be restored." }
            throw RestoreUnavailableException("Restore button is disabled for ${pkg.installId}")
        }
        // Note: Similar to archive, restore typically doesn't show a confirmation dialog
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: AOSPSpecs): AppControlSpecGenerator
    }

    companion object {
        val SETTINGS_PKG = "com.android.settings".toPkgId()

        val TAG: String = logTag("AppControl", "Automation", "AOSP", "Specs")
    }

}
