package mx.loperstack.tokkiphealth.common.files

import android.os.Parcelable
import androidx.annotation.Keep
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
interface APath : Parcelable {
    val path: String
    val name: String
    val pathType: PathType

    val userReadablePath: CaString
        get() = path.toCaString()
    val userReadableName: CaString
        get() = name.toCaString()

    val segments: Segments

    fun child(vararg segments: String): APath

    @Keep
    @Serializable
    enum class PathType {
        @SerialName("RAW") RAW,
        @SerialName("LOCAL") LOCAL,
        @SerialName("SAF") SAF,
    }

}