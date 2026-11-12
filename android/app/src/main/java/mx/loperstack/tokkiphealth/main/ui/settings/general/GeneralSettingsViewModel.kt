package mx.loperstack.tokkiphealth.main.ui.settings.general

import android.annotation.SuppressLint
import android.os.LocaleList
import dagger.hilt.android.lifecycle.HiltViewModel
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.datastore.value
import mx.loperstack.tokkiphealth.common.debug.DebugSettings
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.device.RomType
import mx.loperstack.tokkiphealth.common.flow.combine
import mx.loperstack.tokkiphealth.common.hasApiLevel
import mx.loperstack.tokkiphealth.common.locale.LocaleManager
import mx.loperstack.tokkiphealth.common.theming.ThemeColor
import mx.loperstack.tokkiphealth.common.theming.ThemeMode
import mx.loperstack.tokkiphealth.common.theming.ThemeState
import mx.loperstack.tokkiphealth.common.theming.ThemeStyle
import mx.loperstack.tokkiphealth.common.uix.ViewModel4
import mx.loperstack.tokkiphealth.common.updater.UpdateChecker
import mx.loperstack.tokkiphealth.common.upgrade.UpgradeRepo
import mx.loperstack.tokkiphealth.main.core.GeneralSettings
import mx.loperstack.tokkiphealth.main.core.motd.MotdSettings
import mx.loperstack.tokkiphealth.main.core.themeState
import mx.loperstack.tokkiphealth.common.compose.tour.GuidedTourController
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    upgradeRepo: UpgradeRepo,
    private val generalSettings: GeneralSettings,
    private val debugSettings: DebugSettings,
    private val motdSettings: MotdSettings,
    private val updateChecker: UpdateChecker,
    private val localeManager: LocaleManager,
    private val guidedTourController: GuidedTourController,
) : ViewModel4(dispatcherProvider, TAG) {

    val state: StateFlow<State> = combine(
        upgradeRepo.upgradeInfo.map { it.isPro },
        flow { emit(updateChecker.isCheckSupported()) },
        generalSettings.enableDashboardOneClick.flow,
        generalSettings.shortcutOneClickEnabled.flow,
        generalSettings.themeState,
        generalSettings.usePreviews.flow,
        generalSettings.romTypeDetection.flow,
        generalSettings.isUpdateCheckEnabled.flow,
        motdSettings.isMotdEnabled.flow,
        debugSettings.isDebugMode.flow,
        localeManager.currentLocales,
        generalSettings.oneClickCorpseFinderEnabled.flow,
        generalSettings.oneClickSystemCleanerEnabled.flow,
        generalSettings.oneClickAppCleanerEnabled.flow,
        generalSettings.oneClickDeduplicatorEnabled.flow,
        generalSettings.widgetOneClickEnabled.flow,
        generalSettings.dashboardHeroAutoShow.flow,
    ) { isPro, isUpdateCheckSupported, oneClick, shortcut, theme, previews, romType, updateCheck, motd, debug, locales,
        oneClickCorpseFinder, oneClickSystemCleaner, oneClickAppCleaner, oneClickDeduplicator, widgetOneClick,
        summaryAutoShow ->
        State(
            isPro = isPro,
            isUpdateCheckSupported = isUpdateCheckSupported,
            enableDashboardOneClick = oneClick,
            shortcutOneClickEnabled = shortcut,
            themeMode = theme.mode,
            themeStyle = theme.style,
            themeColor = theme.color,
            usePreviews = previews,
            romTypeDetection = romType,
            isUpdateCheckEnabled = updateCheck,
            isMotdEnabled = motd,
            isDebugMode = debug,
            currentLocales = locales,
            showLanguage = hasApiLevel(33),
            oneClickCorpseFinderEnabled = oneClickCorpseFinder,
            oneClickSystemCleanerEnabled = oneClickSystemCleaner,
            oneClickAppCleanerEnabled = oneClickAppCleaner,
            oneClickDeduplicatorEnabled = oneClickDeduplicator,
            widgetOneClickEnabled = widgetOneClick,
            dashboardSummaryAutoShow = summaryAutoShow,
        )
    }.safeStateIn(
        initialValue = State(),
        onError = { State() },
    )

    fun toggleOneClick(enabled: Boolean) = launch {
        generalSettings.enableDashboardOneClick.value(enabled)
    }

    fun toggleShortcutOneClick(enabled: Boolean) = launch {
        generalSettings.shortcutOneClickEnabled.value(enabled)
    }

    fun toggleDashboardSummaryAutoShow(enabled: Boolean) = launch {
        generalSettings.dashboardHeroAutoShow.value(enabled)
    }

    fun toggleWidgetOneClick(enabled: Boolean) = launch {
        generalSettings.widgetOneClickEnabled.value(enabled)
        // Enabling here counts as consent, so the widget's one-time in-app prompt never appears
        // (and a later disable+widget-tap doesn't re-prompt).
        if (enabled) generalSettings.widgetOneClickConsentAsked.value(true)
    }

    fun setThemeMode(mode: ThemeMode) = launch {
        generalSettings.themeMode.value(mode)
    }

    fun setThemeStyle(style: ThemeStyle) = launch {
        generalSettings.themeStyle.value(style)
    }

    fun setThemeColor(color: ThemeColor) = launch {
        generalSettings.themeColor.value(color)
    }

    fun togglePreviews(enabled: Boolean) = launch {
        generalSettings.usePreviews.value(enabled)
    }

    fun setRomType(romType: RomType) = launch {
        generalSettings.romTypeDetection.value(romType)
    }

    fun toggleUpdateCheck(enabled: Boolean) = launch {
        generalSettings.isUpdateCheckEnabled.value(enabled)
    }

    fun toggleMotd(enabled: Boolean) = launch {
        motdSettings.isMotdEnabled.value(enabled)
    }

    fun toggleDebugMode(enabled: Boolean) = launch {
        debugSettings.isDebugMode.value(enabled)
    }

    fun setOneClickCorpseFinder(enabled: Boolean) = launch {
        generalSettings.oneClickCorpseFinderEnabled.value(enabled)
    }

    fun setOneClickSystemCleaner(enabled: Boolean) = launch {
        generalSettings.oneClickSystemCleanerEnabled.value(enabled)
    }

    fun setOneClickAppCleaner(enabled: Boolean) = launch {
        generalSettings.oneClickAppCleanerEnabled.value(enabled)
    }

    fun setOneClickDeduplicator(enabled: Boolean) = launch {
        generalSettings.oneClickDeduplicatorEnabled.value(enabled)
    }

    fun resetGuidedTours() = launch {
        guidedTourController.reset()
    }

    @SuppressLint("NewApi")
    fun showLanguagePicker() = launch {
        log(TAG) { "showLanguagePicker()" }
        if (hasApiLevel(33)) {
            localeManager.showLanguagePicker()
        } else {
            throw IllegalStateException("This should not be clickable below API 33...")
        }
    }

    data class State(
        val isPro: Boolean = false,
        val isUpdateCheckSupported: Boolean = false,
        val enableDashboardOneClick: Boolean = false,
        val shortcutOneClickEnabled: Boolean = false,
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val themeStyle: ThemeStyle = ThemeStyle.DEFAULT,
        val themeColor: ThemeColor = ThemeColor.GREEN,
        val usePreviews: Boolean = true,
        val romTypeDetection: RomType = RomType.AUTO,
        val isUpdateCheckEnabled: Boolean = false,
        val isMotdEnabled: Boolean = true,
        val isDebugMode: Boolean = false,
        val currentLocales: LocaleList? = null,
        val showLanguage: Boolean = false,
        val oneClickCorpseFinderEnabled: Boolean = true,
        val oneClickSystemCleanerEnabled: Boolean = true,
        val oneClickAppCleanerEnabled: Boolean = true,
        val oneClickDeduplicatorEnabled: Boolean = false,
        val widgetOneClickEnabled: Boolean = false,
        val dashboardSummaryAutoShow: Boolean = true,
    )

    companion object {
        private val TAG = logTag("Settings", "General", "ViewModel")
    }
}
