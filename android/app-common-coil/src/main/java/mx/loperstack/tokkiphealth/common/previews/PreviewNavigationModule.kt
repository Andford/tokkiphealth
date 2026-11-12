package mx.loperstack.tokkiphealth.common.previews

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.navigation.NavigationEntry

@Module
@InstallIn(SingletonComponent::class)
abstract class PreviewNavigationModule {

    @Binds
    @IntoSet
    abstract fun previewNavigation(entry: PreviewNavigation): NavigationEntry
}
