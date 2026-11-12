package mx.loperstack.tokkiphealth.corpsefinder.core.watcher

interface WatcherNotifications {
    fun notifyOfScan(result: ExternalWatcherResult.Scan)
    fun notifyOfDeletion(result: ExternalWatcherResult.Deletion)
    fun clearNotifications()
}
