package mx.loperstack.tokkiphealth.deduplicator.core.arbiter.checks

import dagger.Reusable
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import mx.loperstack.tokkiphealth.deduplicator.core.arbiter.ArbiterCheck
import mx.loperstack.tokkiphealth.deduplicator.core.arbiter.ArbiterCriterium
import javax.inject.Inject

@Reusable
class ModificationCheck @Inject constructor() : ArbiterCheck {
    suspend fun favorite(
        before: List<Duplicate>,
        criterium: ArbiterCriterium.Modified
    ): List<Duplicate> = when (criterium.mode) {
        ArbiterCriterium.Modified.Mode.PREFER_OLDER -> before.sortedBy { it.modifiedAt }
        ArbiterCriterium.Modified.Mode.PREFER_NEWER -> before.sortedByDescending { it.modifiedAt }
    }
}