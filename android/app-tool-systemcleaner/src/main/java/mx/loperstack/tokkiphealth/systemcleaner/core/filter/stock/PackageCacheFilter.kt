package mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Apps
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
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.segs
import mx.loperstack.tokkiphealth.common.root.RootManager
import mx.loperstack.tokkiphealth.common.root.canUseRootNow
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium.Mode
import mx.loperstack.tokkiphealth.common.sieve.TypeCriterium
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleanerSettings
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.BaseSystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.toDeletion
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve.Config
import javax.inject.Inject
import javax.inject.Provider

/**
 * https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/services/core/java/com/android/server/pm/PackageManagerServiceUtils.java;l=1345
 * https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/os/Environment.java;drc=84fe39403a12f7a0aeca462b1cae46fea105e2fb;l=691
 */
@Reusable
class PackageCacheFilter @Inject constructor(
    private val sieveFactory: SystemCrawlerSieve.Factory,
    private val gatewaySwitch: GatewaySwitch,
) : BaseSystemCleanerFilter() {

    override val icon: ImageVector = Icons.TwoTone.Apps

    override suspend fun getLabel(): CaString = R.string.systemcleaner_filter_packagecaches_label.toCaString()

    override suspend fun getDescription(): CaString = R.string.systemcleaner_filter_packagecaches_summary.toCaString()

    override suspend fun targetAreas(): Set<DataArea.Type> = sieve.config.areaTypes!!

    private lateinit var sieve: SystemCrawlerSieve

    override suspend fun initialize() {
        val config = Config(
            targetTypes = setOf(TypeCriterium.FILE, TypeCriterium.DIRECTORY),
            areaTypes = setOf(DataArea.Type.DATA_SYSTEM),
            pfpCriteria = setOf(SegmentCriterium(segs("package_cache"), mode = Mode.Ancestor())),
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
        private val filterProvider: Provider<PackageCacheFilter>,
        private val rootManager: RootManager,
    ) : SystemCleanerFilter.Factory {

        override suspend fun isEnabled(): Boolean {
            val enabled = settings.filterPackageCacheEnabled.value()
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
        private val TAG = logTag("SystemCleaner", "Filter", "PackageCache")
    }
}