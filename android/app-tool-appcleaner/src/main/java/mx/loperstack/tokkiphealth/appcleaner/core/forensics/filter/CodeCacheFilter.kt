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
import mx.loperstack.tokkiphealth.common.storage.StorageEnvironment
import javax.inject.Inject
import javax.inject.Provider

@Reusable
class CodeCacheFilter @Inject constructor(
    environment: StorageEnvironment,
    private val gatewaySwitch: GatewaySwitch,
) : BaseExpendablesFilter() {

    private val cacheFolderPrefixes = environment.ourCodeCacheDirs.map { it.name }

    override suspend fun initialize() {
        log(TAG) { "initialize()" }
    }

    override suspend fun match(
        pkgId: Pkg.Id,
        target: APathLookup<APath>,
        areaType: DataArea.Type,
        pfpSegs: Segments
    ): ExpendablesFilter.Match? {
        if (pfpSegs.isNotEmpty() && IGNORED_FILES.contains(pfpSegs[pfpSegs.size - 1])) return null

        return if (pfpSegs.size >= 3 && areaType == DataArea.Type.PRIVATE_DATA && cacheFolderPrefixes.contains(pfpSegs[1])) {
            target.toDeletionMatch()
        } else {
            null
        }
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
        private val filterProvider: Provider<CodeCacheFilter>
    ) : ExpendablesFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterCodeCacheEnabled.value()
        override suspend fun create(): ExpendablesFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): ExpendablesFilter.Factory
    }

    companion object {
        private val TAG = logTag("AppCleaner", "Scanner", "Filter", "CodeCache")
        private val IGNORED_FILES: Collection<String> = listOf()
    }
}