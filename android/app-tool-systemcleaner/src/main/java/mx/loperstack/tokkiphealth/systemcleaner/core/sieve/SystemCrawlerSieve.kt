package mx.loperstack.tokkiphealth.systemcleaner.core.sieve

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.FileType
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.FileForensics
import mx.loperstack.tokkiphealth.common.forensics.identifyArea
import mx.loperstack.tokkiphealth.common.sieve.FileSieve
import mx.loperstack.tokkiphealth.common.sieve.NameCriterium
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium
import mx.loperstack.tokkiphealth.common.sieve.TypeCriterium
import java.time.Duration

class SystemCrawlerSieve @AssistedInject constructor(
    @Assisted val config: Config,
    private val fileForensics: FileForensics,
) : FileSieve {

    data class Result(
        val item: APathLookup<*>,
        val matches: Boolean,
        val areaInfo: AreaInfo? = null,
    )

    suspend fun match(subject: APathLookup<*>): Result {
        val nope = { Result(subject, matches = false) }

        // Directory or file? Symlinks and unknown entries are treated as FILE, mirroring how
        // CustomFilter maps the configured fileTypes. A symlink is its own node (we only ever
        // delete the link, never its target), so it must not slip past a directory-only filter.
        config.targetTypes?.takeIf { it.isNotEmpty() }?.let { types ->
            val subjectType = when (subject.fileType) {
                FileType.DIRECTORY -> TypeCriterium.DIRECTORY
                FileType.FILE, FileType.SYMBOLIC_LINK, FileType.UNKNOWN -> TypeCriterium.FILE
            }
            if (!types.contains(subjectType)) return nope()
        }

        config.maximumSize?.let {
            // Is our subject too large?
            if (subject.size > it) return nope()
        }

        config.minimumSize?.let {
            // Maybe it's too small
            if (subject.size < it) return nope()
        }

        if (config.maximumAge != null || config.minimumAge != null) {
            val nowMillis = System.currentTimeMillis()
            config.maximumAge?.let {
                if (nowMillis - subject.modifiedAt.toEpochMilli() > it.toMillis()) {
                    return nope()
                }
            }

            config.minimumAge?.let {
                if (nowMillis - subject.modifiedAt.toEpochMilli() < it.toMillis()) {
                    return nope()
                }
            }
        }

        config.pathCriteria?.takeIf { it.isNotEmpty() }?.let { criteria ->
            if (!criteria.matchAny(subject.segments)) return nope()
        }

        config.nameCriteria?.takeIf { it.isNotEmpty() }?.let { criteria ->
            if (!criteria.matchAny(subject.name)) return nope()
        }

        config.pathExclusions?.takeIf { it.isNotEmpty() }?.let { exclusions ->
            // Check what the path should not contain
            if (exclusions.matchAny(subject.segments)) return nope()
        }

        config.pathRegexes?.takeIf { it.isNotEmpty() }?.let { regexes ->
            if (regexes.none { it.matches(subject.path) }) return nope()
        }

        var areaInfo: AreaInfo? = null
        var pfpSegments: List<String>? = null

        suspend fun areaLoader(): Boolean {
            if (areaInfo != null) return true
            areaInfo = fileForensics.identifyArea(subject)
            if (areaInfo == null) {
                log(TAG, WARN) { "Couldn't identify area for $subject" }
                return false
            }
            pfpSegments = areaInfo!!.prefixFreeSegments
            return true
        }

        config.areaTypes?.takeIf { it.isNotEmpty() }?.let { types ->
            val loaded = areaLoader()
            val typeMatches = areaInfo?.type in types
            if (!loaded || !typeMatches) return nope()
        }

        // Now working on the prefix-free-path!
        // Check pfp startsWith/ancestorOf

        config.pfpExclusions?.takeIf { it.isNotEmpty() }?.let { criteria ->
            if (!areaLoader()) return nope()
            if (criteria.matchAny(pfpSegments!!)) return nope()
        }

        config.pfpCriteria?.takeIf { it.isNotEmpty() }?.let { criteria ->
            if (!areaLoader()) return nope()
            if (!criteria.matchAny(pfpSegments!!)) return nope()
        }

        return Result(
            subject,
            matches = true,
            areaInfo = areaInfo
        )
    }

    data class Config(
        val areaTypes: Set<DataArea.Type>? = null,
        val targetTypes: Set<TypeCriterium>? = null,
        val nameCriteria: Set<NameCriterium>? = null,
        val pathCriteria: Set<SegmentCriterium>? = null,
        val pathExclusions: Set<SegmentCriterium>? = null,
        val pathRegexes: Set<Regex>? = null,
        val pfpCriteria: Set<SegmentCriterium>? = null,
        val pfpExclusions: Set<SegmentCriterium>? = null,
        val maximumSize: Long? = null,
        val minimumSize: Long? = null,
        val maximumAge: Duration? = null,
        val minimumAge: Duration? = null,
    )

    @AssistedFactory
    interface Factory {
        fun create(config: Config): SystemCrawlerSieve
    }

    companion object {
        private val TAG = logTag("SystemCleaner", "SystemCrawler", "BaseSieve")
    }

}