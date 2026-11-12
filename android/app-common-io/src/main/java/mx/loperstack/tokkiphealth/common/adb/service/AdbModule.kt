package mx.loperstack.tokkiphealth.common.adb.service

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.migration.DisableInstallInCheck
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.shell.SharedShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import javax.inject.Singleton

/**
 * Installed in non-hilt [AdbComponent]
 */
@DisableInstallInCheck
@Module
class AdbModule {

    @Provides
    @Singleton
    fun sharedShell(@AppScope scope: CoroutineScope, dispatcherProvider: DispatcherProvider): SharedShell {
        return SharedShell(AdbHost.TAG + "-sharedShell", scope + dispatcherProvider.IO)
    }

    @Provides
    @Singleton
    @ApplicationContext
    fun adbContext(context: Context): Context {
        return context
    }

}