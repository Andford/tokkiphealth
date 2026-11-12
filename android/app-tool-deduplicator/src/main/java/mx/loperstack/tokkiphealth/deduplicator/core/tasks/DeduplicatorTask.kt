package mx.loperstack.tokkiphealth.deduplicator.core.tasks

import mx.loperstack.tokkiphealth.main.core.SDMTool

sealed interface DeduplicatorTask : SDMTool.Task {
    override val type: SDMTool.Type get() = SDMTool.Type.DEDUPLICATOR

    sealed interface Result : SDMTool.Task.Result {
        override val type: SDMTool.Type get() = SDMTool.Type.DEDUPLICATOR
    }
}

