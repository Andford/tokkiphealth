package mx.loperstack.tokkiphealth.stats.ui.pkgs

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.get
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.stats.core.AffectedPkg
import mx.loperstack.tokkiphealth.stats.core.Report
import mx.loperstack.tokkiphealth.stats.core.StatsRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AffectedPkgsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val statsRepo: StatsRepo,
    private val pkgRepo: PkgRepo,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    private val reportIdFlow = MutableStateFlow<UUID?>(null)

    fun setReportId(id: UUID) {
        if (reportIdFlow.value == id) return
        log(TAG) { "setReportId($id)" }
        reportIdFlow.value = id
    }

    val state: StateFlow<State> = reportIdFlow
        .filterNotNull()
        .flatMapLatest { id ->
            flow {
                val report = statsRepo.getById(id)
                if (report == null) {
                    log(TAG, WARN) { "Report $id not found" }
                    emit(State.NotFound)
                    return@flow
                }
                val affected = statsRepo.getAffectedPkgs(id)
                val rows = affected
                    .sortedBy { it.pkgId.name }
                    .map { pkg -> Row(pkg, pkgRepo.get(pkg.pkgId).firstOrNull()) }
                emit(State.Ready(report, rows))
            }
        }
        .safeStateIn(
            initialValue = State.Loading,
            onError = { State.NotFound },
        )

    sealed interface State {
        data object Loading : State
        data object NotFound : State
        data class Ready(val report: Report, val rows: List<Row>) : State
    }

    data class Row(
        val affectedPkg: AffectedPkg,
        val installedPkg: Pkg?,
    )

    companion object {
        private val TAG = logTag("Stats", "Affected", "Pkgs", "ViewModel")
    }
}
