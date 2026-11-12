package mx.loperstack.tokkiphealth.appcontrol.core.usage

import android.app.usage.UsageStatsManager
import dagger.Reusable
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.DEBUG
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import mx.loperstack.tokkiphealth.common.sharedresource.HasSharedResource
import mx.loperstack.tokkiphealth.common.sharedresource.SharedResource
import mx.loperstack.tokkiphealth.common.user.UserManager2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import java.util.Calendar
import javax.inject.Inject

@Reusable
class UsageTool @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    dispatcherProvider: DispatcherProvider,
    private val statsManager: UsageStatsManager,
    private val userManager2: UserManager2,
) : HasSharedResource<Any> {

    override val sharedResource = SharedResource.createKeepAlive(TAG, appScope + dispatcherProvider.IO)

    suspend fun lastMonth(): Set<UsageInfo> {
        log(TAG, DEBUG) { "lastMonth()" }
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val stats = statsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_MONTHLY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )
        log(TAG, VERBOSE) { "${stats.size} stats for last month" }
        // TODO how do we get infos for other users?
        val currentUser = userManager2.currentUser()
        return stats
            .groupBy { it.packageName }
            .map { entry ->
                UsageInfo(
                    installId = InstallId(
                        pkgId = entry.key.toPkgId(),
                        userHandle = currentUser.handle,
                    ),
                    stats = entry.value,
                )
            }
            .toSet()
    }

    companion object {
        private val TAG = logTag("AppControl", "UsageTool")
    }
}