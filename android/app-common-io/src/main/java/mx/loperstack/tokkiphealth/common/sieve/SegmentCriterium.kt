package mx.loperstack.tokkiphealth.common.sieve

import mx.loperstack.tokkiphealth.common.files.Segments
import mx.loperstack.tokkiphealth.common.files.containsSegments
import mx.loperstack.tokkiphealth.common.files.endsWith
import mx.loperstack.tokkiphealth.common.files.isAncestorOf
import mx.loperstack.tokkiphealth.common.files.matches
import mx.loperstack.tokkiphealth.common.files.segmentContains
import mx.loperstack.tokkiphealth.common.files.startsWith
import mx.loperstack.tokkiphealth.common.files.toSegs
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@Parcelize
data class SegmentCriterium(
    @SerialName("segments") val segments: Segments,
    @SerialName("mode") override val mode: Mode,
) : SieveCriterium {

    constructor(rawSegments: String, mode: Mode) : this(rawSegments.toSegs(), mode)

    fun matchRaw(rawSegments: String) = match(rawSegments.toSegs())

    fun match(target: Segments): Boolean = when (mode) {
        is Mode.Ancestor -> segments.isAncestorOf(target, ignoreCase = mode.ignoreCase)
        is Mode.Start -> target.startsWith(segments, ignoreCase = mode.ignoreCase, allowPartial = mode.allowPartial)
        is Mode.End -> target.endsWith(segments, ignoreCase = mode.ignoreCase, allowPartial = mode.allowPartial)
        is Mode.Contain -> target.containsSegments(
            segments,
            allowPartial = mode.allowPartial,
            ignoreCase = mode.ignoreCase
        )

        is Mode.Equal -> target.matches(segments, ignoreCase = mode.ignoreCase)
        is Mode.Specific -> target.segmentContains(
            segment = segments.single(),
            index = mode.index,
            backwards = mode.backwards,
            allowPartial = mode.allowPartial,
            ignoreCase = mode.ignoreCase
        )
    }

    @Serializable
    sealed interface Mode : SieveCriterium.Mode {

        @Serializable @SerialName("ANCESTOR")
        @Parcelize
        data class Ancestor(
            @SerialName("ignoreCase") val ignoreCase: Boolean = true,
        ) : Mode

        @Serializable @SerialName("START")
        @Parcelize
        data class Start(
            @SerialName("ignoreCase") val ignoreCase: Boolean = true,
            @SerialName("allowPartial") val allowPartial: Boolean = false,
        ) : Mode

        @Serializable @SerialName("CONTAIN")
        @Parcelize
        data class Contain(
            @SerialName("ignoreCase") val ignoreCase: Boolean = true,
            @SerialName("allowPartial") val allowPartial: Boolean = false,
        ) : Mode

        @Serializable @SerialName("END")
        @Parcelize
        data class End(
            @SerialName("ignoreCase") val ignoreCase: Boolean = true,
            @SerialName("allowPartial") val allowPartial: Boolean = false,
        ) : Mode

        @Serializable @SerialName("MATCH")
        @Parcelize
        data class Equal(
            @SerialName("ignoreCase") val ignoreCase: Boolean = true,
        ) : Mode

        @Serializable @SerialName("SPECIFIC")
        @Parcelize
        data class Specific(
            @SerialName("index") val index: Int,
            @SerialName("backwards") val backwards: Boolean = false,
            @SerialName("ignoreCase") val ignoreCase: Boolean = true,
            @SerialName("allowPartial") val allowPartial: Boolean = false,
        ) : Mode
    }

}