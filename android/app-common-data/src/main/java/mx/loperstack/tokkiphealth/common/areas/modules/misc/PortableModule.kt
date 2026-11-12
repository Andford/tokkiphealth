package mx.loperstack.tokkiphealth.common.areas.modules.misc

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.adb.AdbManager
import mx.loperstack.tokkiphealth.common.adb.canUseAdbNow
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.modules.DataAreaModule
import mx.loperstack.tokkiphealth.common.areas.modules.probeWriteAccess
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.local.LocalGateway
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.files.local.toLocalPath
import mx.loperstack.tokkiphealth.common.files.saf.SAFGateway
import mx.loperstack.tokkiphealth.common.rngString
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.root.canUseRootNow
import mx.loperstack.tokkiphealth.common.storage.PathMapper
import mx.loperstack.tokkiphealth.common.storage.StorageManager2
import mx.loperstack.tokkiphealth.common.user.UserManager2
import javax.inject.Inject


@Reusable
class PortableModule @Inject constructor(
    private val userManager2: UserManager2,
    private val storageManager2: StorageManager2,
    private val gatewaySwitch: GatewaySwitch,
    private val pathMapper: PathMapper,
    private val adbManager: AdbManager,
    private val rootManager: RootManager,
) : DataAreaModule {

    override suspend fun secondPass(firstPass: Collection<DataArea>): Collection<DataArea> {
        val volumeInfos = storageManager2.volumes ?: emptySet()
        log(TAG) { "VolumeInfos: $volumeInfos" }

        val storageVolumes = storageManager2.storageVolumes
        log(TAG) { "StorageVolumes: $storageVolumes" }

        val portables = storageVolumes
            .filter { !it.isPrimary && it.isRemovable }
            .filter { it.directory != null }
            .filter { cand ->
                val isPubStorage = firstPass.any { it.path == cand.directory!!.toLocalPath() }
                if (isPubStorage) {
                    log(TAG) { "$cand is a public storage that is already covered" }
                }
                !isPubStorage
            }
            .filter { cand ->
                val volumeInfoX = volumeInfos.singleOrNull { it.path == cand.directory }
                val isUsbStorage = volumeInfoX?.disk?.isUsb
                if (isUsbStorage == null) log(TAG, WARN) { "DiskInfo.isUsb is NULL for $cand" }
                isUsbStorage == true
            }

        // In the happy path. we just get 3 volumes, one is primary and internal, two are removable and not primary
        // One of them may be an sdcard, the other one would be USB
        // If we remove the sdcard, we have the USB left. The sdcard should be identifiable by getting externalCacheDirs

        return portables
            .map { it.directory!!.toLocalPath() }
            .mapNotNull {
                val accessPath = determineAreaAccessPath(it)
                if (accessPath != null) {
                    log(TAG, INFO) { "Got a portable storage under $accessPath" }
                } else {
                    log(TAG, INFO) { "Got a portable storage but it's not accessible ($it)" }
                }
                accessPath
            }
            .map { accessPath ->
                DataArea(
                    type = DataArea.Type.PORTABLE,
                    path = accessPath,
                    userHandle = userManager2.systemUser().handle,
                )
            }
    }

    private suspend fun determineAreaAccessPath(targetPath: LocalPath): APath? {
        val localGateway = gatewaySwitch.getGateway(APath.PathType.LOCAL) as LocalGateway

        val localProbe = targetPath.child("$TEST_FILE_PREFIX-local-$rngString")
        if (localGateway.probeWriteAccess(TAG, localProbe, LocalGateway.Mode.NORMAL)) {
            log(TAG) { "Original targetPath is accessible $targetPath" }
            return targetPath
        }

        if (adbManager.canUseAdbNow()) {
            val adbProbe = targetPath.child("$TEST_FILE_PREFIX-adb-$rngString")
            if (localGateway.probeWriteAccess(TAG, adbProbe, LocalGateway.Mode.ADB)) {
                log(TAG) { "Original targetPath is accessible via ADB $targetPath" }
                return targetPath
            }
        }

        if (rootManager.canUseRootNow()) {
            val rootProbe = targetPath.child("$TEST_FILE_PREFIX-root-$rngString")
            if (localGateway.probeWriteAccess(TAG, rootProbe, LocalGateway.Mode.ROOT)) {
                log(TAG) { "Original targetPath is accessible via ROOT $targetPath" }
                return targetPath
            }
        }

        log(TAG) { "$targetPath wasn't accessible trying SAF mapping..." }
        pathMapper.toSAFPath(targetPath)?.let { safPath ->
            val safGateway = gatewaySwitch.getGateway(APath.PathType.SAF) as SAFGateway
            val safProbe = safPath.child("$TEST_FILE_PREFIX-saf-$rngString")
            if (safGateway.probeWriteAccess(TAG, safProbe)) {
                log(TAG) { "Switching from $targetPath to $safPath" }
                return safPath
            }
        }

        return null
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: PortableModule): DataAreaModule
    }

    companion object {
        val TEST_PREFIX = "mx.loperstack.tokkiphealth-test-usb"
        val TEST_FILE_PREFIX = "$TEST_PREFIX-area-access"
        val TAG: String = logTag("DataArea", "Module", "Portable")
    }
}