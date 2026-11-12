package mx.loperstack.tokkiphealth.appcontrol.core.automation.specs

import mx.loperstack.tokkiphealth.automation.core.specs.AutomationSpec
import mx.loperstack.tokkiphealth.automation.core.specs.SpecGenerator
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed

interface AppControlSpecGenerator : SpecGenerator {
    suspend fun getForceStop(pkg: Installed): AutomationSpec
    suspend fun getArchive(pkg: Installed): AutomationSpec
    suspend fun getRestore(pkg: Installed): AutomationSpec
}