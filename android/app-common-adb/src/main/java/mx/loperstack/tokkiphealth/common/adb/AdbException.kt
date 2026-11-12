package mx.loperstack.tokkiphealth.common.adb

import java.io.IOException

open class AdbException @JvmOverloads constructor(
    message: String? = null,
    cause: Throwable? = null,
) : IOException(message, cause)