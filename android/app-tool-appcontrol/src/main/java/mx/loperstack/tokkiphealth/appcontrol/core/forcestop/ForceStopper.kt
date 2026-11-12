package mx.loperstack.tokkiphealth.appcontrol.core.forcestop

import dagger.Reusable
import mx.loperstack.tokkiphealth.appcontrol.core.AppInfo
import mx.loperstack.tokkiphealth.automation.core.AutomationSubmitter
import mx.loperstack.tokkiphealth.automation.core.ForceStopAutomationTask
import mx.loperstack.tokkiphealth.common.R
import mx.loperstack.tokkiphealth.common.adb.AdbManager
import mx.loperstack.tokkiphealth.common.adb.canUseAdbNow
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.throttleLatest
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.progress.increaseProgress
import mx.loperstack.tokkiphealth.common.progress.updateProgressCount
import mx.loperstack.tokkiphealth.common.progress.updateProgressPrimary
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.root.canUseRootNow
import mx.loperstack.tokkiphealth.common.sharedresource.HasSharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.SharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.adoptChildResource
import mx.loperstack.tokkiphealth.setup.SetupModule
import mx.loperstack.tokkiphealth.setup.SetupBinding
import mx.loperstack.tokkiphealth.setup.isComplete
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.plus
import javax.inject.Inject

@Reusable
class ForceStopper @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
    private val pkgOps: PkgOps,
    private val automation: AutomationSubmitter,
    private val adbManager: AdbManager,
    private val rootManager: RootManager,
    @SetupBinding(SetupModule.Type.AUTOMATION) private val automationSetupModule: SetupModule,
) : HasSharedResource<Any>, Progress.Host, Progress.Client {

    private val progressPub = MutableStateFlow<Progress.Data?>(
        Progress.Data(primary = R.string.general_progress_preparing.toCaString())
    )
    override val progress: Flow<Progress.Data?> = progressPub.throttleLatest(250)

    override fun updateProgress(update: (Progress.Data?) -> Progress.Data?) {
        progressPub.value = update(progressPub.value)
    }

    override val sharedResource = SharedResource.createKeepAlive(TAG, appScope + dispatcherProvider.IO)

    suspend fun forceStop(apps: List<AppInfo>, allowOffLimits: Boolean = false): Result {
        log(TAG, INFO) { "forceStop(${apps.size}, allowOffLimits=$allowOffLimits)" }
        adoptChildResource(pkgOps)


        val successful = mutableSetOf<InstallId>()
        val failed = mutableSetOf<InstallId>()

        val (offLimits, targets) = when {
            allowOffLimits -> emptyList<AppInfo>() to apps
            else -> apps.partition { ForceStopAutomationTask.OFF_LIMIT_PKGS.contains(it.installId.pkgId) }
        }
        offLimits.forEach {
            log(TAG, WARN) { "Skipping ${it.installId}: force-stopping it would break accessibility automation" }
            failed.add(it.installId)
        }
        if (targets.isEmpty()) {
            return Result(success = successful, failed = failed)
        }

        if (rootManager.canUseRootNow() || adbManager.canUseAdbNow()) {
            log(TAG) { "Using ROOT/ADB..." }
            updateProgressCount(Progress.Count.Percent(targets.size))
            targets.forEach {
                log(TAG) { "Force stopping ${it.installId} " }
                updateProgressPrimary { ctx ->
                    ctx.getString(R.string.general_progress_processing_x, it.label.get(ctx))
                }
                try {
                    pkgOps.forceStop(it.installId)
                    log(TAG, INFO) { "State successfully force stopped ${it.installId}" }
                    successful.add(it.installId)
                } catch (e: Exception) {
                    log(TAG, ERROR) { "Failed to force-stop $it: ${e.asLog()}" }
                    failed.add(it.installId)
                } finally {
                    increaseProgress()
                }
            }
        } else if (automationSetupModule.isComplete()) {
            log(TAG) { "Using Automation..." }
            val task = ForceStopAutomationTask(targets.map { it.installId }.toList(), allowOffLimits = allowOffLimits)
            val result = automation.submit(task) as ForceStopAutomationTask.Result
            successful.addAll(result.successful)
            failed.addAll(result.failed)
        } else {
            log(TAG, WARN) { "No method available for force-stopping apps" }
            failed.addAll(targets.map { it.installId })
        }

        return Result(
            success = successful,
            failed = failed,
        )
    }

    data class Result(
        val success: Set<InstallId>,
        val failed: Set<InstallId>,
    )

    companion object {
        private val TAG = logTag("AppControl", "ForceStopper")
    }
}