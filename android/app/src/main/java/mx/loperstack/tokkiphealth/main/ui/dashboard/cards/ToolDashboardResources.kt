package mx.loperstack.tokkiphealth.main.ui.dashboard.cards

import mx.loperstack.tokkiphealth.appcleaner.R as AppCleanerR
import mx.loperstack.tokkiphealth.common.R as CommonR
import mx.loperstack.tokkiphealth.corpsefinder.R as CorpseFinderR
import mx.loperstack.tokkiphealth.deduplicator.R as DeduplicatorR
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.systemcleaner.R as SystemCleanerR

internal fun toolNameRes(type: SDMTool.Type): Int = when (type) {
    SDMTool.Type.CORPSEFINDER -> CommonR.string.corpsefinder_tool_name
    SDMTool.Type.SYSTEMCLEANER -> CommonR.string.systemcleaner_tool_name
    SDMTool.Type.APPCLEANER -> CommonR.string.appcleaner_tool_name
    SDMTool.Type.DEDUPLICATOR -> CommonR.string.deduplicator_tool_name
    else -> error("Unsupported tool type: $type")
}

internal fun toolDescriptionRes(type: SDMTool.Type): Int = when (type) {
    SDMTool.Type.CORPSEFINDER -> CorpseFinderR.string.corpsefinder_explanation_short
    SDMTool.Type.SYSTEMCLEANER -> SystemCleanerR.string.systemcleaner_explanation_short
    SDMTool.Type.APPCLEANER -> AppCleanerR.string.appcleaner_explanation_short
    SDMTool.Type.DEDUPLICATOR -> DeduplicatorR.string.deduplicator_explanation_short
    else -> error("Unsupported tool type: $type")
}
