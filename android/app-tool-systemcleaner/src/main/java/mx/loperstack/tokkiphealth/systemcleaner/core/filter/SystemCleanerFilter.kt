package mx.loperstack.tokkiphealth.systemcleaner.core.filter

import androidx.compose.ui.graphics.vector.ImageVector
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.progress.Progress
import kotlin.reflect.KClass

interface SystemCleanerFilter : Progress.Host, Progress.Client {

    val identifier: FilterIdentifier
        get() = this::class.qualifiedName!!

    val icon: ImageVector

    suspend fun getLabel(): CaString

    suspend fun getDescription(): CaString

    suspend fun targetAreas(): Set<DataArea.Type>

    suspend fun initialize()

    suspend fun match(item: APathLookup<*>): Match?

    suspend fun process(matches: Collection<Match>): Collection<Processed>

    data class Processed(
        val match: Match,
        val error: Throwable?,
    ) {
        val success: Boolean
            get() = error == null
    }

    interface Match {
        val expectedGain: Long
        val lookup: APathLookup<*>

        val path: APath
            get() = lookup.lookedUp

        data class Deletion(
            override val lookup: APathLookup<*>,
        ) : Match {
            override val expectedGain: Long
                get() = lookup.size
        }
    }

    interface Factory {
        suspend fun isEnabled(): Boolean
        suspend fun create(): SystemCleanerFilter
    }
}

val <T : SystemCleanerFilter> KClass<T>.filterIdentifier: FilterIdentifier
    get() = this.qualifiedName!!
