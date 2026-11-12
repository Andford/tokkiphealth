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
import mx.loperstack.tokkiphealth.common.compose.icons.OsMac
import mx.loperstack.tokkiphealth.common.compose.icons.SdmIcons
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.segs
import mx.loperstack.tokkiphealth.common.sieve.NameCriterium
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium
import mx.loperstack.tokkiphealth.common.sieve.TypeCriterium
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleanerSettings
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.BaseSystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.toDeletion
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve
import javax.inject.Inject
import javax.inject.Provider

class MacFilesFilter @Inject constructor(
    private val sieveFactory: SystemCrawlerSieve.Factory,
    private val gatewaySwitch: GatewaySwitch,
) : BaseSystemCleanerFilter() {

    override val icon: ImageVector = SdmIcons.OsMac

    override suspend fun getLabel(): CaString = R.string.systemcleaner_filter_macfiles_label.toCaString()

    override suspend fun getDescription(): CaString = R.string.systemcleaner_filter_macfiles_summary.toCaString()

    override suspend fun targetAreas(): Set<DataArea.Type> = setOf(
        DataArea.Type.SDCARD,
        DataArea.Type.PUBLIC_MEDIA,
        DataArea.Type.PORTABLE,
    )

    private lateinit var fileSieve: SystemCrawlerSieve
    private lateinit var directorySieve: SystemCrawlerSieve

    override suspend fun initialize() {
        val fileConfig = SystemCrawlerSieve.Config(
            targetTypes = setOf(TypeCriterium.FILE),
            areaTypes = targetAreas(),
            nameCriteria = setOf(
                NameCriterium("._", mode = NameCriterium.Mode.Start()),
                NameCriterium(".DS_Store", mode = NameCriterium.Mode.Equal()),
            ),
        )
        val directoryConfig = SystemCrawlerSieve.Config(
            targetTypes = setOf(TypeCriterium.DIRECTORY),
            areaTypes = setOf(
                DataArea.Type.SDCARD,
                DataArea.Type.PORTABLE,
            ),
            pfpCriteria = setOf(
                SegmentCriterium(segs(".Trashes"), mode = SegmentCriterium.Mode.Equal()),
                SegmentCriterium(segs(".spotlight"), mode = SegmentCriterium.Mode.Equal()),
                SegmentCriterium(segs(".Spotlight-V100"), mode = SegmentCriterium.Mode.Equal()),
                SegmentCriterium(segs(".fseventsd"), mode = SegmentCriterium.Mode.Equal()),
                SegmentCriterium(segs(".TemporaryItems"), mode = SegmentCriterium.Mode.Equal()),
            ),
        )
        fileSieve = sieveFactory.create(fileConfig)
        directorySieve = sieveFactory.create(directoryConfig)
        log(TAG) { "initialized() with $fileConfig and $directoryConfig" }
    }


    override suspend fun match(item: APathLookup<*>): SystemCleanerFilter.Match? {
        return fileSieve.match(item).toDeletion() ?: directorySieve.match(item).toDeletion()
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
        private val filterProvider: Provider<MacFilesFilter>
    ) : SystemCleanerFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterMacFilesEnabled.value()
        override suspend fun create(): SystemCleanerFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): SystemCleanerFilter.Factory
    }

    companion object {
        private val TAG = logTag("SystemCleaner", "Filter", "MacFiles")
    }
}
