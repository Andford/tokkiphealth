@file:UseSerializers(APathSerializer::class)

package mx.loperstack.tokkiphealth.common.previews

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.serialization.APathSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
@Parcelize
data class PreviewOptions(
    val paths: List<APath>,
    val position: Int = 0,
) : Parcelable {

    constructor(path: APath) : this(paths = listOf(path))

}
