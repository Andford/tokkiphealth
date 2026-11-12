package mx.loperstack.tokkiphealth.common.root

import androidx.annotation.StringRes
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

class RootUnavailableException @JvmOverloads constructor(
    message: String? = null,
    cause: Throwable? = null,
    @StringRes val errorMsgRes: Int = mx.loperstack.tokkiphealth.common.R.string.general_error_root_unavailable
) : RootException(message = message, cause = cause), HasLocalizedError {

    override fun getLocalizedError() = LocalizedError(
        throwable = this,
        label = "RootUnavailableException".toCaString(),
        description = errorMsgRes.toCaString()
    )
}