package mx.loperstack.tokkiphealth.common.files.saf

import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.ReadException

class MissingUriPermissionException(
    message: String? = "No matching UriPermission",
    path: APath? = null,
    cause: Throwable? = null,
) : ReadException(message = message, cause = cause, path = path)