package mx.loperstack.tokkiphealth.swiper.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry
import mx.loperstack.tokkiphealth.common.navigation.routes.SwiperSessionsRoute
import mx.loperstack.tokkiphealth.swiper.ui.sessions.SwiperSessionsScreenHost
import mx.loperstack.tokkiphealth.swiper.ui.settings.SwiperSettingsScreenHost
import mx.loperstack.tokkiphealth.swiper.ui.status.SwiperStatusScreenHost
import mx.loperstack.tokkiphealth.swiper.ui.swipe.SwiperSwipeScreenHost
import javax.inject.Inject

class SwiperNavigation @Inject constructor() : NavigationEntry {

    override fun EntryProviderScope<NavKey>.setup() {
        entry<SwiperSettingsRoute> { SwiperSettingsScreenHost() }
        entry<SwiperStatusRoute> { route -> SwiperStatusScreenHost(route = route) }
        entry<SwiperSessionsRoute> { SwiperSessionsScreenHost() }
        entry<SwiperSwipeRoute> { route -> SwiperSwipeScreenHost(route = route) }
    }
}
