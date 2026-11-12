package mx.loperstack.tokkiphealth.exclusion.core.types

import androidx.annotation.Keep
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.getLabel2
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep // simpleName is part of the persisted ExclusionId, see createId()
@Serializable
data class PkgExclusion(
    @SerialName("pkgId") val pkgId: Pkg.Id,
    @SerialName("tags") override val tags: Set<Exclusion.Tag> = setOf(Exclusion.Tag.GENERAL)
) : Exclusion.Pkg {

    override val id: ExclusionId
        get() = createId(pkgId)

    override val label: CaString
        get() = caString { it.packageManager.getLabel2(pkgId) ?: pkgId.name }

    override suspend fun match(candidate: Pkg.Id): Boolean {
        val match = pkgId == candidate
        if (match) log(TAG, VERBOSE) { "Exclusion match: $candidate <- $this " }
        return match
    }

    companion object {
        fun createId(pkgId: Pkg.Id): ExclusionId = "${PkgExclusion::class.simpleName}-${pkgId.name}"
        private val TAG = logTag("Exclusion", "Pkg")
    }
}
