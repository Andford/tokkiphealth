package mx.loperstack.tokkiphealth.systemcleaner.core.filter.custom

import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.systemcleaner.core.SystemCleanerSettings
import mx.loperstack.tokkiphealth.systemcleaner.core.filter.FilterIdentifier
import kotlinx.coroutines.flow.first


suspend fun CustomFilterRepo.currentConfigs() = configs.first()


suspend fun SystemCleanerSettings.toggleCustomFilter(filterId: FilterIdentifier, enabled: Boolean? = null) {
    log(TAG) { "toggleCustomFilter($filterId, $enabled)" }
    enabledCustomFilter.update {
        if (it.contains(filterId) || enabled == false) it - filterId else it + filterId
    }
}

suspend fun SystemCleanerSettings.clearCustomFilter(filterId: FilterIdentifier) {
    log(TAG) { "clearCustomFilter($filterId)" }
    enabledCustomFilter.update { it - filterId }
}

suspend fun SystemCleanerSettings.isCustomFilterEnabled(filterId: FilterIdentifier): Boolean {
    return enabledCustomFilter.value().contains(filterId)
}

private val TAG = logTag("SystemCleaner", "CustomFilter")