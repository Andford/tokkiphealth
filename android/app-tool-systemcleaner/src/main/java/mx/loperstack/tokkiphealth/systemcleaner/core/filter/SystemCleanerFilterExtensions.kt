package mx.loperstack.tokkiphealth.systemcleaner.core.filter

import mx.loperstack.tokkiphealth.exclusion.core.types.Exclusion
import mx.loperstack.tokkiphealth.exclusion.core.types.PathExclusionIndex
import mx.loperstack.tokkiphealth.exclusion.core.types.excludeNestedLookups
import mx.loperstack.tokkiphealth.systemcleaner.core.sieve.SystemCrawlerSieve

suspend fun SystemCrawlerSieve.Result.toDeletion(): SystemCleanerFilter.Match.Deletion? {
    return if (matches) SystemCleanerFilter.Match.Deletion(item) else null
}

suspend fun Collection<Exclusion.Path>.excludeNestedLookups(
    matches: Collection<SystemCleanerFilter.Match>
): Set<SystemCleanerFilter.Match> = PathExclusionIndex(this).excludeNestedLookups(matches)

suspend fun PathExclusionIndex.excludeNestedLookups(
    matches: Collection<SystemCleanerFilter.Match>
): Set<SystemCleanerFilter.Match> {
    val survivors = this.excludeNestedLookups(matches.map { it.lookup })
    return matches
        .filter { survivors.contains(it.lookup) }
        .toSet()
}
