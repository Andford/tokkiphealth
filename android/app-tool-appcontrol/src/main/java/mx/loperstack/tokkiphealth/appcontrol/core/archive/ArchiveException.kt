package mx.loperstack.tokkiphealth.appcontrol.core.archive

import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import okio.IOException

open class ArchiveException @JvmOverloads constructor(
    message: String? = null,
    installId: InstallId? = null,
    cause: Throwable? = null,
) : IOException(message ?: "Failed to archive $installId", cause)
