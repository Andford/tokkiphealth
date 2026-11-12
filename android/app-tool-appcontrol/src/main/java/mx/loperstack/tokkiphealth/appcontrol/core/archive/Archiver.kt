package mx.loperstack.tokkiphealth.appcontrol.core.archive

import dagger.Reusable
import mx.loperstack.tokkiphealth.appcontrol.core.AppInfo
import mx.loperstack.tokkiphealth.automation.core.AutomationSubmitter
import mx.loperstack.tokkiphealth.common.adb.AdbManager
import mx.loperstack.tokkiphealth.common.adb.canUseAdbNow
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.isArchived
import mx.loperstack.tokkiphealth.common.pkgs.pkgs
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.root.canUseRootNow
import mx.loperstack.tokkiphealth.common.sharedresource.HasSharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.SharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.adoptChildResource
import mx.loperstack.tokkiphealth.common.shell.ShellOps
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsCmd
import mx.loperstack.tokkiphealth.common.user.UserManager2
import mx.loperstack.tokkiphealth.setup.SetupModule
import mx.loperstack.tokkiphealth.setup.SetupBinding
import mx.loperstack.tokkiphealth.setup.isComplete
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.plus
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@Reusable
class Archiver @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
    private val pkgRepo: PkgRepo,
    private val shellOps: ShellOps,
    private val rootManager: RootManager,
    private val adbManager: AdbManager,
    private val userManager2: UserManager2,
    private val archiveSupport: ArchiveSupport,
    private val automation: AutomationSubmitter,
    @SetupBinding(SetupModule.Type.AUTOMATION) private val automationSetupModule: SetupModule,
) : HasSharedResource<Any> {

    override val sharedResource = SharedResource.createKeepAlive(TAG, appScope + dispatcherProvider.IO)

    suspend fun archive(app: AppInfo) {
        log(TAG, VERBOSE) { "archive($app)" }

        if (!archiveSupport.isArchivingEnabled) {
            throw ArchiveException(
                message = "Archiving is not available on this device",
                installId = app.installId,
            )
        }

        val isCurrentUser = app.installId.userHandle == userManager2.currentUser().handle
        val userId = app.installId.userHandle.handleId
        val pkgName = app.installId.pkgId.name
        val shellCmd = ShellOpsCmd("pm archive --user $userId $pkgName")

        when {
            rootManager.canUseRootNow() -> {
                log(TAG) { "Using ROOT to archive ${app.installId}" }

                adoptChildResource(shellOps)
                val result = shellOps.execute(shellCmd, mode = ShellOps.Mode.ROOT)
                log(TAG) { "Archive command via ROOT result: $result" }
                if (!result.isSuccess) {
                    throw ArchiveException(
                        installId = app.installId,
                        cause = IllegalStateException(result.errors.joinToString()),
                    )
                }
            }

            adbManager.canUseAdbNow() -> {
                log(TAG) { "Using ADB to archive ${app.installId}" }

                adoptChildResource(shellOps)
                val result = shellOps.execute(shellCmd, mode = ShellOps.Mode.ADB)
                log(TAG) { "Archive command via ADB result: $result" }
                if (!result.isSuccess) {
                    throw ArchiveException(
                        installId = app.installId,
                        cause = IllegalStateException(result.errors.joinToString()),
                    )
                }
            }

            automationSetupModule.isComplete() -> {
                log(TAG) { "Using Automation to archive ${app.installId}" }
                val task = ArchiveAutomationTask(listOf(app.installId))
                val result = automation.submit(task) as ArchiveAutomationTask.Result
                result.failed[app.installId]?.let {
                    throw ArchiveException(
                        message = "Automation failed to archive app",
                        installId = app.installId,
                        cause = it,
                    )
                }
            }

            else -> {
                throw ArchiveException(
                    message = "Archiving requires Root, Shizuku, or Accessibility Service access",
                    installId = app.installId,
                )
            }
        }

        try {
            val timeoutSeconds = 60L
            log(TAG) { "Waiting for system archive process (timeout=$timeoutSeconds)" }

            // Wait until the app is archived (sourceDir becomes null)
            withTimeout(timeoutSeconds * 1000) {
                if (isCurrentUser) {
                    pkgRepo.pkgs().first { pkgs ->
                        pkgs.any { it.installId == app.installId && it.isArchived }
                    }
                } else {
                    delay(3000)

                    val getMatchingPackage = suspend {
                        pkgRepo.refresh()
                        pkgRepo.pkgs().first().firstOrNull {
                            it.installId == app.installId
                        }.also {
                            log(TAG) { "Refreshed archive state: $it, isArchived=${it?.isArchived}" }
                        }
                    }

                    var current = getMatchingPackage()

                    while (currentCoroutineContext().isActive && current?.isArchived != true) {
                        delay(3000)
                        current = getMatchingPackage()
                    }
                }
            }
            log(TAG, INFO) { "Successfully archived ${app.installId}" }
        } catch (e: Exception) {
            log(TAG, ERROR) { "Failed to verify archive for ${app.installId}: ${e.asLog()}" }
            throw ArchiveException(installId = app.installId, cause = e)
        }
    }

    companion object {
        private val TAG = logTag("AppControl", "Archiver")
    }
}
