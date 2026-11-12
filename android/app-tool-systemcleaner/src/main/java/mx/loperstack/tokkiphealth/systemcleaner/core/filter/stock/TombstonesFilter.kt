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
import mx.loperstack.tokkiphealth.common.compose.icons.SdmIcons
import mx.loperstack.tokkiphealth.common.compose.icons.Tombstone
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.segs
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.root.canUseRootNow
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium
import mx.loperstack.tokkiphealth.common.sieve.TypeCriterium
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleanerSettings
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.BaseSystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve
import javax.inject.Inject
import javax.inject.Provider

class TombstonesFilter @Inject constructor(
    private val sieveFactory: SystemCrawlerSieve.Factory,
    private val gatewaySwitch: GatewaySwitch,
) : BaseSystemCleanerFilter() {

    override val icon: ImageVector = SdmIcons.Tombstone

    override suspend fun getLabel(): CaString = R.string.systemcleaner_filter_tombstones_label.toCaString()

    override suspend fun getDescription(): CaString = R.string.systemcleaner_filter_tombstones_summary.toCaString()

    override suspend fun targetAreas(): Set<DataArea.Type> = sieves.map { it.config.areaTypes!! }.flatten().toSet()

    private lateinit var sieves: List<SystemCrawlerSieve>

    override suspend fun initialize() {
        val toLoad = mutableListOf<SystemCrawlerSieve.Config>()

        SystemCrawlerSieve.Config(
            targetTypes = setOf(TypeCriterium.FILE),
            areaTypes = setOf(DataArea.Type.DATA),
            pfpCriteria = setOf(SegmentCriterium(segs("tombstones"), mode = SegmentCriterium.Mode.Ancestor())),
        ).run { toLoad.add(this) }
        SystemCrawlerSieve.Config(
            targetTypes = setOf(TypeCriterium.FILE),
            areaTypes = setOf(DataArea.Type.DATA_VENDOR),
            pfpCriteria = setOf(SegmentCriterium(segs("tombstones"), mode = SegmentCriterium.Mode.Ancestor())),
        ).run { toLoad.add(this) }

        log(TAG) { "initialized() with $toLoad" }
        sieves = toLoad.map { sieveFactory.create(it) }
    }


    override suspend fun match(item: APathLookup<*>): SystemCleanerFilter.Match? {
        val match = sieves.firstOrNull { it.match(item).matches }
        if (match == null) return null

        return SystemCleanerFilter.Match.Deletion(item)
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
        private val filterProvider: Provider<TombstonesFilter>,
        private val rootManager: RootManager,
    ) : SystemCleanerFilter.Factory {

        override suspend fun isEnabled(): Boolean {
            val enabled = settings.filterTombstonesEnabled.value()
            val useRoot = rootManager.canUseRootNow()
            if (enabled && !useRoot) log(TAG, INFO) { "Filter is enabled, but requires root, which is unavailable." }
            return enabled && useRoot
        }

        override suspend fun create(): SystemCleanerFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): SystemCleanerFilter.Factory
    }

    companion object {
        private val TAG = logTag("SystemCleaner", "Filter", "Tombstones")
    }
}
