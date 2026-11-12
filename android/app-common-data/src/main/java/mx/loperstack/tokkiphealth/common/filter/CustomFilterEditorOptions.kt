package mx.loperstack.tokkiphealth.common.filter

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.sieve.NameCriterium
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class CustomFilterEditorOptions(
    val areas: Set<DataArea.Type>? = null,
    val label: String? = null,
    val pathCriteria: Set<SegmentCriterium>? = null,
    val nameCriteria: Set<NameCriterium>? = null,
    val saveAsEnabled: Boolean = false,
) : Parcelable