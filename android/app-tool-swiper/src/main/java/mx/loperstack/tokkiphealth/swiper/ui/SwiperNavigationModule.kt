package mx.loperstack.tokkiphealth.swiper.ui

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry

@Module
@InstallIn(SingletonComponent::class)
abstract class SwiperNavigationModule {

    @Binds
    @IntoSet
    abstract fun swiperNavigation(entry: SwiperNavigation): NavigationEntry
}
