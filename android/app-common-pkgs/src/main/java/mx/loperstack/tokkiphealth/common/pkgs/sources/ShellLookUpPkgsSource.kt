package mx.loperstack.tokkiphealth.common.pkgs.sources

import android.content.pm.PackageManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.adb.AdbManager
import mx.loperstack.tokkiphealth.common.adb.canUseAdbNow
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.pkgs.PkgDataSource
import mx.loperstack.tokkiphealth.common.pkgs.container.HiddenPkg
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.root.canUseRootNow
import mx.loperstack.tokkiphealth.common.shell.ShellOps
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsCmd
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsStreamEvent
import mx.loperstack.tokkiphealth.common.user.UserManager2
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ShellLookUpPkgsSource @Inject constructor(
    private val pkgOps: PkgOps,
    private val rootManager: RootManager,
    private val adbManager: AdbManager,
    private val userManager: UserManager2,
    private val shellOps: ShellOps,
) : PkgDataSource {

    override suspend fun getPkgs(): Collection<Installed> = pkgOps.useRes {
        log(TAG) { "getPkgs()" }

        val mode = when {
            rootManager.canUseRootNow() -> ShellOps.Mode.ROOT
            adbManager.canUseAdbNow() -> ShellOps.Mode.ADB
            else -> null
        }

        if (mode == null) {
            log(TAG) { "Requires root or adb, skipping..." }
            return@useRes emptySet()
        }

        log(TAG) { "Using shell mode $mode" }

        val alreadyKnown = try {
            pkgOps.queryPkgs(0L).map { it.packageName }
        } catch (e: Exception) {
            log(TAG, ERROR) { "Failed to grab default packages: ${e.asLog()}" }
            emptySet()
        }

        userManager.allUsers()
            .mapNotNull { user ->
                val output = mutableListOf<String>()
                var exitCode = -1
                shellOps.executeStream(
                    ShellOpsCmd("pm list packages -f -u --user ${user.handle.handleId}"),
                    mode,
                ).collect { event ->
                    when (event) {
                        is ShellOpsStreamEvent.Stdout -> output.add(event.line)
                        is ShellOpsStreamEvent.Exit -> exitCode = event.exitCode
                        is ShellOpsStreamEvent.Stderr -> Unit
                        is ShellOpsStreamEvent.Error -> Unit
                    }
                }
                if (exitCode != 0) return@mapNotNull null
                user to output.toList()
            }
            .map { (user, lines) ->
                log(TAG, VERBOSE) { "${lines.size} entries for $user" }
                lines.mapNotNull { PATTERN.matchEntire(it) }.map { user to it }
            }
            .flatten()
            .mapNotNull { (user, match) ->
                val pkgName = match.groupValues[2]
                if (alreadyKnown.contains((pkgName))) {
                    return@mapNotNull null
                }

                log(TAG) { "Potentially hidden pkg: $pkgName" }

                val rawPath = match.groupValues[1]
                if (rawPath == "null") {
                    log(TAG, VERBOSE) { "No APK path for $pkgName (cross-profile)" }
                    return@mapNotNull null
                }

                val stateBacked = try {
                    pkgOps.queryPkg(
                        pkgName.toPkgId(),
                        PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong(),
                        user.handle,
                    )
                } catch (e: Exception) {
                    log(TAG, WARN) { "State backed lookup failed for $pkgName: ${e.asLog()}" }
                    null
                }
                if (stateBacked != null) {
                    log(TAG, VERBOSE) { "State backed info for $pkgName: $stateBacked" }
                    return@mapNotNull HiddenPkg(
                        packageInfo = stateBacked,
                        userHandle = user.handle,
                    )
                }

                val sourcePath = LocalPath.build(rawPath)
                log(TAG, VERBOSE) { "Reading archive $sourcePath" }
                val apkInfo = pkgOps.viewArchive(sourcePath)

                if (apkInfo == null) {
                    log(TAG, WARN) { "Failed to read APK info from $sourcePath" }
                    return@mapNotNull null
                }

                if (pkgName != apkInfo.packageName) {
                    log(TAG, ERROR) { "Packagename did not match: $apkInfo" }
                    return@mapNotNull null
                }

                HiddenPkg(
                    packageInfo = apkInfo.packageInfo,
                    userHandle = user.handle,
                    apkPath = sourcePath,
                )
            }
    }


    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: ShellLookUpPkgsSource): PkgDataSource
    }

    companion object {

        private val PATTERN = Regex("^package:(.+?)=([\\w._]+)$")
        private val TAG = logTag("Pkg", "Repo", "Source", "ShellLookUp")
    }
}