package mx.loperstack.tokkiphealth.common.adb

import kotlinx.coroutines.flow.first

suspend fun AdbManager.canUseAdbNow(): Boolean = useAdb.first()