package mx.loperstack.tokkiphealth.systemcleaner.core.filter.custom

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.FilterAlt
import androidx.compose.ui.graphics.vector.ImageVector
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import mx.loperstack.tokkiphealth.systemcleaner.R
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.FileType
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.sieve.TypeCriterium
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.BaseSystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.FilterIdentifier
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve

class CustomFilter @AssistedInject constructor(
    @Assisted private val filterConfig: CustomFilterConfig,
    private val systemCrawlerSieveFactory: SystemCrawlerSieve.Factory,
    private val gatewaySwitch: GatewaySwitch,
) : BaseSystemCleanerFilter() {

    override val identifier: FilterIdentifier = filterConfig.identifier

    override val icon: ImageVector = Icons.TwoTone.FilterAlt

    override suspend fun getLabel(): CaString = filterConfig.label.toCaString()

    override suspend fun getDescription(): CaString = R.string.systemcleaner_customfilter_label.toCaString()

    override suspend fun targetAreas(): Set<DataArea.Type> = filterConfig.areas ?: DataArea.Type.entries.toSet()

    private lateinit var sieve: SystemCrawlerSieve

    override suspend fun initialize() {
        val sieveConfig = SystemCrawlerSieve.Config(
            areaTypes = targetAreas(),
            targetTypes = filterConfig.fileTypes?.map {
                when (it) {
                    FileType.DIRECTORY -> TypeCriterium.DIRECTORY
                    FileType.SYMBOLIC_LINK -> TypeCriterium.FILE
                    FileType.FILE -> TypeCriterium.FILE
                    FileType.UNKNOWN -> TypeCriterium.FILE
                }
            }?.toSet(),
            pathCriteria = filterConfig.pathCriteria,
            nameCriteria = filterConfig.nameCriteria,
            pathExclusions = filterConfig.exclusionCriteria,
            minimumSize = filterConfig.sizeMinimum,
            maximumSize = filterConfig.sizeMaximum,
            minimumAge = filterConfig.ageMinimum,
            maximumAge = filterConfig.ageMaximum,
            pathRegexes = filterConfig.pathRegexes,
        )
        sieve = systemCrawlerSieveFactory.create(sieveConfig)
        log(TAG) { "initialized()" }
    }

    override suspend fun match(item: APathLookup<*>): SystemCleanerFilter.Match? {
        if (!sieve.match(item).matches) return null

        return SystemCleanerFilter.Match.Deletion(item)
    }

    override suspend fun process(
        matches: Collection<SystemCleanerFilter.Match>
    ): Collection<SystemCleanerFilter.Processed> {
        return matches.filterIsInstance<SystemCleanerFilter.Match.Deletion>().deleteAll(gatewaySwitch)
    }

    override fun toString(): String = "${this::class.simpleName}(${filterConfig.label})"

    @AssistedFactory
    interface Factory {
        fun create(filterConfig: CustomFilterConfig): CustomFilter
    }

    companion object {
        private val TAG = logTag("SystemCleaner", "Filter", "Custom")
    }
}
