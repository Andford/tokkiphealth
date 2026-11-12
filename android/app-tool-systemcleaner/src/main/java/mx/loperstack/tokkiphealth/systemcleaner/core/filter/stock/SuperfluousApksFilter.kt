package mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.LibraryAdd
import androidx.compose.ui.graphics.vector.ImageVector
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.systemcleaner.R
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.cache.CacheRepo
import mx.loperstack.tokkiphealth.common.compression.entries
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.copyToAutoClose
import mx.loperstack.tokkiphealth.common.files.core.local.deleteRecursivelySafe
import mx.loperstack.tokkiphealth.common.files.file
import mx.loperstack.tokkiphealth.common.files.inputStream
import mx.loperstack.tokkiphealth.common.files.local.toLocalPath
import mx.loperstack.tokkiphealth.common.files.segs
import mx.loperstack.tokkiphealth.common.hashing.Hasher
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.get
import mx.loperstack.tokkiphealth.common.pkgs.isInstalled
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.sieve.NameCriterium
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium
import mx.loperstack.tokkiphealth.common.sieve.TypeCriterium
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleanerSettings
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.BaseSystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Provider

class SuperfluousApksFilter @Inject constructor(
    private val sieveFactory: SystemCrawlerSieve.Factory,
    private val pkgOps: PkgOps,
    private val pkgRepo: PkgRepo,
    private val gatewaySwitch: GatewaySwitch,
    private val cacheRepo: CacheRepo,
    private val settings: SystemCleanerSettings,
) : BaseSystemCleanerFilter() {

    override val icon: ImageVector = Icons.TwoTone.LibraryAdd

    override suspend fun getLabel(): CaString = R.string.systemcleaner_filter_superfluosapks_label.toCaString()

    override suspend fun getDescription(): CaString = R.string.systemcleaner_filter_superfluosapks_summary.toCaString()

    override suspend fun targetAreas(): Set<DataArea.Type> = setOf(
        DataArea.Type.SDCARD,
        DataArea.Type.PORTABLE,
    )

    private val cacheDir by lazy {
        File(cacheRepo.baseCacheDir, "systemcleaner/filter/superfluousapks").also { dir ->
            dir.mkdirs()
            dir.let { it.listFiles()?.toList() }?.forEach {
                log(TAG, WARN) { "Deleting stale cache data: $it" }
                it.deleteRecursivelySafe()
            }
        }
    }

    private lateinit var sieve: SystemCrawlerSieve
    private var includeSameVersion: Boolean = true

    override suspend fun initialize() {
        includeSameVersion = settings.filterSuperfluosApksIncludeSameVersion.value()
        val config = SystemCrawlerSieve.Config(
            targetTypes = setOf(TypeCriterium.FILE),
            areaTypes = targetAreas(),
            nameCriteria = setOf(
                NameCriterium(".apk", mode = NameCriterium.Mode.End()),
                NameCriterium(".apks", mode = NameCriterium.Mode.End()),
            ),
            pathExclusions = EXCLUSIONS
        )
        sieve = sieveFactory.create(config)
        log(TAG) { "initialized() with $config" }
    }

    override suspend fun match(item: APathLookup<*>): SystemCleanerFilter.Match? {
        val sieveResult = sieve.match(item)
        if (!sieveResult.matches) return null
        log(TAG, VERBOSE) { "Passed sieve, checking $item" }

        val apkInfo = when {
            item.name.endsWith(".apk") -> {
                pkgOps.viewArchive(item.lookedUp)
            }

            item.name.endsWith(".apks") -> withContext(NonCancellable) {
                val checksum = item.file(gatewaySwitch, readWrite = false).source().use {
                    Hasher(Hasher.Type.MD5).calc(it)
                }.format()
                log(TAG, VERBOSE) { "Checksum is $checksum for ${item.path}" }

                val baseNames = setOf("base.apk")

                val extractedDir = File(cacheDir, checksum).also { it.mkdirs() }
                try {
                    val extractedBase = File(extractedDir, "base.apk")

                    if (extractedBase.exists()) {
                        log(TAG, WARN) { "Why do we already have extracted this?: $extractedBase" }
                    } else if (cacheRepo.canSpare(item.size)) {
                        item.file(gatewaySwitch, readWrite = false).source().use { apksSource ->
                            ZipInputStream(apksSource.inputStream()).use { zis ->
                                zis.entries
                                    .find { (_, entry) ->
                                        log(TAG, VERBOSE) { "Checking archive entry: ${item.path}/${entry.name}" }
                                        baseNames.contains(entry.name)
                                    }
                                    ?.let { (stream, _) ->
                                        log(TAG, VERBOSE) { "Extracting to $extractedBase" }
                                        stream.source().buffer().use { it.copyToAutoClose(extractedBase) }
                                    }
                            }
                        }
                        log(TAG, VERBOSE) { "$extractedBase is ${extractedBase.length()}" }
                    } else {
                        log(TAG, WARN) { "Don't have enough cache space to extract $item" }
                    }

                    if (extractedBase.exists()) pkgOps.viewArchive(extractedBase.toLocalPath()) else null
                } finally {
                    extractedDir.deleteRecursivelySafe()
                }
            }

            else -> null
        }

        if (apkInfo == null) {
            log(TAG, WARN) { "Failed to read archive: $item" }
            return null
        }

        log(TAG) { "Checking status for ${apkInfo.packageName} (${apkInfo.versionCode})" }

        // TODO Multiple profiles can't have different versions of the same APK, right?
        // An archived or uninstalled-with-data app still has a version, but the APK may be its only way back
        val installed = pkgRepo.get(apkInfo.id).firstOrNull { it.isInstalled }
        if (installed == null) {
            log(TAG, VERBOSE) {
                "Not superfluous (not installed): ${apkInfo.packageName} (archive=${apkInfo.versionCode})"
            }
            return null
        }

        val superfluos = if (includeSameVersion) {
            installed.versionCode >= apkInfo.versionCode
        } else {
            installed.versionCode > apkInfo.versionCode
        }
        if (superfluos) {
            log(TAG, VERBOSE) {
                "Superfluos: ${installed.packageName} installed=${installed.versionCode}, archive=${apkInfo.versionCode}"
            }
        } else {
            log(TAG, VERBOSE) {
                "Not superfluous (archive is newer): ${installed.packageName} installed=${installed.versionCode}, archive=${apkInfo.versionCode}"
            }
        }
        return if (superfluos) SystemCleanerFilter.Match.Deletion(item) else null
    }

    override suspend fun process(
        matches: Collection<SystemCleanerFilter.Match>
    ): Collection<SystemCleanerFilter.Processed> {
        return matches.filterIsInstance<SystemCleanerFilter.Match.Deletion>().deleteAll(gatewaySwitch)
    }

    override fun toString(): String = "${this::class.simpleName}(${hashCode()})"

    @Reusable
    class Factory @Inject constructor(
        private val settings: SystemCleanerSettings,
        private val filterProvider: Provider<SuperfluousApksFilter>
    ) : SystemCleanerFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterSuperfluosApksEnabled.value()
        override suspend fun create(): SystemCleanerFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): SystemCleanerFilter.Factory
    }

    companion object {
        private val TAG = logTag("SystemCleaner", "Filter", "SuperfluousApks")
        val EXCLUSIONS = setOf(
            SegmentCriterium(segs("Backup"), mode = SegmentCriterium.Mode.Contain()),
            SegmentCriterium(segs("Backups"), mode = SegmentCriterium.Mode.Contain()),
            SegmentCriterium(segs("Recover"), mode = SegmentCriterium.Mode.Contain()),
            SegmentCriterium(segs("Recovery"), mode = SegmentCriterium.Mode.Contain()),
            SegmentCriterium(segs("TWRP"), mode = SegmentCriterium.Mode.Contain()),
        )
    }
}