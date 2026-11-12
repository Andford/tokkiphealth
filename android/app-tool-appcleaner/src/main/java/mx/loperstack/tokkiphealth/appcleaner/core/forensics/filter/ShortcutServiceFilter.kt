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
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.Segments
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import javax.inject.Inject
import javax.inject.Provider

@Reusable
class ShortcutServiceFilter @Inject constructor(
    private val gatewaySwitch: GatewaySwitch,
) : BaseExpendablesFilter() {

    override suspend fun initialize() {
        log(TAG) { "initialize()" }
    }

    override suspend fun match(
        pkgId: Pkg.Id,
        target: APathLookup<APath>,
        areaType: DataArea.Type,
        pfpSegs: Segments
    ): ExpendablesFilter.Match? {
        // Path structure: /data/system_ce/{userId}/shortcut_service/bitmaps/{packageName}/...
        if (areaType != DataArea.Type.DATA_SYSTEM_CE) return null

        // pfpSegs: ["shortcut_service", "bitmaps", "{packageName}", "file.png", ...]
        if (pfpSegs.size < 4) return null
        if (pfpSegs[0] != "shortcut_service") return null
        if (pfpSegs[1] != "bitmaps") return null

        if (pfpSegs[2] != pkgId.name) return null

        return target.toDeletionMatch()
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
        private val filterProvider: Provider<ShortcutServiceFilter>
    ) : ExpendablesFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterShortcutServiceEnabled.value()
        override suspend fun create(): ExpendablesFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): ExpendablesFilter.Factory
    }

    companion object {
        private val TAG = logTag("AppCleaner", "Scanner", "Filter", "ShortcutService")
    }
}
