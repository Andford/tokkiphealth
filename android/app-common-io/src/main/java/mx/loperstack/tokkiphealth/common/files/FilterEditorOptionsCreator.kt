package mx.loperstack.tokkiphealth.common.files

import android.os.Parcelable

fun interface FilterEditorOptionsCreator {
    suspend fun createOptions(targets: Set<APathLookup<*>>): Parcelable
}
