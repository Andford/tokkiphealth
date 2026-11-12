package mx.loperstack.tokkiphealth.squeezer.core.tasks

import mx.loperstack.tokkiphealth.main.core.SDMTool

sealed interface SqueezerTask : SDMTool.Task {
    override val type: SDMTool.Type get() = SDMTool.Type.SQUEEZER

    sealed interface Result : SDMTool.Task.Result {
        override val type: SDMTool.Type get() = SDMTool.Type.SQUEEZER
    }
}
