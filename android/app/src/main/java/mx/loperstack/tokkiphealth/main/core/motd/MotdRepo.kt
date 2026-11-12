package mx.loperstack.tokkiphealth.main.core.motd

import mx.loperstack.tokkiphealth.common.BuildConfigWrap
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.INFO
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.flow.replayingShare
import mx.loperstack.tokkiphealth.common.locale.LocaleManager
import mx.loperstack.tokkiphealth.common.locale.primary
import mx.loperstack.tokkiphealth.main.core.GeneralSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MotdRepo @Inject constructor(
    @AppScope private val scope: CoroutineScope,
    private val endpoint: MotdEndpoint,
    private val settings: MotdSettings,
    generalSettings: GeneralSettings,
    localeManager: LocaleManager,
) {
    private val refreshTrigger = MutableStateFlow(UUID.randomUUID())

    val motd: Flow<MotdState?> = combine(
        refreshTrigger,
        settings.isMotdEnabled.flow,
        generalSettings.isOnboardingCompleted.flow,
        localeManager.currentLocales,
    ) { _, isEnabled, isOnboardingCompleted, locales ->
        log(TAG, VERBOSE) { "isEnabled=$isEnabled, isOnboardingCompleted=$isOnboardingCompleted" }

        if (!isOnboardingCompleted) {
            log(TAG, INFO) { "Onboarding is not yet complete!" }
            return@combine null
        }
        if (!isEnabled || !FETCHES) {
            log(TAG, INFO) { "MOTD is disabled." }
            return@combine null
        }
        try {
            val newMotd = endpoint.getMotd(locales.primary)
                ?.takeIf { val min = it.motd.minimumVersion; min == null || BuildConfigWrap.VERSION_CODE >= min }
                ?.takeIf { val max = it.motd.maximumVersion; max == null || BuildConfigWrap.VERSION_CODE <= max }

            settings.lastMotd.value(newMotd)
            log(TAG) { "New MOTD is $newMotd" }
            newMotd
        } catch (e: Exception) {
            log(TAG, ERROR) { "Failed to retrieve MOTD" }
            null
        }
    }
        .onStart {
            emit(if (FETCHES && settings.isMotdEnabled.value()) settings.lastMotd.value() else null)
        }
        .flatMapLatest { motd -> settings.lastDismissedMotd.flow.map { motd to it } }
        .map { (motd, dismissedId) ->
            if (motd != null && motd.id != dismissedId) motd else null
        }
        .replayingShare(scope)

    suspend fun dismiss(id: UUID) {
        log(TAG) { "dismiss(${id})" }
        settings.lastDismissedMotd.value(id)
    }

    companion object {
        /** Tokkip Health asks no one for news: the network only goes where the user asks. */
        private const val FETCHES = false
        private val TAG = logTag("Motd", "Repo")
    }
}