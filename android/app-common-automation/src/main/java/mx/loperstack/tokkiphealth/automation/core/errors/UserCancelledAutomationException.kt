package mx.loperstack.tokkiphealth.automation.core.errors

import kotlinx.coroutines.CancellationException

class UserCancelledAutomationException : CancellationException("User has cancelled automation")