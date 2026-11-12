package mx.loperstack.tokkiphealth.appcontrol.core.restore

import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import okio.IOException

open class RestoreException @JvmOverloads constructor(
    message: String? = null,
    installId: InstallId? = null,
    cause: Throwable? = null,
) : IOException(message ?: "Failed to restore $installId", cause)
