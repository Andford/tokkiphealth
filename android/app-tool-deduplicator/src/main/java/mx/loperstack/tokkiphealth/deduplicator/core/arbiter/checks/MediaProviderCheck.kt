package mx.loperstack.tokkiphealth.deduplicator.core.arbiter.checks

import dagger.Reusable
import mx.loperstack.tokkiphealth.common.files.MediaStoreTool
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import mx.loperstack.tokkiphealth.deduplicator.core.arbiter.ArbiterCheck
import mx.loperstack.tokkiphealth.deduplicator.core.arbiter.ArbiterCriterium
import javax.inject.Inject

@Reusable
class MediaProviderCheck @Inject constructor(
    private val mediaStoreTool: MediaStoreTool,
) : ArbiterCheck {

    suspend fun favorite(
        before: List<Duplicate>,
        criterium: ArbiterCriterium.MediaProvider,
    ): List<Duplicate> {
        val withIndexInfo = before.map { it to mediaStoreTool.isIndexed(it.path) }

        val sorted = when (criterium.mode) {
            ArbiterCriterium.MediaProvider.Mode.PREFER_INDEXED -> withIndexInfo.sortedByDescending {
                it.second
            }

            ArbiterCriterium.MediaProvider.Mode.PREFER_UNKNOWN -> withIndexInfo.sortedBy {
                it.second
            }
        }

        return sorted.map { it.first }
    }
}