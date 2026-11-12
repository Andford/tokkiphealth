package mx.loperstack.tokkiphealth.analyzer.core.storage.categories

import mx.loperstack.tokkiphealth.analyzer.core.content.ContentGroup
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed
import mx.loperstack.tokkiphealth.common.storage.StorageId

data class AppCategory(
    override val storageId: StorageId,
    val setupIncomplete: Boolean = false,
    val pkgStats: Map<InstallId, PkgStat>,
) : ContentCategory {

    override val spaceUsed: Long
        get() = pkgStats.values.sumOf { it.totalSize }

    override val groups: Collection<ContentGroup>
        get() = pkgStats.values
            .map { setOfNotNull(it.appCode, it.appData, it.appMedia, it.extraData) }
            .flatten()

    data class PkgStat(
        val pkg: Installed,
        val isShallow: Boolean,
        val appCode: ContentGroup?,
        val appData: ContentGroup?,
        val appMedia: ContentGroup?,
        val extraData: ContentGroup?,
    ) {

        val id: InstallId
            get() = pkg.installId

        val label: CaString
            get() = pkg.label ?: pkg.packageName.toCaString()

        val totalSize by lazy {
            var size = 0L
            appCode?.groupSize?.let { size += it }
            appData?.groupSize?.let { size += it }
            appMedia?.groupSize?.let { size += it }
            extraData?.groupSize?.let { size += it }
            size
        }
    }
}