package mx.loperstack.tokkiphealth.common.ipc

import android.os.DeadObjectException
import mx.loperstack.tokkiphealth.common.R
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError
import mx.loperstack.tokkiphealth.common.error.getStackTracePeek

class ServiceConnectionLostException(
    override val cause: DeadObjectException
) : Exception(), HasLocalizedError {

    override fun getLocalizedError(): LocalizedError = LocalizedError(
        throwable = this,
        label = R.string.general_error_ipc_deadobject_title.toCaString(),
        description = caString {
            var message = it.getString(R.string.general_error_ipc_deadobject_description)
            message += "\n\n"
            message += cause.getStackTracePeek()
            message
        }
    )

}