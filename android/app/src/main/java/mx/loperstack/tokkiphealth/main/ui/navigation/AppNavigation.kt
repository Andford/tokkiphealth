package mx.loperstack.tokkiphealth.main.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import mx.loperstack.tokkiphealth.common.navigation.modalBottomSheetMetadata
import mx.loperstack.tokkiphealth.common.navigation.routes.DashboardRoute
import mx.loperstack.tokkiphealth.main.ui.dashboard.DashboardScreenHost
import mx.loperstack.tokkiphealth.common.navigation.routes.DataAreasRoute
import mx.loperstack.tokkiphealth.common.navigation.routes.UpgradeRoute
import mx.loperstack.tokkiphealth.common.upgrade.ui.UpgradeScreenHost
import mx.loperstack.tokkiphealth.main.ui.areas.DataAreasScreenHost
import mx.loperstack.tokkiphealth.main.ui.backup.BackupRestoreScreenHost
import mx.loperstack.tokkiphealth.main.ui.onboarding.privacy.OnboardingPrivacyScreenHost
import mx.loperstack.tokkiphealth.main.ui.onboarding.setup.OnboardingSetupScreenHost
import mx.loperstack.tokkiphealth.main.ui.onboarding.versus.VersusSetupScreenHost
import mx.loperstack.tokkiphealth.main.ui.onboarding.welcome.OnboardingWelcomeScreenHost
import mx.loperstack.tokkiphealth.main.ui.settings.SettingsScreenHost
import mx.loperstack.tokkiphealth.main.ui.settings.acks.AcknowledgementsScreenHost
import mx.loperstack.tokkiphealth.main.ui.settings.cards.DashboardCardConfigScreenHost
import mx.loperstack.tokkiphealth.main.ui.settings.general.GeneralSettingsScreenHost
import mx.loperstack.tokkiphealth.main.ui.settings.support.SupportScreenHost
import mx.loperstack.tokkiphealth.main.ui.settings.support.contactform.SupportContactFormScreenHost
import mx.loperstack.tokkiphealth.main.ui.settings.support.sessions.DebugLogSessionsScreenHost
import mx.loperstack.tokkiphealth.setup.SetupRoute
import mx.loperstack.tokkiphealth.setup.SetupScreenHost
import javax.inject.Inject

class AppNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<DashboardRoute> { DashboardScreenHost() }

        entry<OnboardingWelcomeRoute> { OnboardingWelcomeScreenHost() }
        entry<VersusSetupRoute> { VersusSetupScreenHost() }
        entry<OnboardingPrivacyRoute> { OnboardingPrivacyScreenHost() }
        entry<OnboardingSetupRoute> { OnboardingSetupScreenHost() }

        entry<DataAreasRoute> { DataAreasScreenHost() }
        entry<UpgradeRoute> { route -> UpgradeScreenHost(route = route) }
        entry<SetupRoute> { route -> SetupScreenHost(route = route) }

        entry<SettingsRoute> { SettingsScreenHost() }
        entry<BackupRestoreRoute> { BackupRestoreScreenHost() }
        entry<GeneralSettingsRoute> { GeneralSettingsScreenHost() }
        entry<AcknowledgementsRoute> { AcknowledgementsScreenHost() }
        entry<SupportRoute> { SupportScreenHost() }
        entry<DashboardCardConfigRoute> { DashboardCardConfigScreenHost() }
        entry<SupportFormRoute> { SupportContactFormScreenHost() }
        entry<DebugLogSessionsRoute>(metadata = modalBottomSheetMetadata()) {
            DebugLogSessionsScreenHost()
        }
    }
}
