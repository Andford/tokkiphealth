package mx.loperstack.tokkiphealth.corpsefinder.core.tasks

import mx.loperstack.tokkiphealth.corpsefinder.R
import mx.loperstack.tokkiphealth.common.ByteFormatter
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.getQuantityString2
import mx.loperstack.tokkiphealth.corpsefinder.core.CorpseIdentifier
import mx.loperstack.tokkiphealth.stats.core.ReportDetails
import mx.loperstack.tokkiphealth.stats.core.Reportable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CorpseFinderDeleteTask(
    val targetCorpses: Set<CorpseIdentifier>? = null,
    val targetContent: Set<APath>? = null,
) : CorpseFinderTask, Reportable {

    init {
        // Without a targetCorpses scope, `targetContent` would be attempted against every
        // corpse in turn — historically that smeared the same paths across unrelated corpses.
        // Today the only caller (CorpseDetailsViewModel) always pairs targetContent with a
        // single corpse. The contract here is the minimum needed to prevent the smear: a
        // non-null scope of one or more corpses, leaving room for future callers that want
        // multi-corpse partial deletes. The per-corpse content intersection in CorpseFinder
        // makes that case safe.
        require(targetContent == null || targetCorpses != null) {
            "targetContent requires a non-null targetCorpses scope"
        }
    }

    sealed interface Result : CorpseFinderTask.Result

    @Parcelize
    data class Success(
        override val affectedSpace: Long,
        override val affectedPaths: Set<APath>
    ) : Result, ReportDetails.AffectedSpace, ReportDetails.AffectedPaths {

        override val primaryInfo
            get() = caString {
                getQuantityString2(R.plurals.corpsefinder_result_x_corpses_deleted, affectedCount)
            }

        override val secondaryInfo
            get() = caString {
                val (formatted, quantity) = ByteFormatter.formatSize(this, affectedSpace)
                getQuantityString2(
                    mx.loperstack.tokkiphealth.common.R.plurals.general_result_x_space_freed,
                    quantity,
                    formatted,
                )
            }
    }
}