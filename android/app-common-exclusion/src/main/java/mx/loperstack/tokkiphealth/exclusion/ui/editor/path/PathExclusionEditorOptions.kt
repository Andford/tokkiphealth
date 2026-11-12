@file:UseSerializers(APathSerializer::class)

package mx.loperstack.tokkiphealth.exclusion.ui.editor.path

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.serialization.APathSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
@Parcelize
data class PathExclusionEditorOptions(
    val targetPath: APath,
) : Parcelable