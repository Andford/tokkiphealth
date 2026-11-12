package mx.loperstack.tokkiphealth.deduplicator.core.scanner

import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import kotlinx.coroutines.flow.Flow

interface Sleuth : Progress.Host, Progress.Client {

    suspend fun investigate(searchFlow: Flow<APathLookup<*>>): Set<Duplicate.Group>

    interface Factory {
        suspend fun create(): Sleuth
    }
}