package mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.RunningWithErrors
import androidx.compose.ui.graphics.vector.ImageVector
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.systemcleaner.R
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.DataAreaManager
import mx.loperstack.tokkiphealth.common.areas.currentAreas
import mx.loperstack.tokkiphealth.common.areas.hasFlags
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
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
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.toDeletion
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve
import javax.inject.Inject
import javax.inject.Provider

class AnrFilter @Inject constructor(
    private val sieveFactory: SystemCrawlerSieve.Factory,
    private val areaManager: DataAreaManager,
    private val gatewaySwitch: GatewaySwitch,
) : BaseSystemCleanerFilter() {

    override val icon: ImageVector = Icons.TwoTone.RunningWithErrors

    override suspend fun getLabel(): CaString = R.string.systemcleaner_filter_anr_label.toCaString()

    override suspend fun getDescription(): CaString = R.string.systemcleaner_filter_anr_summary.toCaString()

    override suspend fun targetAreas(): Set<DataArea.Type> = setOf(
        DataArea.Type.DATA,
        DataArea.Type.DOWNLOAD_CACHE,
    )

    private var sieve: SystemCrawlerSieve? = null

    override suspend fun initialize() {
        val regexPairs = areaManager.currentAreas()
            .filter { targetAreas().contains(it.type) }
            .filter { it.hasFlags(DataArea.Flag.PRIMARY) }
            .map { area ->
                val path = area.path.child("anr")
                val regex = String.format(
                    "^(?:%s/anr/[\\W\\w]+)$".replace("/", "\\/"),
                    area.path.path.replace("\\", "\\\\")
                )
                path to regex
            }

        if (regexPairs.isNotEmpty()) {
            val config = SystemCrawlerSieve.Config(
                targetTypes = setOf(TypeCriterium.FILE),
                areaTypes = targetAreas(),
                pfpCriteria = setOf(
                    SegmentCriterium(segs("anr"), mode = SegmentCriterium.Mode.Ancestor())
                ),
                pathRegexes = regexPairs.map { Regex(it.second) }.toSet(),
            )
            sieve = sieveFactory.create(config)
            log(TAG) { "initialized() with $config" }
        } else {
            log(TAG, WARN) { "Sieve is underdefined" }
        }
    }

    override suspend fun match(item: APathLookup<*>): SystemCleanerFilter.Match? {
        if (sieve == null) log(TAG, INFO) { "Sieve is underdefined, skipping match" }
        return sieve?.match(item)?.toDeletion()
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
        private val filterProvider: Provider<AnrFilter>,
        private val rootManager: RootManager,
    ) : SystemCleanerFilter.Factory {

        override suspend fun isEnabled(): Boolean {
            val enabled = settings.filterAnrEnabled.value()
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
        private val TAG = logTag("SystemCleaner", "Filter", "Anr")
    }
}