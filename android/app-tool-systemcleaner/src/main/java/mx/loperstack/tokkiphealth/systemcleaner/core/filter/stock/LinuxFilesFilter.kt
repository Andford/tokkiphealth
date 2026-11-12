package mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock

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
import mx.loperstack.tokkiphealth.common.compose.icons.Penguin
import mx.loperstack.tokkiphealth.common.compose.icons.SdmIcons
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.segs
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium
import mx.loperstack.tokkiphealth.common.sieve.TypeCriterium
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleanerSettings
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.BaseSystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.toDeletion
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve
import javax.inject.Inject
import javax.inject.Provider

class LinuxFilesFilter @Inject constructor(
    private val sieveFactory: SystemCrawlerSieve.Factory,
    private val gatewaySwitch: GatewaySwitch,
) : BaseSystemCleanerFilter() {

    override val icon: ImageVector = SdmIcons.Penguin

    override suspend fun getLabel(): CaString = R.string.systemcleaner_filter_linuxfiles_label.toCaString()

    override suspend fun getDescription(): CaString = R.string.systemcleaner_filter_linuxfiles_summary.toCaString()

    override suspend fun targetAreas(): Set<DataArea.Type> = setOf(
        DataArea.Type.SDCARD,
        DataArea.Type.PORTABLE,
    )

    private lateinit var sieve: SystemCrawlerSieve

    override suspend fun initialize() {
        val config = SystemCrawlerSieve.Config(
            targetTypes = setOf(TypeCriterium.DIRECTORY),
            areaTypes = targetAreas(),
            pfpCriteria = setOf(
                SegmentCriterium(segs(".Trash"), mode = SegmentCriterium.Mode.Ancestor()),
                SegmentCriterium(segs(".Trash-"), mode = SegmentCriterium.Mode.Start(allowPartial = true)),
            ),
            pfpExclusions = setOf(
                SegmentCriterium(segs("Android"), mode = SegmentCriterium.Mode.Ancestor())
            )
        )
        sieve = sieveFactory.create(config)
        log(TAG) { "initialized() with $config" }
    }


    override suspend fun match(item: APathLookup<*>): SystemCleanerFilter.Match? {
        val result = sieve.match(item)
        if (!result.matches) return null
        // `.Trash-1000` is Linux trash, `.trash-storage` is Android's own trash and holds restorable files
        val topDir = result.areaInfo?.prefixFreeSegments?.firstOrNull() ?: return null
        if (!TRASH_DIR.matches(topDir)) return null
        return result.toDeletion()
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
        private val filterProvider: Provider<LinuxFilesFilter>
    ) : SystemCleanerFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterLinuxFilesEnabled.value()
        override suspend fun create(): SystemCleanerFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): SystemCleanerFilter.Factory
    }

    companion object {
        private val TAG = logTag("SystemCleaner", "Filter", "LinuxFiles")
        private val TRASH_DIR = Regex("^\\.Trash(-\\d+)?$", RegexOption.IGNORE_CASE)
    }
}