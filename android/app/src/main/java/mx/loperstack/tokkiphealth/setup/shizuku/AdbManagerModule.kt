package mx.loperstack.tokkiphealth.setup.shizuku

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdbManagerModule {

    @Binds
    @Singleton
    abstract fun installGuide(guide: ShizukuInstallGuide): AdbManagerInstallGuide
}
