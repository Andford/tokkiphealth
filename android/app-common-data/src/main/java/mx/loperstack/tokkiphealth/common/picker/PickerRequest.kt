@file:UseSerializers(APathSerializer::class)

package mx.loperstack.tokkiphealth.common.picker

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.serialization.APathSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
@Parcelize
data class PickerRequest(
    val requestKey: String,
    val mode: PickMode,
    val allowedAreas: Set<DataArea.Type> = emptySet(),
    val selectedPaths: List<APath> = emptyList(),
) : Parcelable {
    @Serializable
    enum class PickMode {
        @SerialName("DIR") DIR,
        @SerialName("DIRS") DIRS,

        /** Multi-select like [DIRS], but files are also listed and selectable (folders + files). */
        @SerialName("FILES_AND_DIRS") FILES_AND_DIRS,
    }
}
