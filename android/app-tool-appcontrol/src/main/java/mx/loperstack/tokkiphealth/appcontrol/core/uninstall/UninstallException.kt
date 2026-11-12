package mx.loperstack.tokkiphealth.appcontrol.core.uninstall

import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import okio.IOException

open class UninstallException @JvmOverloads constructor(
    message: String? = null,
    installId: InstallId? = null,
    cause: Throwable? = null,
) : IOException(message ?: "Failed to uninstall $installId", cause)