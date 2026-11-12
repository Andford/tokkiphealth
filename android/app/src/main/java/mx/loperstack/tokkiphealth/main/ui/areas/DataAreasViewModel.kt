package mx.loperstack.tokkiphealth.main.ui.areas

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.WebpageTool
import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.areas.DataAreaManager
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class DataAreasViewModel @Inject constructor(
    @Suppress("unused") private val handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val dataAreaManager: DataAreaManager,
    private val taskManager: TaskManager,
    private val webpageTool: WebpageTool,
) : ViewModel4(dispatcherProvider = dispatcherProvider, tag = TAG) {

    private val isReloading = MutableStateFlow(false)

    val state: StateFlow<State> = combine(
        dataAreaManager.results,
        taskManager.state,
        isReloading,
    ) { areaResult, taskState, reloading ->
        State(
            areas = areaResult.getOrNull()?.areas,
            areasUnavailable = areaResult.isFailure,
            allowReload = taskState.isIdle && !reloading,
            isReloading = reloading,
        )
    }.safeStateIn(
        initialValue = State(),
        onError = { State() },
    )

    data class State(
        val areas: Set<DataArea>? = null,
        val areasUnavailable: Boolean = false,
        val allowReload: Boolean = false,
        val isReloading: Boolean = false,
    )

    fun reloadDataAreas() = launch {
        if (!isReloading.compareAndSet(expect = false, update = true)) return@launch

        log(TAG) { "reloadDataAreas()" }
        try {
            dataAreaManager.reloadAndAwait()
        } finally {
            isReloading.value = false
        }
    }

    fun openDocumentation() {
        // TODO: replace with a direct link to the data-areas documentation page.
        webpageTool.open("https://sdmse.darken.eu/")
    }

    companion object {
        private val TAG = logTag("DataAreas", "ViewModel")
    }
}
