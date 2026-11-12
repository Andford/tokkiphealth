package mx.loperstack.tokkiphealth.appcleaner.ui

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry

@Module
@InstallIn(SingletonComponent::class)
abstract class AppCleanerNavigationModule {

    @Binds
    @IntoSet
    abstract fun appCleanerNavigation(entry: AppCleanerNavigation): NavigationEntry
}
