package mx.loperstack.tokkiphealth.appcontrol.ui

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry

@Module
@InstallIn(SingletonComponent::class)
abstract class AppControlNavigationModule {

    @Binds
    @IntoSet
    abstract fun appControlNavigation(entry: AppControlNavigation): NavigationEntry
}
