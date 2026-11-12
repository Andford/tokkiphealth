package mx.loperstack.tokkiphealth.common.pkgs

import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.pkgs.features.Installed

fun Installed.getPrivateDataDirs(areas: Collection<DataArea>): Collection<APath> {
    val privateAreas = areas
        .filter { it.type == DataArea.Type.PRIVATE_DATA }
        .filter { it.userHandle == userHandle }

    if (privateAreas.isEmpty()) log(TAG, WARN) { "No PRIVATE_DATA areas provided" }

    return privateAreas.map { it.path.child(packageName) }
}


private val TAG = logTag("Pkg", "InstalledPathAreaMapping")
