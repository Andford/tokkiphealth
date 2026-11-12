package mx.loperstack.tokkiphealth.deduplicator.core.arbiter.checks

import dagger.Reusable
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import mx.loperstack.tokkiphealth.deduplicator.core.arbiter.ArbiterCheck
import mx.loperstack.tokkiphealth.deduplicator.core.arbiter.ArbiterCriterium
import javax.inject.Inject

@Reusable
class SizeCheck @Inject constructor() : ArbiterCheck {
    suspend fun favorite(
        before: List<Duplicate>,
        criterium: ArbiterCriterium.Size
    ): List<Duplicate> = when (criterium.mode) {
        ArbiterCriterium.Size.Mode.PREFER_SMALLER -> before.sortedBy { it.size }
        ArbiterCriterium.Size.Mode.PREFER_LARGER -> before.sortedByDescending { it.size }
    }
}