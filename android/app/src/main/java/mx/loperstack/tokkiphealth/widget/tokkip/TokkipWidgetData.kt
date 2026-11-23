package mx.loperstack.tokkiphealth.widget.tokkip

import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.flow.shareLatest
import mx.loperstack.tokkiphealth.main.core.health.PhoneHealth
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskSubmitter
import mx.loperstack.tokkiphealth.stats.core.StatsRepo
import mx.loperstack.tokkiphealth.stats.core.StatsSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/** What Tokkip's two widgets (Salud and Historial) show: how the phone feels and its cleans. */
data class TokkipWidgetState(
    val health: PhoneHealth,
    val working: Boolean,
    /** Freed per day, oldest first, the last [DAYS] days (today last). */
    val perDay: List<Long>,
    val cleans: Int,
) {
    companion object {
        const val DAYS = 14

        /** Pure, so it's tested without a phone. */
        fun build(
            total: Long,
            free: Long,
            cleans: List<Pair<Instant, Long>>,
            totalFreed: Long,
            working: Boolean,
            now: Instant,
            zone: ZoneId,
        ): TokkipWidgetState {
            val freed = cleans.filter { it.second > 0L }
            val today = now.atZone(zone).toLocalDate()
            val perDay = LongArray(DAYS)
            for ((at, bytes) in freed) {
                val ago = ChronoUnit.DAYS.between(at.atZone(zone).toLocalDate(), today).toInt()
                if (ago in 0 until DAYS) perDay[DAYS - 1 - ago] += bytes
            }
            return TokkipWidgetState(
                health = PhoneHealth.assess(total, free, freed.maxOfOrNull { it.first }, totalFreed, now),
                working = working,
                perDay = perDay.toList(),
                cleans = freed.size,
            )
        }
    }
}

@Singleton
class TokkipWidgetData @Inject constructor(
    @AppScope appScope: CoroutineScope,
    statsRepo: StatsRepo,
    statsSettings: StatsSettings,
    taskSubmitter: TaskSubmitter,
) {
    val state: Flow<TokkipWidgetState> = combine(
        statsRepo.reports
            .map { reports -> reports.map { it.endAt to (it.affectedSpace ?: 0L) } }
            .onStart { emit(emptyList()) }
            .catch { emit(emptyList()) },
        statsSettings.totalSpaceFreed.flow.catch { emit(0L) },
        taskSubmitter.state.map { !it.isIdle }.distinctUntilChanged(),
    ) { cleans, freed, working ->
        val (total, free) = try {
            val fs = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            fs.totalBytes to fs.availableBytes
        } catch (e: Exception) {
            0L to 0L
        }
        TokkipWidgetState.build(total, free, cleans, freed, working, Instant.now(), ZoneId.systemDefault())
    }
        .distinctUntilChanged()
        .shareLatest(appScope)

    suspend fun snapshot(): TokkipWidgetState = state.first()
}
