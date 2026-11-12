package mx.loperstack.tokkiphealth.common.user

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.R
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.caString
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserProfile2(
    val handle: UserHandle2,
    val label: String? = null,
    val code: String? = null,
    val isRunning: Boolean = true,
) : Parcelable {
    fun getHumanLabel(): CaString = caString {
        when {
            label != null -> label
            handle.handleId == 0 -> getString(R.string.general_user_label_owner)
            handle.handleId == -1 -> getString(R.string.general_user_label_system)
            else -> "User-${handle.handleId}"
        }
    }
}