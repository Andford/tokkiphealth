package mx.loperstack.tokkiphealth.scheduler.ui

import mx.loperstack.tokkiphealth.common.navigation.NavigationDestination
import kotlinx.serialization.Serializable

@Serializable
data object SchedulerSettingsRoute : NavigationDestination

@Serializable
data object SchedulerManagerRoute : NavigationDestination

@Serializable
data class ScheduleItemRoute(
    val scheduleId: String,
) : NavigationDestination
