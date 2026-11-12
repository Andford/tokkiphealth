package mx.loperstack.tokkiphealth.systemcleaner.core.filter.stock

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AdsClick
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
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.isDirectory
import mx.loperstack.tokkiphealth.common.files.segs
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium.Mode
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleanerSettings
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.BaseSystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

@Reusable
class AdvertisementFilter @Inject constructor(
    private val sieveFactory: SystemCrawlerSieve.Factory,
    private val areaManager: DataAreaManager,
    private val gatewaySwitch: GatewaySwitch,
) : BaseSystemCleanerFilter() {

    override val icon: ImageVector = Icons.TwoTone.AdsClick

    override suspend fun getLabel(): CaString = R.string.systemcleaner_filter_advertisements_label.toCaString()

    override suspend fun getDescription(): CaString = R.string.systemcleaner_filter_advertisements_summary.toCaString()

    override suspend fun targetAreas(): Set<DataArea.Type> = setOf(
        DataArea.Type.SDCARD,
    )

    private lateinit var sieve: SystemCrawlerSieve

    override suspend fun initialize() {
        val pfpCriteria = mutableSetOf<SegmentCriterium>()
        val rawRegexes = mutableSetOf<String>()

        // TODO this doesn't work on SAFPath files

        areaManager.currentAreas()
            .filter { targetAreas().contains(it.type) }
            .map { it.path }
            .forEach { toCheck ->
                pfpCriteria.add(SegmentCriterium(segs("ppy_cross"), mode = Mode.Equal()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/ppy_cross)$".replace("/", "\\${File.separator}"),
                        toCheck.path.replace("\\", "\\\\")
                    )
                )
                pfpCriteria.add(SegmentCriterium(segs(".mologiq"), mode = Mode.Start(allowPartial = true)))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:\\.mologiq|\\.mologiq/.+)$".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )
                )
                pfpCriteria.add(SegmentCriterium(segs(".Adcenix"), mode = Mode.Start()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:\\.Adcenix|\\.Adcenix/.+)$".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )
                )
                pfpCriteria.add(SegmentCriterium(segs("ApplifierVideoCache"), mode = Mode.Start()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:ApplifierVideoCache|ApplifierVideoCache/.+)".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )
                )
                pfpCriteria.add(SegmentCriterium(segs("burstlyVideoCache"), mode = Mode.Start()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:burstlyVideoCache|burstlyVideoCache/.+)".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )
                )
                pfpCriteria.add(SegmentCriterium(segs("UnityAdsVideoCache"), mode = Mode.Start()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:UnityAdsVideoCache|UnityAdsVideoCache/.+)".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )
                )
                pfpCriteria.add(SegmentCriterium(segs("ApplifierImageCache"), mode = Mode.Start()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:ApplifierImageCache|ApplifierImageCache/.+)".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )
                )
                pfpCriteria.add(SegmentCriterium(segs("burstlyImageCache"), mode = Mode.Start()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:burstlyImageCache|burstlyImageCache/.+)".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )

                )
                pfpCriteria.add(SegmentCriterium(segs("UnityAdsImageCache"), mode = Mode.Start()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:UnityAdsImageCache|UnityAdsImageCache/.+)".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )

                )
                pfpCriteria.add(SegmentCriterium(segs("__chartboost"), mode = Mode.Start()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:__chartboost|__chartboost/.+)$".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )

                )
                pfpCriteria.add(SegmentCriterium(segs(".chartboost"), mode = Mode.Start()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:\\.chartboost|\\.chartboost/.+)$".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )

                )
                pfpCriteria.add(SegmentCriterium(segs("adhub"), mode = Mode.Start()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:adhub|adhub/.+)$".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )
                )
                pfpCriteria.add(SegmentCriterium(segs(".mobvista"), mode = Mode.Start(allowPartial = true)))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:\\.mobvista\\d+|\\.mobvista\\d+/.+)$".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )
                )
                pfpCriteria.add(SegmentCriterium(segs(".goadsdk"), mode = Mode.Start()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:\\.goadsdk|\\.goadsdk/.+)$".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )
                )
                pfpCriteria.add(SegmentCriterium(segs(".goproduct"), mode = Mode.Start()))
                rawRegexes.add(
                    String.format(
                        "^(?:%s/)(?:\\.goproduct|\\.goproduct/.+)$".replace("/", "\\" + File.separator),
                        toCheck.path.replace("\\", "\\\\")
                    )
                )
            }

        val config = SystemCrawlerSieve.Config(
            areaTypes = targetAreas(),
            pfpCriteria = pfpCriteria,
            pathRegexes = rawRegexes.map { Regex(it) }.toSet()
        )
        sieve = sieveFactory.create(config)
        log(TAG) { "initialized() with $config" }
    }

    override suspend fun match(item: APathLookup<*>): SystemCleanerFilter.Match? {
        val sieveResult = sieve.match(item)
        if (!sieveResult.matches) return null

        if (item.name.endsWith("chartboost") && !item.isDirectory) {
            return null
        }

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
        private val filterProvider: Provider<AdvertisementFilter>
    ) : SystemCleanerFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterAdvertisementsEnabled.value()
        override suspend fun create(): SystemCleanerFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): SystemCleanerFilter.Factory
    }

    companion object {
        private val TAG = logTag("SystemCleaner", "Filter", "Advertisements")
    }
}