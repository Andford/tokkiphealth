package mx.loperstack.tokkiphealth.main.core

import android.os.Parcelable
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.progress.Progress
import mx.loperstack.tokkiphealth.common.sharedresource.HasSharedResource
import kotlinx.coroutines.flow.Flow

interface SDMTool : Progress.Host, Progress.Client, HasSharedResource<Any> {

    val type: Type

    val state: Flow<State>

    interface State

    suspend fun submit(task: Task): Task.Result

    interface Task : Parcelable {
        val type: Type

        interface Result : Parcelable {
            val type: Type

            val primaryInfo: CaString
            val secondaryInfo: CaString? get() = null
        }
    }

    // If you rename any of these, check mapping for ROOM DB
    enum class Type {
        CORPSEFINDER,
        SYSTEMCLEANER,
        APPCLEANER,
        APPCONTROL,
        ANALYZER,
        DEDUPLICATOR,
SQUEEZER,
        SWIPER,
    }
}