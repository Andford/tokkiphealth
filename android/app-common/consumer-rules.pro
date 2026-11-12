# Task classes are logged by class name (OneTapCleaner, TaskManager's TaskEntry.toString()) and
# route classes label the unknown-destination fallback screen; keep the names readable in
# obfuscated gplay builds. Names only, members and shrinking unaffected.
-keepnames class * implements mx.loperstack.tokkiphealth.main.core.SDMTool$Task
-keepnames class * implements mx.loperstack.tokkiphealth.common.navigation.NavigationDestination

# Loggers are logged by default Object.toString() when installed or removed (Logging.install),
# upgrade info by class name in the dashboard's verbose flow diagnostics (DashboardViewModel).
-keepnames class * implements mx.loperstack.tokkiphealth.common.debug.logging.Logging$Logger
-keepnames class * implements mx.loperstack.tokkiphealth.common.upgrade.UpgradeRepo$Info
