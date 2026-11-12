package mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.appcleaner.core.AppCleanerSettings
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.BaseExpendablesFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.ExpendablesFilter
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.sieves.JsonAppSieve
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.Segments
import mx.loperstack.tokkiphealth.common.files.isAncestorOf
import mx.loperstack.tokkiphealth.common.files.lowercase
import mx.loperstack.tokkiphealth.common.files.segs
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import javax.inject.Inject
import javax.inject.Provider


@Reusable
class RecycleBinsFilter @Inject constructor(
    private val jsonBasedSieveFactory: JsonAppSieve.Factory,
    private val gatewaySwitch: GatewaySwitch,
) : BaseExpendablesFilter() {

    private lateinit var sieve: JsonAppSieve

    override suspend fun initialize() {
        log(TAG) { "initialize()" }
        sieve = jsonBasedSieveFactory.create("expendables/db_trash_files.json")
    }

    override suspend fun match(
        pkgId: Pkg.Id,
        target: APathLookup<APath>,
        areaType: DataArea.Type,
        pfpSegs: Segments
    ): ExpendablesFilter.Match? {
        val lcsegments = pfpSegs.lowercase()

        if (lcsegments.isNotEmpty() && IGNORED_FILES.contains(lcsegments[lcsegments.size - 1])) {
            return null
        }

        //    0      1     2
        // topdir/.trash/file
        if (lcsegments.size >= 3
            && AREAS.contains(areaType)
            && (areaType != DataArea.Type.SDCARD || !segs("Android").isAncestorOf(lcsegments, ignoreCase = true))
            && TRASH_FOLDERS.contains(lcsegments[1])
        ) {
            return target.toDeletionMatch()
        }

        //    0      1     2     3
        // topdir/files/.trash/file
        if (lcsegments.size >= 4
            && AREAS.contains(areaType)
            && (areaType != DataArea.Type.SDCARD || !segs("Android").isAncestorOf(lcsegments, ignoreCase = true))
            && "files" == lcsegments[1]
            && TRASH_FOLDERS.contains(lcsegments[2])
        ) {
            return target.toDeletionMatch()
        }

        //    0        1     2     3
        // Android/.Trash/<pkg>/file
        if (lcsegments.size >= 4
            && "android" == lcsegments[0]
            && ".trash" == lcsegments[1]
            && lcsegments[2] == pkgId.name
        ) {
            return target.toDeletionMatch()
        }

        return if (pfpSegs.isNotEmpty() && sieve.matches(pkgId, areaType, pfpSegs)) {
            target.toDeletionMatch()
        } else {
            null
        }
    }

    override suspend fun process(
        targets: Collection<ExpendablesFilter.Match>,
        allMatches: Collection<ExpendablesFilter.Match>
    ): ExpendablesFilter.ProcessResult {
        return deleteAll(
            targets.map { it as ExpendablesFilter.Match.Deletion },
            gatewaySwitch,
            allMatches
        )
    }

    @Reusable
    class Factory @Inject constructor(
        private val settings: AppCleanerSettings,
        private val filterProvider: Provider<RecycleBinsFilter>
    ) : ExpendablesFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterRecycleBinsEnabled.value()
        override suspend fun create(): ExpendablesFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): ExpendablesFilter.Factory
    }

    companion object {
        private val AREAS = setOf(
            DataArea.Type.SDCARD,
            DataArea.Type.PRIVATE_DATA,
            DataArea.Type.PUBLIC_DATA,
            DataArea.Type.PUBLIC_MEDIA,
        )
        private val TRASH_FOLDERS: Collection<String> = listOf(
            ".trash",
            "trash",
            ".trashfiles",
            "trashfiles",
            ".trashbin",
            "trashbin",
            ".recycle",
            "recycle",
            ".recyclebin",
            "recyclebin",
            ".garbage"
        )
        private val IGNORED_FILES: Collection<String> = listOf(
            ".nomedia",
        )
        private val TAG = logTag("AppCleaner", "Scanner", "Filter", "RecycleBins")
    }
}