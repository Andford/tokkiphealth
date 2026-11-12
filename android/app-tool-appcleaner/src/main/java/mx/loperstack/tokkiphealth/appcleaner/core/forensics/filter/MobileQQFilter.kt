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
import mx.loperstack.tokkiphealth.appcleaner.core.forensics.sieves.DynamicAppSieve2
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.Segments
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.sieve.CriteriaOperator
import mx.loperstack.tokkiphealth.common.sieve.NameCriterium
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium.Mode
import javax.inject.Inject
import javax.inject.Provider

@Reusable
class MobileQQFilter @Inject constructor(
    private val dynamicSieveFactory: DynamicAppSieve2.Factory,
    private val gatewaySwitch: GatewaySwitch,
) : BaseExpendablesFilter() {

    private lateinit var sieve: DynamicAppSieve2

    override suspend fun initialize() {
        log(TAG) { "initialize()" }
        val configOne = DynamicAppSieve2.MatchConfig(
            pkgNames = setOf("com.tencent.mobileqq".toPkgId()),
            areaTypes = setOf(DataArea.Type.SDCARD, DataArea.Type.PUBLIC_DATA),
            pfpCriteria = setOf(
                SegmentCriterium("Tencent/MobileQQ/chatpic", Mode.Ancestor()),
                SegmentCriterium("Tencent/MobileQQ/shortvideo", Mode.Ancestor()),
                SegmentCriterium("com.tencent.mobileqq/MobileQQ/chatpic", Mode.Ancestor()),
                SegmentCriterium("com.tencent.mobileqq/MobileQQ/shortvideo", Mode.Ancestor()),
            ),
            pfpExclusions = setOf(NameCriterium(".nomedia", mode = NameCriterium.Mode.Equal())),
        )
        val configTwo = DynamicAppSieve2.MatchConfig(
            pkgNames = setOf("com.tencent.mobileqq".toPkgId()),
            areaTypes = setOf(DataArea.Type.SDCARD, DataArea.Type.PUBLIC_DATA),
            pfpCriteria = setOf(
                CriteriaOperator.And(
                    SegmentCriterium("com.tencent.mobileqq/MobileQQ", Mode.Ancestor()),
                    SegmentCriterium("ptt", Mode.Specific(index = 1, backwards = true)),
                ),
                CriteriaOperator.And(
                    SegmentCriterium("Tencent/MobileQQ", Mode.Ancestor()),
                    SegmentCriterium("ptt", Mode.Specific(index = 1, backwards = true)),
                ),
            ),
            pfpExclusions = setOf(NameCriterium(".nomedia", mode = NameCriterium.Mode.Equal())),
        )

        sieve = dynamicSieveFactory.create(setOf(configOne, configTwo))
    }

    override suspend fun match(
        pkgId: Pkg.Id,
        target: APathLookup<APath>,
        areaType: DataArea.Type,
        pfpSegs: Segments
    ): ExpendablesFilter.Match? = if (pfpSegs.isNotEmpty() && sieve.matches(pkgId, target, areaType, pfpSegs)) {
        target.toDeletionMatch()
    } else {
        null
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
        private val filterProvider: Provider<MobileQQFilter>
    ) : ExpendablesFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterMobileQQEnabled.value()
        override suspend fun create(): ExpendablesFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): ExpendablesFilter.Factory
    }

    companion object {
        private val TAG = logTag("AppCleaner", "Scanner", "Filter", "MobileQQ")
    }
}