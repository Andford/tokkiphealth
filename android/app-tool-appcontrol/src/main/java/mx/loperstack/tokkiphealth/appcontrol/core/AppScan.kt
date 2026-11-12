package mx.loperstack.tokkiphealth.appcontrol.core

import mx.loperstack.tokkiphealth.appcontrol.core.archive.ArchiveSupport
import mx.loperstack.tokkiphealth.appcontrol.core.usage.UsageInfo
import mx.loperstack.tokkiphealth.appcontrol.core.usage.UsageTool
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.throttleLatest
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.container.NormalPkg
import mx.loperstack.tokkiphealth.common.pkgs.current
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.pkgs.features.SourceAvailable
import mx.loperstack.tokkiphealth.common.pkgs.isArchived
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.progress.increaseProgress
import mx.loperstack.tokkiphealth.common.progress.updateProgressCount
import mx.loperstack.tokkiphealth.common.progress.updateProgressSecondary
import mx.loperstack.tokkiphealth.common.sharedresource.HasSharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.SharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.adoptChildResource
import mx.loperstack.tokkiphealth.common.user.UserHandle2
import mx.loperstack.tokkiphealth.common.user.UserManager2
import mx.loperstack.tokkiphealth.common.user.UserProfile2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class AppScan @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
    private val pkgRepo: PkgRepo,
    private val pkgOps: PkgOps,
    private val usageTool: UsageTool,
    private val userManager: UserManager2,
    private val archiveSupport: ArchiveSupport,
) : HasSharedResource<Any>, Progress.Host, Progress.Client {

    override val sharedResource = SharedResource.createKeepAlive(PkgOps.TAG, appScope + dispatcherProvider.IO)

    private val progressPub = MutableStateFlow<Progress.Data?>(restingProgress())
    override val progress: Flow<Progress.Data?> = progressPub.throttleLatest(250)

    override fun updateProgress(update: (Progress.Data?) -> Progress.Data?) {
        progressPub.value = update(progressPub.value)
    }

    private val mutex = Mutex()
    private var activeCache: Map<InstallId, Boolean?>? = null
    private var sizeCache: Map<InstallId, PkgOps.SizeStats?>? = null
    private var usageCache: Map<InstallId, UsageInfo?>? = null
    private var userProfilesCache: Map<UserHandle2, UserProfile2>? = null
    private val archivableCache = mutableMapOf<UserHandle2, Set<String>>()

    private suspend fun <T> doRun(action: suspend () -> T): T = mutex.withLock {
        adoptChildResource(pkgOps.sharedResource)
        progressPub.value = restingProgress()
        try {
            action()
        } finally {
            progressPub.value = restingProgress()
        }
    }

    suspend fun refresh() = doRun {
        activeCache = null
        sizeCache = null
        usageCache = null
        userProfilesCache = null
        archivableCache.clear()
        pkgRepo.refresh()
    }

    private suspend fun getUsage(id: InstallId): UsageInfo? {
        if (usageCache == null) {
            usageCache = mutableMapOf<InstallId, UsageInfo?>().apply {
                putAll(usageTool.lastMonth().map { it.installId to it })
            }
        }
        return usageCache!![id]
    }

    private suspend fun getSize(id: InstallId): PkgOps.SizeStats? {
        sizeCache?.let { if (it.containsKey(id)) return it[id] }
        return pkgOps.querySizeStats(id)
    }

    private suspend fun getActive(id: InstallId): Boolean? {
        if (activeCache == null) {
            activeCache = mutableMapOf<InstallId, Boolean?>().apply {
                putAll(pkgOps.getRunningPackages().map { it to true })
            }
        }
        return activeCache!![id]
    }

    private suspend fun getUserProfile(id: InstallId): UserProfile2? {
        if (userProfilesCache == null) {
            userProfilesCache = userManager.allUsers().associateBy { it.handle }
        }
        return userProfilesCache!![id.userHandle]
    }

    private suspend fun getArchivable(user: UserHandle2): Set<String> = archivableCache.getOrPut(user) {
        archiveSupport.getArchivablePackages(user)
    }

    suspend fun allApps(
        user: UserHandle2?,
        includeUsage: Boolean,
        includeActive: Boolean,
        includeSize: Boolean,
    ): Set<AppInfo> = doRun {
        log(TAG, VERBOSE) { "allApps(user=$user)" }
        val pkgs = pkgRepo.current().filter { user == null || it.userHandle == user }

        val runSizeSweep = includeSize && sizeCache == null
        // One counter spanning both phases: two Percent(pkgs.size) counters under the same label
        // would run 0-100 twice and read as the scan restarting.
        updateProgressCount(Progress.Count.Percent(if (runSizeSweep) pkgs.size * 2 else pkgs.size))

        if (runSizeSweep) {
            sizeCache = pkgs
                .map { it.installId }
                .asFlow()
                .flatMapMerge(4) {
                    flow { emit(it to pkgOps.querySizeStats(it)) }
                }
                // After the merge, so the read-modify-write runs in the collecting coroutine
                // instead of racing across the four producers.
                .onEach { increaseProgress() }
                .toList().associate { (id, size) -> id to size }
        }

        pkgs.map { pkg ->
            // Nothing in this loop is guaranteed to suspend, so without this a cancel can only land
            // once every package has been processed.
            currentCoroutineContext().ensureActive()
            // Package name, not Pkg.label: NormalPkg.label resolves through the PackageManager and
            // the overlay resolves `secondary` on the composition thread.
            updateProgressSecondary(pkg.packageName)
            val appInfo = pkg.toAppInfo(
                includeUsage = includeUsage,
                includeActive = includeActive,
                includeSize = includeSize,
                includeUserProfiles = user == null,
            )
            increaseProgress()
            appInfo
        }.toSet()
    }

    suspend fun app(
        pkgId: Pkg.Id,
        user: UserHandle2?,
        includeUsage: Boolean,
        includeActive: Boolean,
        includeSize: Boolean,
    ): Set<AppInfo> = doRun {
        log(TAG, VERBOSE) { "app($pkgId, user=$user)" }

        pkgRepo.current()
            .filter { it.id == pkgId }
            .filter { user == null || it.userHandle == user }
            .map {
                it.toAppInfo(
                    includeUsage = includeUsage,
                    includeActive = includeActive,
                    includeSize = includeSize,
                    includeUserProfiles = user == null,
                )
            }
            .toSet()
    }


    private suspend fun Installed.toAppInfo(
        includeActive: Boolean,
        includeSize: Boolean,
        includeUsage: Boolean,
        includeUserProfiles: Boolean,
    ) = AppInfo(
        pkg = this,
        isActive = if (includeActive) getActive(installId) ?: false else null,
        sizes = if (includeSize) getSize(installId) else null,
        usage = if (includeUsage) getUsage(installId) else null,
        userProfile = if (includeUserProfiles) getUserProfile(installId) else null,
        canBeToggled = this is NormalPkg,
        canBeStopped = this is NormalPkg,
        canBeExported = this is SourceAvailable,
        canBeDeleted = true,
        canBeArchived = this is NormalPkg &&
                archiveSupport.isArchivable(this) { getArchivable(it) } &&
                !isArchived &&
                !isSystemApp,
        canBeRestored = isArchived && archiveSupport.isArchivingEnabled,
    )

    companion object {
        /**
         * What the publisher holds between runs. Never null and never a finished count: the
         * forwarder started by `withProgress` replays whatever is in there as its first emission.
         */
        private fun restingProgress() = Progress.Data(
            primary = mx.loperstack.tokkiphealth.common.R.string.general_progress_loading_app_data.toCaString(),
        )

        private val TAG = logTag("AppControl", "AppScan")
    }
}