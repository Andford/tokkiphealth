package mx.loperstack.tokkiphealth.exclusion.core.types

import androidx.annotation.Keep
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.Segments
import mx.loperstack.tokkiphealth.common.files.containsSegments
import mx.loperstack.tokkiphealth.common.files.joinSegments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep // simpleName is part of the persisted ExclusionId, see createId()
@Serializable
data class SegmentExclusion(
    @SerialName("segments") val segments: Segments,
    @SerialName("allowPartial") val allowPartial: Boolean,
    @SerialName("ignoreCase") val ignoreCase: Boolean,
    @SerialName("tags") override val tags: Set<Exclusion.Tag> = setOf(Exclusion.Tag.GENERAL)
) : Exclusion.Path {

    override val id: ExclusionId
        get() = createId(segments)

    override val label: CaString
        get() = caString { segments.joinSegments() }

    override suspend fun match(candidate: APath): Boolean {
        val match = candidate.segments.containsSegments(
            other = this.segments,
            allowPartial = allowPartial,
            ignoreCase = ignoreCase,
        )
        if (match) log(TAG, VERBOSE) { "Exclusion match: $candidate <- $this " }
        return match
    }

    companion object {
        fun createId(segments: Segments): ExclusionId =
            "${SegmentExclusion::class.simpleName}-${segments.joinToString("/")}"

        private val TAG = logTag("Exclusion", "Segment")
    }
}
