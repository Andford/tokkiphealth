package mx.loperstack.tokkiphealth.common.adb

class AdbUnavailableException @JvmOverloads constructor(
    message: String? = null,
    cause: Throwable? = null,
) : AdbException(message = message, cause = cause)