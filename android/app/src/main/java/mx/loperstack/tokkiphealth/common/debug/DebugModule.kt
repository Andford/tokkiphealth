package mx.loperstack.tokkiphealth.common.debug

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mx.loperstack.tokkiphealth.common.debug.autoreporting.FossAutoReporting
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class DebugModule {
    @Binds
    @Singleton
    abstract fun autoreporting(foss: FossAutoReporting): AutomaticBugReporter
}