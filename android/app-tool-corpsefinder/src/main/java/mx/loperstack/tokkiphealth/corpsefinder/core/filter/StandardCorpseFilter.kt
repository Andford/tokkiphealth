package mx.loperstack.tokkiphealth.corpsefinder.core.filter

import androidx.annotation.StringRes
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.DataAreaManager
import mx.loperstack.tokkiphealth.common.areas.currentAreas
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.ReadException
import mx.loperstack.tokkiphealth.common.files.listFiles
import mx.loperstack.tokkiphealth.common.files.local.LocalGateway
import mx.loperstack.tokkiphealth.common.forensics.FileForensics
import mx.loperstack.tokkiphealth.common.forensics.OwnerInfo
import mx.loperstack.tokkiphealth.common.hasApiLevel
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.progress.updateProgressCount
import mx.loperstack.tokkiphealth.common.progress.updateProgressPrimary
import mx.loperstack.tokkiphealth.common.progress.updateProgressSecondary
import mx.loperstack.tokkiphealth.corpsefinder.core.Corpse
import mx.loperstack.tokkiphealth.corpsefinder.core.CorpseFinderSettings
import mx.loperstack.tokkiphealth.exclusion.core.ExclusionManager
import mx.loperstack.tokkiphealth.exclusion.core.pathExclusions
import mx.loperstack.tokkiphealth.exclusion.core.types.PathExclusionIndex
import mx.loperstack.tokkiphealth.main.core.SDMTool

/**
 * The scan skeleton shared by the filters that check the top level content of a single data area:
 * gate on the gateway's capabilities, list each area, drop excluded paths, hand the rest to
 * [filterCandidates].
 *
 * [DalvikCorpseFilter] is not one of these, it walks two areas in one scan.
 */
abstract class StandardCorpseFilter(
    private val filterTag: String,
    private val areaType: DataArea.Type,
    @param:StringRes private val defaultProgressLabel: Int,
    @param:StringRes private val scanProgressLabel: Int,
    private val capabilityPolicy: CapabilityPolicy,
    /** Skip the scan entirely from this API level on, for areas that no longer exist. */
    private val apiBailAt: Int? = null,
    /** Scan but withhold the findings from this API level on, see [CorpseFilter.untestedApiCeiling]. */
    untestedFromApi: Int? = null,
    private val indeterminateWhileListing: Boolean = false,
    private val excludedNames: Set<String> = emptySet(),
    private val onOwnerFound: ((OwnerInfo) -> Unit)? = null,
    private val areaManager: DataAreaManager,
    private val gatewaySwitch: GatewaySwitch,
    private val fileForensics: FileForensics,
    private val corpseFinderSettings: CorpseFinderSettings,
    private val exclusionManager: ExclusionManager,
) : CorpseFilter(filterTag, Progress.Data(primary = defaultProgressLabel.toCaString())) {

    final override val untestedApiCeiling: Int? = untestedFromApi

    /** What the [LocalGateway] has to offer before a scan makes sense. */
    sealed interface CapabilityPolicy {
        /** No gateway is needed at all. */
        data object None : CapabilityPolicy

        data object AlwaysRoot : CapabilityPolicy

        data class RootFromApi(val api: Int) : CapabilityPolicy

        data class RootOrAdbFromApi(val api: Int) : CapabilityPolicy
    }

    final override suspend fun doScan(): Collection<Corpse> {
        log(filterTag) { "Scanning..." }

        if (!hasRequiredCapabilities()) return emptySet()

        if (apiBailAt != null && hasApiLevel(apiBailAt)) {
            log(filterTag, WARN) { "Area is no longer used from API $apiBailAt on, skipping." }
            return emptySet()
        }

        updateProgressPrimary(scanProgressLabel)

        val exclusionIndex = PathExclusionIndex(exclusionManager.pathExclusions(SDMTool.Type.CORPSEFINDER))

        return areaManager.currentAreas()
            .filter { it.type == areaType }
            .map { area ->
                updateProgressSecondary {
                    it.getString(mx.loperstack.tokkiphealth.common.R.string.general_progress_processing_x, area.label.get(it))
                }
                if (indeterminateWhileListing) updateProgressCount(Progress.Count.Indeterminate())

                log(filterTag) { "Reading $area" }
                val listing = try {
                    area.path.listFiles(gatewaySwitch)
                } catch (e: ReadException) {
                    log(filterTag, WARN) { "Skipping unreadable area $area: ${e.asLog()}" }
                    emptyList()
                }
                val topLevelContents = listing
                    .filter { path ->
                        val isExcluded = exclusionIndex.matches(path)
                        if (isExcluded) log(filterTag, INFO) { "Excluded due to path exclusion: $path" }
                        !isExcluded
                    }

                log(filterTag) { "Filtering $area" }
                doFilter(topLevelContents)
            }
            .flatten()
    }

    private suspend fun hasRequiredCapabilities(): Boolean = when (val policy = capabilityPolicy) {
        is CapabilityPolicy.None -> true

        is CapabilityPolicy.AlwaysRoot -> {
            val gateway = gatewaySwitch.getGateway(APath.PathType.LOCAL) as LocalGateway
            if (gateway.hasRoot()) {
                true
            } else {
                log(filterTag) { "LocalGateway has no root, skipping." }
                false
            }
        }

        is CapabilityPolicy.RootFromApi -> {
            val gateway = gatewaySwitch.getGateway(APath.PathType.LOCAL) as LocalGateway
            if (hasApiLevel(policy.api) && !gateway.hasRoot()) {
                log(filterTag) { "LocalGateway has no root, skipping public data on Android 13" }
                false
            } else {
                true
            }
        }

        is CapabilityPolicy.RootOrAdbFromApi -> {
            val gateway = gatewaySwitch.getGateway(APath.PathType.LOCAL) as LocalGateway
            if (hasApiLevel(policy.api) && !gateway.hasRoot() && !gateway.hasAdb()) {
                log(filterTag) { "LocalGateway has no root/adb, skipping public data on Android 13+" }
                false
            } else {
                true
            }
        }
    }

    private suspend fun doFilter(candidates: Collection<APath>): Collection<Corpse> = filterCandidates(
        candidates = candidates,
        areaType = areaType,
        filterType = this::class,
        includeRiskKeeper = corpseFinderSettings.includeRiskKeeper.value(),
        includeRiskCommon = corpseFinderSettings.includeRiskCommon.value(),
        tag = filterTag,
        progress = this,
        gatewaySwitch = gatewaySwitch,
        fileForensics = fileForensics,
        shouldExcludeCandidate = { shouldExcludeCandidate(it) },
        onOwnerFound = onOwnerFound,
    )

    private fun shouldExcludeCandidate(path: APath): Boolean = excludedNames.contains(path.name)
}
