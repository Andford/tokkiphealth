package mx.loperstack.tokkiphealth.appcleaner.core.forensics

import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.Segments
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.progress.Progress
import kotlin.reflect.KClass

interface ExpendablesFilter : Progress.Host, Progress.Client {

    val identifier: ExpendablesFilterIdentifier
        get() = this.javaClass.kotlin

    suspend fun initialize()

    suspend fun match(
        pkgId: Pkg.Id,
        target: APathLookup<APath>,
        areaType: DataArea.Type,
        pfpSegs: Segments
    ): Match?

    suspend fun process(targets: Collection<Match>, allMatches: Collection<Match>): ProcessResult

    data class ProcessResult(
        val success: Collection<Match>,
        val failed: Collection<Pair<Match, Exception>>,
    )

    interface Match {
        val identifier: ExpendablesFilterIdentifier
        val lookup: APathLookup<*>
        val path: APath
            get() = lookup.lookedUp

        val expectedGain: Long

        data class Deletion(
            override val identifier: ExpendablesFilterIdentifier,
            override val lookup: APathLookup<*>,
        ) : Match {
            override val expectedGain: Long
                get() = lookup.size
        }
    }

    interface Factory {
        suspend fun isEnabled(): Boolean
        suspend fun create(): ExpendablesFilter
    }
}

typealias ExpendablesFilterIdentifier = KClass<out ExpendablesFilter>