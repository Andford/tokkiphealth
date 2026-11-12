package mx.loperstack.tokkiphealth.analyzer.core.device

import android.os.storage.StorageManager
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.asFile
import mx.loperstack.tokkiphealth.common.flow.throttleLatest
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.progress.updateProgressPrimary
import mx.loperstack.tokkiphealth.common.progress.updateProgressSecondary
import mx.loperstack.tokkiphealth.common.storage.StorageEnvironment
import mx.loperstack.tokkiphealth.common.storage.StorageId
import mx.loperstack.tokkiphealth.common.storage.StorageManager2
import mx.loperstack.tokkiphealth.common.storage.StorageSpaceReconcile
import mx.loperstack.tokkiphealth.common.storage.StorageStatsManager2
import mx.loperstack.tokkiphealth.setup.SetupModule
import mx.loperstack.tokkiphealth.setup.isComplete
import mx.loperstack.tokkiphealth.setup.SetupBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import javax.inject.Inject

class DeviceStorageScanner @Inject constructor(
    @SetupBinding(SetupModule.Type.STORAGE) private val storageSetupModule: SetupModule,
    private val environment: StorageEnvironment,
    private val storageManager2: StorageManager2,
    private val storageStatsmanager: StorageStatsManager2,
) : Progress.Host, Progress.Client {

    private val progressPub = MutableStateFlow<Progress.Data?>(
        Progress.Data(primary = mx.loperstack.tokkiphealth.common.R.string.general_progress_preparing.toCaString())
    )

    override val progress: Flow<Progress.Data?> = progressPub.throttleLatest(250)

    override fun updateProgress(update: (Progress.Data?) -> Progress.Data?) {
        progressPub.value = update(progressPub.value)
    }

    suspend fun scan(): Set<DeviceStorage> {
        log(TAG) { "Scanning..." }

        updateProgressPrimary(mx.loperstack.tokkiphealth.analyzer.R.string.analyzer_progress_scanning_device)

        val setupIncomplete = !storageSetupModule.isComplete()

        val primaryDevice = run {
            updateProgressSecondary(mx.loperstack.tokkiphealth.analyzer.R.string.analyzer_storage_type_primary_title)
            val primaryUuid = StorageManager.UUID_DEFAULT
                ?: UUID.fromString("00000000-0000-0000-0000-000000000000")
            val id = StorageId(
                internalId = null,
                externalId = primaryUuid,
            )

            val dataDirFile = environment.dataDir.asFile()
            val fileTotal = dataDirFile.totalSpace
            val fileFree = dataDirFile.freeSpace

            val (totalBytes, freeBytes) = try {
                // Keep the pair coupled: if either call fails the other is suspect too.
                val statsTotal = storageStatsmanager.getTotalBytes(id)
                val statsFree = storageStatsmanager.getFreeBytes(id)

                val reconciled = StorageSpaceReconcile.reconcilePrimary(
                    statsTotal = statsTotal,
                    statsFree = statsFree,
                    fileTotal = fileTotal,
                    fileFree = fileFree,
                )
                if (reconciled.usedFileFallback) {
                    log(TAG, WARN) {
                        "Primary StorageStats total=$statsTotal implausible vs File=$fileTotal for $id; using File"
                    }
                }
                if (reconciled.normalizedStatsTotal) {
                    log(TAG, WARN) {
                        "Primary StorageStats total=$statsTotal is binary-rounded for $id; using ${reconciled.total}"
                    }
                }
                reconciled.total to reconciled.free
            } catch (e: Exception) {
                log(TAG, WARN) { "StorageStatsManager failed for primary $id, using File API: ${e.message}" }
                fileTotal to fileFree
            }

            DeviceStorage(
                id = id,
                label = mx.loperstack.tokkiphealth.analyzer.R.string.analyzer_storage_type_primary_title.toCaString(),
                type = DeviceStorage.Type.PRIMARY,
                hardware = DeviceStorage.Hardware.BUILT_IN,
                spaceCapacity = totalBytes,
                spaceFree = freeBytes,
                setupIncomplete = setupIncomplete,
            )
        }

        log(TAG) { "Primary: $primaryDevice" }
        updateProgressSecondary(mx.loperstack.tokkiphealth.analyzer.R.string.analyzer_storage_type_secondary_title)

        val secondaryDevices: Set<DeviceStorage> = (storageManager2.volumes ?: emptySet())
            .filter { it.isPrimary == false && it.fsUuid != null && it.isMounted }
            .mapNotNull { volume ->
                updateProgressSecondary(
                    volume.path?.path?.toCaString() ?: mx.loperstack.tokkiphealth.analyzer.R.string.analyzer_storage_type_secondary_title.toCaString()
                )

                val volumeId = StorageId.parseVolumeUuid(volume.fsUuid)
                if (volumeId == null) {
                    log(TAG, WARN) { "Failed to determine UUID of $volume" }
                    return@mapNotNull null
                }

                val id = StorageId(internalId = volume.fsUuid, externalId = volumeId)
                val isFatUuid = volumeId.toString().startsWith(StorageId.FAT_UUID_PREFIX)
                val fileTotal = volume.path?.totalSpace ?: 0L
                val fileFree = volume.path?.freeSpace ?: 0L

                val (totalBytes, freeBytes) = try {
                    // Secondary storage isn't available on all APIs (e.g. not on a Redmi 7A @ Android 9).
                    // Keep the pair coupled: if either call fails the sentinel guard the other is suspect too (#2389).
                    val statsTotal = storageStatsmanager.getTotalBytes(id)
                    val statsFree = storageStatsmanager.getFreeBytes(id)

                    val reconciled = StorageSpaceReconcile.reconcileSecondary(
                        statsTotal = statsTotal,
                        statsFree = statsFree,
                        fileTotal = fileTotal,
                        fileFree = fileFree,
                        isFatUuid = isFatUuid,
                    )
                    if (reconciled.usedFileFallback) {
                        log(TAG, WARN) {
                            "StorageStats total=$statsTotal disagrees with File=$fileTotal for FAT $id; using File"
                        }
                    }
                    reconciled.total to reconciled.free
                } catch (e: Exception) {
                    log(TAG, WARN) { "StorageStatsManager failed for $id, using File API: ${e.message}" }
                    fileTotal to fileFree
                }

                if (totalBytes <= 0L) {
                    log(TAG, WARN) { "Secondary volume reports zero capacity, skipping: $volume" }
                    return@mapNotNull null
                }

                val type = when {
                    volume.disk?.isUsb == true -> DeviceStorage.Type.PORTABLE
                    else -> DeviceStorage.Type.SECONDARY
                }
                val hardware = when {
                    volume.disk?.isUsb == true -> DeviceStorage.Hardware.USB
                    else -> DeviceStorage.Hardware.SDCARD
                }

                DeviceStorage(
                    id = id,
                    label = when (type) {
                        DeviceStorage.Type.PRIMARY -> throw IllegalArgumentException("Can't be primary")
                        DeviceStorage.Type.SECONDARY -> mx.loperstack.tokkiphealth.analyzer.R.string.analyzer_storage_type_secondary_title.toCaString()
                        DeviceStorage.Type.PORTABLE -> mx.loperstack.tokkiphealth.analyzer.R.string.analyzer_storage_type_tertiary_title.toCaString()
                    },
                    type = type,
                    hardware = hardware,
                    spaceCapacity = totalBytes,
                    spaceFree = freeBytes,
                    setupIncomplete = setupIncomplete,
                )
            }
            .toSet()

        log(TAG) { "Secondary devices: $secondaryDevices" }

        return setOf(primaryDevice) + secondaryDevices
    }

    companion object {
        private val TAG = logTag("Analyzer", "DeviceStorage", "Scanner")
    }
}