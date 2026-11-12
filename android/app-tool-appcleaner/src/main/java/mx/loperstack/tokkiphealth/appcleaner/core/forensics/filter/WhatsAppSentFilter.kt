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
import mx.loperstack.tokkiphealth.common.sieve.NameCriterium
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium
import javax.inject.Inject
import javax.inject.Provider

@Reusable
class WhatsAppSentFilter @Inject constructor(
    private val dynamicSieveFactory: DynamicAppSieve2.Factory,
    private val gatewaySwitch: GatewaySwitch,
) : BaseExpendablesFilter() {

    private lateinit var sieve: DynamicAppSieve2

    override suspend fun initialize() {
        log(TAG) { "initialize()" }

        val configs = listOf(
            "com.whatsapp" to "WhatsApp",
            "com.whatsapp.w4b" to "WhatsApp Business"
        )
            .flatMap { (pkg, name) ->
                listOf(
                    WhatsAppMapping(
                        location = DataArea.Type.SDCARD,
                        pkg = pkg,
                        folder1 = name,
                        folder2 = name,
                    ),
                    WhatsAppMapping(
                        location = DataArea.Type.PUBLIC_MEDIA,
                        pkg = pkg,
                        folder1 = "$pkg/$name",
                        folder2 = name,
                    ),
                )
            }
            .map { (location, pkg, folder1, folder2) ->
                DynamicAppSieve2.MatchConfig(
                    pkgNames = setOf(pkg.toPkgId()),
                    areaTypes = setOf(location),
                    pfpCriteria = setOf(
                        SegmentCriterium("$folder1/Media/$folder2 Video/Sent", SegmentCriterium.Mode.Ancestor()),
                        SegmentCriterium(
                            "$folder1/Media/$folder2 Animated Gifs/Sent",
                            SegmentCriterium.Mode.Ancestor()
                        ),
                        SegmentCriterium("$folder1/Media/$folder2 Images/Sent", SegmentCriterium.Mode.Ancestor()),
                        SegmentCriterium("$folder1/Media/$folder2 Audio/Sent", SegmentCriterium.Mode.Ancestor()),
                        SegmentCriterium("$folder1/Media/$folder2 Documents/Sent", SegmentCriterium.Mode.Ancestor()),
                    ),
                    pfpExclusions = setOf(NameCriterium(".nomedia", mode = NameCriterium.Mode.Equal())),
                )
            }
            .toSet()

        sieve = dynamicSieveFactory.create(configs)
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
        private val filterProvider: Provider<WhatsAppSentFilter>
    ) : ExpendablesFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterWhatsAppSentEnabled.value()
        override suspend fun create(): ExpendablesFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): ExpendablesFilter.Factory
    }

    companion object {
        private val TAG = logTag("AppCleaner", "Scanner", "Filter", "WhatsApp", "Sent")
    }
}