package mx.loperstack.tokkiphealth.exclusion.ui.editor.segment

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.files.Segments
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class SegmentExclusionEditorOptions(
    val targetSegments: Segments? = null,
) : Parcelable