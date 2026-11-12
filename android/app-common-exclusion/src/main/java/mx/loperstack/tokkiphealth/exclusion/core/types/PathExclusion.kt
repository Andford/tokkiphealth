package mx.loperstack.tokkiphealth.exclusion.core.types

import androidx.annotation.Keep
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.isAncestorOf
import mx.loperstack.tokkiphealth.common.files.matches
import mx.loperstack.tokkiphealth.common.serialization.APathSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep // simpleName is part of the persisted ExclusionId, see createId()
@Serializable
data class PathExclusion(
    @SerialName("path") @Serializable(with = APathSerializer::class) val path: APath,
    @SerialName("tags") override val tags: Set<Exclusion.Tag> = setOf(Exclusion.Tag.GENERAL)
) : Exclusion.Path {

    override val id: ExclusionId
        get() = createId(path)

    override val label: CaString
        get() = path.userReadablePath

    override suspend fun match(candidate: APath): Boolean {
        // path = dirA/dirB excludes dirA/dirB and dirA/dirB/file
        val match = candidate.matches(path) || path.isAncestorOf(candidate)
        if (match) log(TAG, VERBOSE) { "Exclusion match: $candidate <- $this " }
        return match
    }

    companion object {
        fun createId(path: APath): ExclusionId = "${PathExclusion::class.simpleName}-${path.path}"
        private val TAG = logTag("Exclusion", "Path")

    }
}
