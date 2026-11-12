package mx.loperstack.tokkiphealth.squeezer.ui

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry

@Module
@InstallIn(SingletonComponent::class)
abstract class SqueezerNavigationModule {

    @Binds
    @IntoSet
    abstract fun squeezerNavigation(entry: SqueezerNavigation): NavigationEntry
}
