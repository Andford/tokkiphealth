package mx.loperstack.tokkiphealth.appcleaner.core

import mx.loperstack.tokkiphealth.appcleaner.core.forensics.ExpendablesFilter
import mx.loperstack.tokkiphealth.exclusion.core.types.Exclusion
import mx.loperstack.tokkiphealth.exclusion.core.types.PathExclusionIndex
import mx.loperstack.tokkiphealth.exclusion.core.types.excludeNestedLookups

suspend fun Collection<Exclusion.Path>.excludeNestedLookups(
    matches: Collection<ExpendablesFilter.Match>
): Set<ExpendablesFilter.Match> = PathExclusionIndex(this).excludeNestedLookups(matches)

suspend fun PathExclusionIndex.excludeNestedLookups(
    matches: Collection<ExpendablesFilter.Match>
): Set<ExpendablesFilter.Match> {
    val survivors = this.excludeNestedLookups(matches.map { it.lookup })
    return matches
        .filter { survivors.contains(it.lookup) }
        .toSet()
}
