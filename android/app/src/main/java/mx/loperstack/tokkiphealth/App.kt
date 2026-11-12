package mx.loperstack.tokkiphealth

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.Coil
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import mx.loperstack.tokkiphealth.common.BuildConfigWrap
import mx.loperstack.tokkiphealth.common.BuildWrap
import mx.loperstack.tokkiphealth.common.coil.CoilTempFiles
import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.debug.AutomaticBugReporter
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.DebugSettings
import mx.loperstack.tokkiphealth.common.debug.logging.LogCatLogger
import mx.loperstack.tokkiphealth.common.debug.logging.Logging
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.error.installErrorDialogCustomizer
import mx.loperstack.tokkiphealth.common.debug.exit.ExitInfoLogger
import mx.loperstack.tokkiphealth.common.debug.memory.MemoryMonitor
import mx.loperstack.tokkiphealth.common.debug.recorder.core.RecorderModule
import mx.loperstack.tokkiphealth.common.storage.StorageRescue
import mx.loperstack.tokkiphealth.common.upgrade.UpgradeRepo
import mx.loperstack.tokkiphealth.corpsefinder.core.watcher.UninstallWatcherControl
import mx.loperstack.tokkiphealth.main.core.CurriculumVitae
import mx.loperstack.tokkiphealth.main.core.shortcuts.ShortcutManager
import mx.loperstack.tokkiphealth.main.core.taskmanager.TaskResultNotifier
import mx.loperstack.tokkiphealth.stats.core.LowSpaceMonitor
import mx.loperstack.tokkiphealth.stats.core.SpaceMonitorControl
import mx.loperstack.tokkiphealth.stats.core.TaskStatsCoordinator
import mx.loperstack.tokkiphealth.widget.WidgetRefreshCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltAndroidApp
open class App : Application(), Configuration.Provider {

    @Inject @AppScope lateinit var appScope: CoroutineScope
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var bugReporter: AutomaticBugReporter
    @Inject lateinit var recorderModule: RecorderModule
    @Inject lateinit var imageLoaderFactory: ImageLoaderFactory
    @Inject lateinit var debugSettings: DebugSettings
    @Inject lateinit var curriculumVitae: CurriculumVitae
    @Inject lateinit var coilTempFiles: CoilTempFiles
    @Inject lateinit var memoryMonitor: MemoryMonitor
    @Inject lateinit var exitInfoLogger: ExitInfoLogger
    @Inject lateinit var shortcutManager: ShortcutManager
    @Inject lateinit var spaceMonitorControl: SpaceMonitorControl
    @Inject lateinit var lowSpaceMonitor: LowSpaceMonitor
    @Inject lateinit var taskStatsCoordinator: TaskStatsCoordinator
    @Inject lateinit var taskResultNotifier: TaskResultNotifier
    @Inject lateinit var storageRescue: StorageRescue
    @Inject lateinit var widgetRefreshCoordinator: WidgetRefreshCoordinator
    @Inject lateinit var uninstallWatcherControl: UninstallWatcherControl
    @Inject lateinit var upgradeRepo: UpgradeRepo

    private val logCatLogger = LogCatLogger()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Runs BEFORE Hilt field-injection in super.onCreate(): release the storage
        // rescue early so the very first DataStore/Room writes triggered by
        // singleton init blocks have somewhere to land. See StorageRescue / #2401.
        StorageRescue.releaseIfNeeded(this)
    }

    override fun onCreate() {
        super.onCreate()
        mx.loperstack.tokkip.character.Sounds.init(this)
        if (BuildConfigWrap.DEBUG) {
            Logging.install(logCatLogger)
            log(TAG) { "BuildConfigWrap.DEBUG=true" }
        }
        log(TAG) { "Fingerprint: ${BuildWrap.FINGERPRINT}" }

        combine(
            debugSettings.isDebugMode.flow,
            debugSettings.isTraceMode.flow,
            debugSettings.isDryRunMode.flow,
            recorderModule.state,
        ) { isDebug, isTrace, isDryRun, recorder ->
            log(TAG) { "isDebug=$isDebug, isTrace=$isTrace, isDryRun=$isDryRun, recorder=$recorder" }

            if (isDebug) {
                Logging.install(logCatLogger)
            } else {
                Logging.remove(logCatLogger)
            }

            Bugs.isDebug = isDebug || recorder.isRecording
            Bugs.isTrace = isDebug && isTrace
            Bugs.isDryRun = isDebug && isDryRun
        }.launchIn(appScope)

        bugReporter.setup(this)

        installErrorDialogCustomizer()

        memoryMonitor.register()
        exitInfoLogger.logPreviousExits()

        appScope.launch { coilTempFiles.cleanUp() }
        appScope.launch { storageRescue.restoreIfPossible() }
        Coil.setImageLoader(imageLoaderFactory)

        curriculumVitae.updateAppLaunch()

        shortcutManager.initialize()
        taskStatsCoordinator.start()
        taskResultNotifier.start()
        spaceMonitorControl.start()
        appScope.launch { upgradeRepo.onAppStart() }
        lowSpaceMonitor.start(appScope)
        widgetRefreshCoordinator.start()
        uninstallWatcherControl.start()

        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            log(TAG, ERROR) { "UNCAUGHT EXCEPTION: ${throwable.asLog()}" }

            if (throwable is OutOfMemoryError) {
                log(TAG, ERROR) { "OutOfMemoryError detected! Memory risk: ${memoryMonitor.getMemoryPressureRisk()}" }
                memoryMonitor.logCurrentMemoryState()
            }

            if (oldHandler != null) oldHandler.uncaughtException(thread, throwable) else exitProcess(1)
            Thread.sleep(100)
        }

        log(TAG) { "onCreate() done! ${Exception().asLog()}" }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(
                when {
                    BuildConfigWrap.DEBUG -> android.util.Log.VERBOSE
                    BuildConfigWrap.BUILD_TYPE == BuildConfigWrap.BuildType.DEV -> android.util.Log.DEBUG
                    BuildConfigWrap.BUILD_TYPE == BuildConfigWrap.BuildType.BETA -> android.util.Log.INFO
                    BuildConfigWrap.BUILD_TYPE == BuildConfigWrap.BuildType.RELEASE -> android.util.Log.WARN
                    else -> android.util.Log.VERBOSE
                }
            )
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        internal val TAG = logTag("App")
    }
}
