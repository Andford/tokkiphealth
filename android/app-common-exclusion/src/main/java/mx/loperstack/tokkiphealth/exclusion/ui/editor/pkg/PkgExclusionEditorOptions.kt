package mx.loperstack.tokkiphealth.exclusion.ui.editor.pkg

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class PkgExclusionEditorOptions(
    val targetPkgId: Pkg.Id,
) : Parcelable