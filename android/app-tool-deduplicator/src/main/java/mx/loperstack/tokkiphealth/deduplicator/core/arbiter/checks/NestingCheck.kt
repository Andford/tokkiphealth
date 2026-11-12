package mx.loperstack.tokkiphealth.deduplicator.core.arbiter.checks

import dagger.Reusable
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import mx.loperstack.tokkiphealth.deduplicator.core.arbiter.ArbiterCheck
import mx.loperstack.tokkiphealth.deduplicator.core.arbiter.ArbiterCriterium
import javax.inject.Inject

@Reusable
class NestingCheck @Inject constructor() : ArbiterCheck {
    suspend fun favorite(
        before: List<Duplicate>,
        criterium: ArbiterCriterium.Nesting
    ): List<Duplicate> = when (criterium.mode) {
        ArbiterCriterium.Nesting.Mode.PREFER_SHALLOW -> before.sortedBy { it.path.segments.size }
        ArbiterCriterium.Nesting.Mode.PREFER_DEEPER -> before.sortedByDescending { it.path.segments.size }
    }
}