package mx.loperstack.tokkiphealth.automation.core.specs

import android.content.res.Resources
import mx.loperstack.tokkiphealth.common.locale.toList


fun AutomationExplorer.Context.getLocales() = Resources.getSystem().configuration.locales.toList()