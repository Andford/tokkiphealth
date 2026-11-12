package mx.loperstack.tokkiphealth.scheduler.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import mx.loperstack.tokkiphealth.common.navigation.modalBottomSheetMetadata
import mx.loperstack.tokkiphealth.scheduler.ui.manager.SchedulerManagerScreenHost
import mx.loperstack.tokkiphealth.scheduler.ui.manager.create.ScheduleItemSheetHost
import mx.loperstack.tokkiphealth.scheduler.ui.settings.SchedulerSettingsScreenHost
import javax.inject.Inject

class SchedulerNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<SchedulerSettingsRoute> { SchedulerSettingsScreenHost() }
        entry<SchedulerManagerRoute> { SchedulerManagerScreenHost() }
        entry<ScheduleItemRoute>(metadata = modalBottomSheetMetadata()) { route ->
            ScheduleItemSheetHost(scheduleId = route.scheduleId)
        }
    }
}
