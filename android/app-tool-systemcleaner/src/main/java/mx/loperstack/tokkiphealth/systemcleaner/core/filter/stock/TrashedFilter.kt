package mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Delete
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
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.sieve.NameCriterium
import mx.loperstack.tokkiphealth.common.sieve.TypeCriterium
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleanerSettings
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.BaseSystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.toDeletion
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve
import javax.inject.Inject
import javax.inject.Provider

class TrashedFilter @Inject constructor(
    private val sieveFactory: SystemCrawlerSieve.Factory,
    private val gatewaySwitch: GatewaySwitch,
) : BaseSystemCleanerFilter() {

    override val icon: ImageVector = Icons.TwoTone.Delete

    override suspend fun getLabel(): CaString = R.string.systemcleaner_filter_trashed_label.toCaString()

    override suspend fun getDescription(): CaString {
        return R.string.systemcleaner_filter_trashed_summary.toCaString()
    }

    override suspend fun targetAreas(): Set<DataArea.Type> = setOf(
        DataArea.Type.SDCARD,
        DataArea.Type.PORTABLE,
    )

    private lateinit var sieve: SystemCrawlerSieve

    override suspend fun initialize() {
        val config = SystemCrawlerSieve.Config(
            targetTypes = setOf(TypeCriterium.FILE),
            areaTypes = targetAreas(),
            nameCriteria = setOf(
                NameCriterium(".trashed-", mode = NameCriterium.Mode.Start()),
            ),
        )
        sieve = sieveFactory.create(config)
        log(TAG) { "initialized() with $config" }
    }

    override suspend fun match(item: APathLookup<*>): SystemCleanerFilter.Match? {
        return sieve.match(item).toDeletion()
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
        private val filterProvider: Provider<TrashedFilter>
    ) : SystemCleanerFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterTrashedEnabled.value()
        override suspend fun create(): SystemCleanerFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): SystemCleanerFilter.Factory
    }

    companion object {
        private val TAG = logTag("SystemCleaner", "Filter", "Trashed")
    }
}
