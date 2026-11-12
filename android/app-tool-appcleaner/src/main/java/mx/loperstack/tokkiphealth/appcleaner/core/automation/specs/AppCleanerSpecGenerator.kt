package mx.loperstack.tokkiphealth.appcleaner.core.automation.specs

import mx.loperstack.tokkiphealth.automation.core.specs.AutomationSpec
import mx.loperstack.tokkiphealth.automation.core.specs.SpecGenerator
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed

interface AppCleanerSpecGenerator : SpecGenerator {

    suspend fun getClearCache(pkg: Installed): AutomationSpec
}