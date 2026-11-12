package mx.loperstack.tokkiphealth.common.picker

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.files.APath
import kotlinx.parcelize.Parcelize

@Parcelize
data class PickerResult(
    val selectedPaths: Set<APath>,
) : Parcelable
