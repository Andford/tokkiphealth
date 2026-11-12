package eu.darken.flowshell.core

import mx.loperstack.tokkiphealth.common.debug.Bugs

object FlowShellDebug {
    var isDebug: Boolean = Bugs.isTrace
    internal var tag = "SDMSE:FS"
}