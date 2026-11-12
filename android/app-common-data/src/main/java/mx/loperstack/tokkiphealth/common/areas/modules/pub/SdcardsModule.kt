package mx.loperstack.tokkiphealth.common.areas.modules.pub

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
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.local.LocalGateway
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.files.saf.SAFGateway
import mx.loperstack.tokkiphealth.common.rngString
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.root.canUseRootNow
import mx.loperstack.tokkiphealth.common.storage.PathMapper
import mx.loperstack.tokkiphealth.common.storage.StorageEnvironment
import mx.loperstack.tokkiphealth.common.user.UserManager2
import javax.inject.Inject

@Reusable
class SdcardsModule @Inject constructor(
    private val storageEnvironment: StorageEnvironment,
    private val userManager2: UserManager2,
    private val gatewaySwitch: GatewaySwitch,
    private val pathMapper: PathMapper,
    private val rootManager: RootManager,
    private val adbManager: AdbManager,
) : DataAreaModule {

    override suspend fun firstPass(): Collection<DataArea> {
        val sdcards = mutableSetOf<DataArea>()

        // TODO we are not getting multiuser sdcards
        storageEnvironment.getPublicPrimaryStorage(userManager2.currentUser().handle)
            ?.let { determineAreaAccessPath(it) }
            ?.let {
                DataArea(
                    path = it,
                    type = DataArea.Type.SDCARD,
                    userHandle = userManager2.currentUser().handle,
                    flags = setOf(DataArea.Flag.PRIMARY),
                )
            }
            ?.run { sdcards.add(this) }

        // Secondary storage is not user specific, e.g. /storage/3135-3132/Android/data
        storageEnvironment.getPublicSecondaryStorage(userManager2.currentUser().handle)
            .mapNotNull { determineAreaAccessPath(it) }
            .map {
                DataArea(
                    path = it,
                    type = DataArea.Type.SDCARD,
                    userHandle = userManager2.systemUser().handle,
                    flags = emptySet(),
                )
            }
            .run { sdcards.addAll(this) }

        log(TAG, VERBOSE) { "firstPass():$sdcards" }

        return sdcards
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

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: SdcardsModule): DataAreaModule
    }

    companion object {
        val TEST_PREFIX = "mx.loperstack.tokkiphealth-test-sd"
        val TEST_FILE_PREFIX = "$TEST_PREFIX-area-access"
        val TAG: String = logTag("DataArea", "Module", "Sdcard")
    }
}