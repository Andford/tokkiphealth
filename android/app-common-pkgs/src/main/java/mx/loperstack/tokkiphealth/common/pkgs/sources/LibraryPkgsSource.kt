package mx.loperstack.tokkiphealth.common.pkgs.sources

import android.content.pm.PackageInfo
import android.content.pm.SharedLibraryInfo
import androidx.core.content.pm.PackageInfoCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.adb.AdbManager
import mx.loperstack.tokkiphealth.common.adb.canUseAdbNow
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.hasApiLevel
import mx.loperstack.tokkiphealth.common.pkgs.PkgDataSource
import mx.loperstack.tokkiphealth.common.pkgs.container.LibraryPkg
import mx.loperstack.tokkiphealth.common.pkgs.container.toVersionedPkgId
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.root.canUseRootNow
import mx.loperstack.tokkiphealth.common.user.UserHandle2
import mx.loperstack.tokkiphealth.common.user.UserManager2
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryPkgsSource @Inject constructor(
    private val pkgOps: PkgOps,
    private val userManager2: UserManager2,
    private val pathClaw: SharedLibraryPathClaw,
    private val rootManager: RootManager,
    private val adbManager: AdbManager,
) : PkgDataSource {

    override suspend fun getPkgs(): Collection<Installed> = pkgOps.useRes {
        log(TAG, VERBOSE) { "getPkgs()" }

        val allUsers = userManager2.allUsers().map { it.handle }

        // Static and SDK shared libraries are invisible to the normal PackageManager path
        // and can only be queried from a privileged caller (ROOT or ADB/Shizuku) with the
        // MATCH_STATIC_SHARED_AND_SDK_LIBRARIES flag. Pick one privileged mode up-front —
        // both routes go through the same PkgOpsClient IPC, so no escalation is needed.
        val privilegedMode: PkgOps.Mode? = when {
            rootManager.canUseRootNow() -> PkgOps.Mode.ROOT
            adbManager.canUseAdbNow() -> PkgOps.Mode.ADB
            else -> null
        }

        // Pre-fetch static and SDK shared library PackageInfos in a single IPC per user.
        // The returned PackageInfo.packageName for a static/SDK lib is the *unversioned*
        // library name (e.g. com.google.android.trichromelibrary), so we key the map by
        // "${packageName}_${longVersionCode}" to disambiguate multiple versions of the
        // same library — which matches the format produced by SharedLibraryInfo.toVersionedPkgId().
        val privilegedLibInfosByUser: Map<UserHandle2, Map<String, PackageInfo>> = if (privilegedMode != null) {
            allUsers.associateWith { handle ->
                try {
                    pkgOps.queryPkgs(
                        flags = MATCH_STATIC_SHARED_AND_SDK_LIBRARIES,
                        userHandle = handle,
                        mode = privilegedMode,
                    ).associateBy { info ->
                        val longVersion = PackageInfoCompat.getLongVersionCode(info)
                        "${info.packageName}_$longVersion"
                    }
                } catch (e: Exception) {
                    log(TAG, WARN) { "Batch lib query failed for user=$handle ($privilegedMode): ${e.asLog()}" }
                    emptyMap()
                }
            }
        } else {
            emptyMap()
        }

        val libraryPkgs = pkgOps.getSharedLibraries(0)
            .onEach { log(TAG, VERBOSE) { "Checking $it" } }
            .filter { it.type != 0 } // Built-in types like .jars
            .mapNotNull { libraryInfo ->
                val apkPath = pathClaw.clawOutPath(libraryInfo)
                val apkInfo = apkPath?.let { path ->
                    try {
                        pkgOps.viewArchive(path, 0)
                    } catch (e: Exception) {
                        log(TAG) { "PkgInfo lookup on clawed path failed $libraryInfo: ${e.asLog()}" }
                        null
                    }
                } ?: return@mapNotNull null

                val needsPrivileged = libraryInfo.type == SharedLibraryInfo.TYPE_STATIC ||
                        (hasApiLevel(31) && libraryInfo.type == SHARED_LIBRARY_TYPE_SDK_PACKAGE)

                allUsers.map { handle ->
                    val livePkgInfo: PackageInfo? = if (needsPrivileged) {
                        // O(1) lookup in the pre-fetched batch, keyed by versioned name.
                        val versionedKey = libraryInfo.toVersionedPkgId().name
                        privilegedLibInfosByUser[handle]?.get(versionedKey)
                    } else {
                        // Dynamic libs: cheap in-process NORMAL query by the APK's real
                        // package name. queryPkg's NORMAL branch ignores the user handle
                        // (it calls the app-process PackageManager directly).
                        try {
                            pkgOps.queryPkg(
                                id = apkInfo.id,
                                flags = 0L,
                                userHandle = handle,
                                mode = PkgOps.Mode.NORMAL,
                            )
                        } catch (e: Exception) {
                            log(TAG) { "Normal query failed for ${apkInfo.id} user=$handle: ${e.message}" }
                            null
                        }
                    }

                    LibraryPkg(
                        sharedLibraryInfo = libraryInfo,
                        apkPath = apkPath,
                        packageInfo = livePkgInfo ?: apkInfo.packageInfo,
                        userHandle = handle,
                    )
                }
            }.flatten()

        log(TAG) { "Found ${libraryPkgs.size} library pkgs" }
        log(TAG, VERBOSE) { libraryPkgs.joinToString("\n") }

        libraryPkgs
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: LibraryPkgsSource): PkgDataSource
    }

    companion object {
        private val TAG = logTag("Pkg", "Repo", "Source", "LibraryPkgs")

        // PackageManager.MATCH_STATIC_SHARED_AND_SDK_LIBRARIES is @SystemApi hidden;
        // the int value is stable since API 26 (renamed in API 31 to also cover SDK libs).
        private const val MATCH_STATIC_SHARED_AND_SDK_LIBRARIES = 0x04000000L

        // SharedLibraryInfo.TYPE_SDK_PACKAGE was added in API 31. Use the literal to keep
        // this file compilable against older SDK stubs.
        private const val SHARED_LIBRARY_TYPE_SDK_PACKAGE = 3
    }
}
