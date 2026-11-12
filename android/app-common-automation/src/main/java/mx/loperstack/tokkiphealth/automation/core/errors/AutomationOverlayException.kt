package mx.loperstack.tokkiphealth.automation.core.errors

import android.view.WindowManager
import mx.loperstack.tokkiphealth.automation.R
import mx.loperstack.tokkiphealth.common.ca.toCaString
import mx.loperstack.tokkiphealth.common.error.HasLocalizedError
import mx.loperstack.tokkiphealth.common.error.LocalizedError

open class AutomationOverlayException(
    cause: WindowManager.BadTokenException,
) : AutomationException(
    "Couldn't show overlay. This sometimes happens on certain devices — a reboot usually helps.",
    cause,
), HasLocalizedError {

    override fun getLocalizedError(): LocalizedError = LocalizedError(
        throwable = this,
        label = R.string.automation_error_overlay_title.toCaString(),
        description = R.string.automation_error_overlay_body.toCaString(),
    )

}