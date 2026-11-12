package mx.loperstack.tokkiphealth.analyzer.core

import mx.loperstack.tokkiphealth.main.core.SDMTool

interface AnalyzerTask : SDMTool.Task {
    override val type: SDMTool.Type get() = SDMTool.Type.ANALYZER

    interface Result : SDMTool.Task.Result {
        override val type: SDMTool.Type get() = SDMTool.Type.ANALYZER
    }
}

