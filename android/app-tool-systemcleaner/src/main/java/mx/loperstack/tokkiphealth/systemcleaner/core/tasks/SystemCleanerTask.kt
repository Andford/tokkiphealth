package mx.loperstack.tokkiphealth.systemcleaner.core.tasks

import mx.loperstack.tokkiphealth.main.core.SDMTool

sealed interface SystemCleanerTask : SDMTool.Task {
    override val type: SDMTool.Type get() = SDMTool.Type.SYSTEMCLEANER


    sealed interface Result : SDMTool.Task.Result {
        override val type: SDMTool.Type get() = SDMTool.Type.SYSTEMCLEANER
    }
}

