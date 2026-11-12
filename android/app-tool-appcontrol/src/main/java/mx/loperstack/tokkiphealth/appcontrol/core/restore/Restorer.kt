package mx.loperstack.tokkiphealth.appcontrol.core.restore

import android.os.Build
import androidx.annotation.RequiresApi
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
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.hasApiLevel
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.isArchived
import mx.loperstack.tokkiphealth.common.pkgs.pkgs
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.root.canUseRootNow
import mx.loperstack.tokkiphealth.common.sharedresource.HasSharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.SharedResource
import mx.loperstack.tokkiphealth.common.user.UserManager2
import mx.loperstack.tokkiphealth.setup.SetupModule
import mx.loperstack.tokkiphealth.setup.SetupBinding
import mx.loperstack.tokkiphealth.setup.isComplete
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.plus
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@Reusable
class Restorer @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
    private val pkgRepo: PkgRepo,
    private val userManager2: UserManager2,
    private val automation: AutomationSubmitter,
    @SetupBinding(SetupModule.Type.AUTOMATION) private val automationSetupModule: SetupModule,
    private val unarchiveManager: UnarchiveManager,
    private val rootManager: RootManager,
    private val adbManager: AdbManager,
) : HasSharedResource<Any> {

    override val sharedResource = SharedResource.createKeepAlive(TAG, appScope + dispatcherProvider.IO)

    suspend fun restore(app: AppInfo) {
        log(TAG, VERBOSE) { "restore($app)" }

        if (!app.pkg.isArchived) {
            throw RestoreException(
                message = "App is not archived and cannot be restored",
                installId = app.installId,
            )
        }

        val isCurrentUser = app.installId.userHandle == userManager2.currentUser().handle

        val hasElevatedAccess = rootManager.canUseRootNow() || adbManager.canUseAdbNow()

        // Try PackageInstaller API via Root/ADB first (API 35+)
        if (hasApiLevel(35) && hasElevatedAccess && !isCurrentUser) {
            requestUnarchiveForOtherUser(app)
        } else if (hasApiLevel(35) && hasElevatedAccess) {
            log(TAG) { "Attempting PackageInstaller.requestUnarchive via Root/ADB for ${app.installId}" }
            // Only the request itself is guarded, the automation fallback must not catch its own failure
            val result = try {
                unarchiveManager.requestUnarchive(app.installId)
            } catch (e: Exception) {
                log(TAG, WARN) { "PackageInstaller unarchive exception, falling back: ${e.asLog()}" }
                null
            }
            when {
                result == null -> useAutomationFallback(app)
                result.isSuccess -> {
                    log(TAG, INFO) { "PackageInstaller unarchive initiated successfully for ${app.installId}" }
                    // Continue to wait for the app to be fully restored below
                }

                else -> {
                    log(TAG, WARN) { "PackageInstaller unarchive failed: ${result.statusMessage}" }
                    // Fall through to automation
                    useAutomationFallback(app)
                }
            }
        } else {
            // Pre-API 35 or no Root/ADB: Only automation is supported for restore
            if (hasApiLevel(35)) {
                log(TAG) { "No Root/ADB access for ${app.installId}, falling back to automation" }
            }
            useAutomationFallback(app)
        }

        try {
            // Longer timeout for restore - may need to download APK from Play Store
            val timeoutSeconds = 120L
            log(TAG) { "Waiting for system restore process (timeout=$timeoutSeconds)" }

            // Wait until the app is no longer archived (sourceDir becomes non-null)
            withTimeout(timeoutSeconds * 1000) {
                if (isCurrentUser) {
                    pkgRepo.pkgs().first { pkgs ->
                        pkgs.any { it.installId == app.installId && !it.isArchived }
                    }
                } else {
                    delay(3000)

                    val getMatchingPackage = suspend {
                        pkgRepo.refresh()
                        pkgRepo.pkgs().first().firstOrNull {
                            it.installId == app.installId
                        }.also {
                            log(TAG) { "Refreshed restore state: $it, isArchived=${it?.isArchived}" }
                        }
                    }

                    var current = getMatchingPackage()

                    while (currentCoroutineContext().isActive && current?.isArchived != false) {
                        delay(3000)
                        current = getMatchingPackage()
                    }
                }
            }
            log(TAG, INFO) { "Successfully restored ${app.installId}" }
        } catch (e: Exception) {
            log(TAG, ERROR) { "Failed to verify restore for ${app.installId}: ${e.asLog()}" }
            throw RestoreException(installId = app.installId, cause = e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun requestUnarchiveForOtherUser(app: AppInfo) {
        log(TAG) { "Attempting PackageInstaller.requestUnarchive via Root/ADB for other user ${app.installId}" }
        val result = try {
            unarchiveManager.requestUnarchive(app.installId)
        } catch (e: TimeoutCancellationException) {
            log(TAG, WARN) { "No unarchive status for ${app.installId}, waiting for the restore anyway: $e" }
            return
        } catch (e: Exception) {
            log(TAG, ERROR) { "PackageInstaller unarchive failed for ${app.installId}: ${e.asLog()}" }
            throw RestoreException(installId = app.installId, cause = e)
        }

        if (!result.isSuccess) {
            log(TAG, WARN) { "PackageInstaller unarchive failed for ${app.installId}: $result" }
            throw RestoreException(
                message = "Unarchive request failed (status=${result.status}): ${result.statusMessage}",
                installId = app.installId,
            )
        }
        log(TAG, INFO) { "PackageInstaller unarchive initiated successfully for ${app.installId}" }
    }

    private suspend fun useAutomationFallback(app: AppInfo) {
        if (!automationSetupModule.isComplete()) {
            throw RestoreException(
                message = "Restoring requires Accessibility Service access (or PackageInstaller for Play Store apps on Android 15+)",
                installId = app.installId,
            )
        }

        log(TAG) { "Using Automation to restore ${app.installId}" }
        val task = RestoreAutomationTask(listOf(app.installId))
        val result = automation.submit(task) as RestoreAutomationTask.Result
        result.failed[app.installId]?.let {
            throw RestoreException(
                message = "Automation failed to restore app",
                installId = app.installId,
                cause = it,
            )
        }
    }

    companion object {
        private val TAG = logTag("AppControl", "Restorer")
    }
}
