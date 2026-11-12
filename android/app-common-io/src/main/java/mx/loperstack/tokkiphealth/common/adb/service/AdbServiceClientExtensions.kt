package mx.loperstack.tokkiphealth.common.adb.service

import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.sharedresource.runSessionAction


@Suppress("UNCHECKED_CAST")
suspend fun <R, T> AdbServiceClient.runModuleAction(
    moduleClass: Class<out R>,
    action: suspend (R) -> T
): T = runSessionAction { session ->
    if (Bugs.isTrace) {
        log(AdbServiceClient.TAG, VERBOSE) { "runModuleAction(moduleClass=$moduleClass, action=$action)" }
    }
    val module = session.clientModules.single { moduleClass.isInstance(it) } as R
    return@runSessionAction action(module)
}