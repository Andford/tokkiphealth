package mx.loperstack.tokkiphealth.corpsefinder.core.filter

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.DataAreaManager
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.forensics.FileForensics
import mx.loperstack.tokkiphealth.corpsefinder.R
import mx.loperstack.tokkiphealth.corpsefinder.core.CorpseFinderSettings
import mx.loperstack.tokkiphealth.exclusion.core.ExclusionManager
import javax.inject.Inject
import javax.inject.Provider


@Reusable
class PrivateDataCorpseFilter @Inject constructor(
    areaManager: DataAreaManager,
    gatewaySwitch: GatewaySwitch,
    fileForensics: FileForensics,
    corpseFinderSettings: CorpseFinderSettings,
    exclusionManager: ExclusionManager,
) : StandardCorpseFilter(
    filterTag = TAG,
    areaType = DataArea.Type.PRIVATE_DATA,
    defaultProgressLabel = R.string.corpsefinder_filter_privatedata_label,
    scanProgressLabel = R.string.corpsefinder_filter_privatedata_label,
    capabilityPolicy = CapabilityPolicy.AlwaysRoot,
    // TODO needs to be checked on more rooted ROMs
    untestedFromApi = 37,
    indeterminateWhileListing = true,
    excludedNames = setOf("hosts", "lost+found"),
    areaManager = areaManager,
    gatewaySwitch = gatewaySwitch,
    fileForensics = fileForensics,
    corpseFinderSettings = corpseFinderSettings,
    exclusionManager = exclusionManager,
) {

    @Reusable
    class Factory @Inject constructor(
        private val settings: CorpseFinderSettings,
        private val filterProvider: Provider<PrivateDataCorpseFilter>
    ) : CorpseFilter.Factory {
        override suspend fun isEnabled(): Boolean = settings.filterPrivateDataEnabled.value()
        override suspend fun create(): CorpseFilter = filterProvider.get()
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: Factory): CorpseFilter.Factory
    }

    companion object {
        val TAG: String = logTag("CorpseFinder", "Filter", "PrivateData")
    }
}
