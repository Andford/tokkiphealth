package mx.loperstack.tokkiphealth.corpsefinder.core.tasks

import mx.loperstack.tokkiphealth.main.core.SDMTool

sealed interface CorpseFinderTask : SDMTool.Task {
    override val type: SDMTool.Type get() = SDMTool.Type.CORPSEFINDER

    sealed interface Result : SDMTool.Task.Result {
        override val type: SDMTool.Type get() = SDMTool.Type.CORPSEFINDER
    }
}

