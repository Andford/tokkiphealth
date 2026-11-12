# Stock filters and their factories are logged by class name (toString() / default Object.toString());
# keep the names readable in obfuscated gplay logs. Names only, members and shrinking unaffected.
-keepnames class * implements mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter
-keepnames class * implements mx.loperstack.tokkiphealth.systemcleaner.core.filter.SystemCleanerFilter$Factory
