package mx.loperstack.tokkiphealth.common.forensics.csi.dalvik.tools

import dagger.Reusable
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.Owner
import mx.loperstack.tokkiphealth.common.forensics.csi.dalvik.DalvikCheck
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Reusable
class ApkCheck @Inject constructor(
    private val pkgOps: PkgOps
) : DalvikCheck {
    suspend fun check(areaInfo: AreaInfo, candidates: Collection<LocalPath>): DalvikCheck.Result {
        val userHandle = areaInfo.userHandle
        val owners = candidates
            .asFlow()
            .filter { it.name.endsWith(".apk") }
            .mapNotNull { pkgOps.viewArchive(it) }
            .onEach { log(TAG, VERBOSE) { "ApkInfo: $it" } }
            .map {
                if (it.id.name.startsWith("com.google.android.gms.")) {
                    /**
                     * /data/dalvik-cache/arm64/system@product@priv-app@PrebuiltGmsCore@app_chimera@m@PrebuiltGmsCoreRvc_DynamiteModulesC.apk@classes.vdex
                     * to
                     * /system/product/priv-app/PrebuiltGmsCore/app_chimera/m/PrebuiltGmsCoreRvc_DynamiteModulesC.apk
                     *
                     * /data/dalvik-cache/arm64/system@product@priv-app@PrebuiltGmsCore@m@independent@AndroidPlatformServices.apk@classes.dex
                     * to
                     * /system/product/priv-app/PrebuiltGmsCore/m/independent/AndroidPlatformServices.apk
                     */
                    setOf(Owner(it.id, userHandle), Owner("com.google.android.gms".toPkgId(), userHandle))
                } else {
                    setOf(Owner(it.id, userHandle))
                }
            }
            .firstOrNull()
            ?: emptySet()

        return DalvikCheck.Result(owners)
    }

    companion object {
        val TAG: String = logTag("CSI", "Dalvik", "Dex", "ApkCheck")
    }
}