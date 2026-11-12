package mx.loperstack.tokkiphealth.systemcleaner.ui

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry

@Module
@InstallIn(SingletonComponent::class)
abstract class SystemCleanerNavigationModule {

    @Binds
    @IntoSet
    abstract fun systemCleanerNavigation(entry: SystemCleanerNavigation): NavigationEntry
}
