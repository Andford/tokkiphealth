package mx.loperstack.tokkiphealth.common.ipc

import java.io.IOException

class UnwrappedIPCException(
    message: String
) : IOException(message)