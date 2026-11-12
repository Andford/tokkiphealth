package mx.loperstack.tokkiphealth.common.areas.modules.pub

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.modules.DataAreaModule
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.device.DeviceDetective
import mx.loperstack.tokkiphealth.common.device.RomType
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.canRead
import mx.loperstack.tokkiphealth.common.files.canWrite
import mx.loperstack.tokkiphealth.common.files.listFiles
import mx.loperstack.tokkiphealth.common.files.lookup
import mx.loperstack.tokkiphealth.common.storage.StorageEnvironment
import mx.loperstack.tokkiphealth.common.user.UserManager2
import java.io.IOException
import javax.inject.Inject

@Reusable
class PublicMediaModule @Inject constructor(
    private val gatewaySwitch: GatewaySwitch,
    private val deviceDetective: DeviceDetective,
    private val storageEnv: StorageEnvironment,
    private val userManager2: UserManager2,
) : DataAreaModule {

    override suspend fun secondPass(firstPass: Collection<DataArea>): Collection<DataArea> {
        val areas = mutableSetOf<DataArea>()

        val sdcardAreas = firstPass.filter { it.type == DataArea.Type.SDCARD }.toMutableSet()

        sdcardAreas
            .map { parentArea ->
                parentArea to parentArea.path.child("Android", "media")
            }
            .filter { (area, path) ->
                val canRead = try {
                    path.canRead(gatewaySwitch)
                } catch (e: IOException) {
                    log(TAG, ERROR) { "Failed canRead() for $area: ${e.asLog()}" }
                    return@filter false
                }
                if (!canRead) {
                    log(TAG) { "Can't read $area" }
                    return@filter false
                }

                try {
                    path.lookup(gatewaySwitch)
                } catch (e: IOException) {
                    log(TAG, ERROR) { "Failed lookup() for $area: ${e.asLog()}" }
                    return@filter false
                }

                try {
                    path.listFiles(gatewaySwitch)
                } catch (e: IOException) {
                    log(TAG, ERROR) { "Failed listFiles() for $area: ${e.asLog()}" }
                    return@filter false
                }

                true
            }
            .map { (parentArea, path) ->
                DataArea(
                    type = DataArea.Type.PUBLIC_MEDIA,
                    path = path,
                    flags = parentArea.flags,
                    userHandle = parentArea.userHandle,
                )
            }
            .run { areas.addAll(this) }

        if (sdcardAreas.isEmpty() && deviceDetective.getROMType() == RomType.ANDROID_TV) {
            log(TAG, INFO) { "AndroidTV: Restricted access. Trying manual data area creation." }
            userManager2.allUsers()
                .map { profile ->
                    storageEnv.getPublicStorage(profile.handle).map {
                        profile to it.child("Android", "media")
                    }
                }
                .flatten()
                .filter { (_, path) ->
                    val canWrite = try {
                        path.canWrite(gatewaySwitch)
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Failed canWrite() for $path: ${e.asLog()}" }
                        false
                    }
                    if (!canWrite) log(TAG) { "Can't write $path" }
                    canWrite
                }
                .map { (profile, path) ->
                    DataArea(
                        type = DataArea.Type.PUBLIC_MEDIA,
                        path = path,
                        userHandle = profile.handle,
                    )
                }
                .run { areas.addAll(this) }
        }

        log(TAG, VERBOSE) { "secondPass(): $areas" }

        return areas
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: PublicMediaModule): DataAreaModule
    }

    companion object {
        val TAG: String = logTag("DataArea", "Module", "Public", "Media")
    }
}