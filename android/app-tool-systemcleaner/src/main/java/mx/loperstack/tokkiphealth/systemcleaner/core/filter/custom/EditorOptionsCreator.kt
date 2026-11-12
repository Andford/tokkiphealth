package mx.loperstack.tokkiphealth.systemcleaner.core.filter.custom

import android.content.Context
import android.os.Parcelable
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.FileType
import mx.loperstack.tokkiphealth.common.files.FilterEditorOptionsCreator
import mx.loperstack.tokkiphealth.common.files.segs
import mx.loperstack.tokkiphealth.common.forensics.FileForensics
import mx.loperstack.tokkiphealth.common.forensics.identifyArea
import mx.loperstack.tokkiphealth.common.getQuantityString2
import mx.loperstack.tokkiphealth.common.sieve.SegmentCriterium
import mx.loperstack.tokkiphealth.common.filter.CustomFilterEditorOptions
import javax.inject.Inject

@Reusable
class EditorOptionsCreator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileForensics: FileForensics,
) : FilterEditorOptionsCreator {

    override suspend fun createOptions(targets: Set<APathLookup<*>>): Parcelable {
        log(TAG, VERBOSE) { "Determining options for ${targets.size} targets:\n${targets.joinToString("\n")}" }

        val areaInfos = targets.mapNotNull { target ->
            fileForensics.identifyArea(target).also {
                if (it == null) log(TAG, WARN) { "Failed to determine AreaInfo for $target" }
            }
        }

        val label = areaInfos
            .map { it.prefix }
            .toSet().singleOrNull()
            ?.let {
                val pathPrefix = it.userReadablePath.get(context)
                val itemCount = context.getQuantityString2(
                    mx.loperstack.tokkiphealth.common.R.plurals.result_x_items,
                    targets.size
                )
                "$pathPrefix - $itemCount"
            }

        val targetAreas = areaInfos.map { it.dataArea.type }.toSet()

        val pathCriteria = targets.map {
            when (it.fileType) {
                FileType.DIRECTORY -> SegmentCriterium(
                    it.segments + segs(""),
                    mode = SegmentCriterium.Mode.Start(allowPartial = true),
                )

                FileType.SYMBOLIC_LINK,
                FileType.FILE,
                FileType.UNKNOWN -> SegmentCriterium(
                    it.segments,
                    mode = SegmentCriterium.Mode.Equal(),
                )
            }
        }.toSet()

        return CustomFilterEditorOptions(
            label = label,
            areas = targetAreas,
            pathCriteria = pathCriteria,
            saveAsEnabled = true,
        ).also { log(TAG, INFO) { "Editor options are : $it" } }
    }

    @InstallIn(SingletonComponent::class)
    @Module
    abstract class DIM {
        @Binds abstract fun bindFilterEditorOptionsCreator(impl: EditorOptionsCreator): FilterEditorOptionsCreator
    }

    companion object {
        private val TAG = logTag("SystemCleaner", "CustomFilter", "EditorOptions", "Creator")
    }
}