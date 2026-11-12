package mx.loperstack.tokkiphealth.appcontrol.core

import mx.loperstack.tokkiphealth.main.core.SDMTool

interface AppControlTask : SDMTool.Task {
    override val type: SDMTool.Type get() = SDMTool.Type.APPCONTROL

    interface Result : SDMTool.Task.Result {
        override val type: SDMTool.Type get() = SDMTool.Type.APPCONTROL
    }
}

