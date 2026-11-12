package mx.loperstack.tokkiphealth.automation.core

import android.content.Context
import android.content.Intent

fun interface AutomationReturnHelper {
    fun createReturnToAppIntent(context: Context): Intent
}
