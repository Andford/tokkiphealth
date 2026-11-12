package mx.loperstack.tokkiphealth.stats.ui.reports

import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.SingleEventFlow
import mx.loperstack.tokkiphealth.common.flow.intervalFlow
import mx.loperstack.tokkiphealth.common.navigation.routes.UpgradeRoute
import mx.loperstack.tokkiphealth.common.upgrade.UpgradeRepo
import mx.loperstack.tokkiphealth.common.upgrade.isProForUi
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.main.core.SDMTool
import mx.loperstack.tokkiphealth.stats.core.Report
import mx.loperstack.tokkiphealth.stats.core.ReportId
import mx.loperstack.tokkiphealth.stats.core.StatsRepo
import mx.loperstack.tokkiphealth.stats.ui.AffectedFilesRoute
import mx.loperstack.tokkiphealth.stats.ui.AffectedPkgsRoute
import mx.loperstack.tokkiphealth.stats.ui.SpaceHistoryRoute
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class ReportsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    private val statsRepo: StatsRepo,
    private val upgradeRepo: UpgradeRepo,
) : ViewModel4(dispatcherProvider, tag = TAG) {

    val events = SingleEventFlow<Event>()

    private val nowTicks = intervalFlow(1.minutes).map { Instant.now() }

    val state: StateFlow<State> = combine(
        statsRepo.reports,
        nowTicks,
        upgradeRepo.upgradeInfo.map { it.isPro },
    ) { reports, now, isPro ->
        State(
            rows = reports.reversed().map { it.toRow() },
            isPro = isPro,
            now = now,
        )
    }.safeStateIn(
        initialValue = State(),
        onError = { State(rows = emptyList()) },
    )

    fun onReportClick(row: Row) {
        log(TAG) { "onReportClick($row)" }
        when (row.status) {
            Report.Status.SUCCESS, Report.Status.PARTIAL_SUCCESS -> when (row.tool) {
                SDMTool.Type.APPCONTROL -> navTo(AffectedPkgsRoute(reportId = row.reportId.toString()))
                else -> navTo(AffectedFilesRoute(reportId = row.reportId.toString()))
            }

            Report.Status.FAILURE -> {
                row.errorMessage?.let { events.tryEmit(Event.ShowError(it)) }
            }
        }
    }

    fun onStorageTrendClick() = launch {
        log(TAG) { "onStorageTrendClick()" }
        if (upgradeRepo.isProForUi()) {
            navTo(SpaceHistoryRoute())
        } else {
            navTo(UpgradeRoute(forced = true))
        }
    }

    private fun Report.toRow() = Row(
        reportId = reportId,
        tool = tool,
        status = status,
        endAt = endAt,
        primaryMessage = primaryMessage,
        secondaryMessage = secondaryMessage,
        errorMessage = errorMessage,
    )

    data class State(
        val rows: List<Row>? = null,
        val isPro: Boolean = false,
        val now: Instant = Instant.EPOCH,
    )

    data class Row(
        val reportId: ReportId,
        val tool: SDMTool.Type,
        val status: Report.Status,
        val endAt: Instant,
        val primaryMessage: String?,
        val secondaryMessage: String?,
        val errorMessage: String?,
    )

    sealed interface Event {
        data class ShowError(val msg: String) : Event
    }

    companion object {
        private val TAG = logTag("Stats", "Reports", "ViewModel")
    }
}
