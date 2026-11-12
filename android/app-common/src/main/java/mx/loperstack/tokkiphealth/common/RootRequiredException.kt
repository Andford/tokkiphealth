package mx.loperstack.tokkiphealth.common

import androidx.annotation.StringRes
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

class RootRequiredException(
    message: String,
    cause: Throwable? = null,
    @StringRes val errorMsgRes: Int = R.string.general_error_root_unavailable
) : IllegalStateException(message, cause), HasLocalizedError {

    override fun getLocalizedError() = LocalizedError(
        throwable = this,
        label = "RootRequiredException".toCaString(),
        description = errorMsgRes.toCaString()
    )
}