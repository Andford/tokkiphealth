package mx.loperstack.tokkiphealth.common.forensics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.isInstalled
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.sharedresource.HasSharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.SharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.keepResourceHoldersAlive
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class FileForensics @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    @ApplicationContext val context: Context,
    private val pkgRepo: PkgRepo,
    private val csiProcessorsProvider: Provider<Set<@JvmSuppressWildcards CSIProcessor>>,
    private val gatewaySwitch: GatewaySwitch,
    private val pkgOps: PkgOps,
) : HasSharedResource<Any> {

    override val sharedResource = SharedResource.createKeepAlive(TAG, appScope)

    private val csiProcessors by lazy {
        csiProcessorsProvider.get().also {
            log(TAG, INFO) { "${it.size} CSI processors loaded." }
            log(TAG, VERBOSE) { it.joinToString("\n") }
        }
    }

    suspend fun identifyArea(file: APath): AreaInfo? = keepResourceHoldersAlive(gatewaySwitch, pkgOps) {
        if (file is LocalPath && !file.file.isAbsolute) throw IllegalArgumentException("Not absolute: ${file.path}")

        csiProcessors.firstNotNullOfOrNull { it.identifyArea(file) }
    }

    suspend fun findOwners(file: APath): OwnerInfo? {
        if (file is LocalPath && !file.file.isAbsolute) throw IllegalArgumentException("Not absolute:" + file.path)

        return identifyArea(file)?.let { findOwners(it) }
    }

    suspend fun findOwners(areaInfo: AreaInfo): OwnerInfo = keepResourceHoldersAlive(gatewaySwitch, pkgOps) {
        val startFindingOwner = System.currentTimeMillis()

        val result = csiProcessors
            .firstOrNull { it.hasJurisdiction(areaInfo.type) }
            ?.findOwners(areaInfo)
            ?: CSIProcessor.Result().also {
                log(TAG, WARN) { "No CSI processor has juridiction: $areaInfo" }
                if (Bugs.isDebug) throw IllegalStateException("Missing CSI processor")
            }

        val installedOwners = result.owners.filter {
            // Check each owner against its own user, not the area's: owners normally carry the area's
            // user, but uid-based attribution (PrivateDataCSI) can resolve an owner in a different user
            // than the area, and the two must agree for the install check to be meaningful.
            pkgRepo.isInstalled(it.pkgId, it.userHandle)
        }.toSet()

        val ownerInfo = OwnerInfo(
            areaInfo = areaInfo,
            owners = result.owners,
            installedOwners = installedOwners,
            hasUnknownOwner = result.hasKnownUnknownOwner,
        )

        val timeForProcessing = System.currentTimeMillis() - startFindingOwner

        if (Bugs.isTrace) {
            time += timeForProcessing
            count++
            val avg = time / count
            log(TAG, VERBOSE) { "Processing: ${timeForProcessing}ms, avg. ${avg}ms ($areaInfo)" }
            for (own in ownerInfo.owners) log(TAG, VERBOSE) { "Matched ${areaInfo.file} to ${own.pkgId}" }
            if (ownerInfo.hasUnknownOwner) log(TAG, VERBOSE) { "$areaInfo has an unknown Owner" }
        }

        ownerInfo
    }

    private var time: Long = 0
    private var count: Long = 0

    companion object {
        val TAG: String = logTag("CSI", "FileForensics")
    }
}