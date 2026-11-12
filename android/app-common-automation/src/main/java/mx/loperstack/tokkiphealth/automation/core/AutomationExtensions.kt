package mx.loperstack.tokkiphealth.automation.core

import kotlinx.coroutines.flow.first

suspend fun AutomationManager.canUseAcsNow(): Boolean = useAcs.first()
