package mx.loperstack.tokkiphealth.common.forensics.csi.dalvik.tools

import dagger.Reusable
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.Owner
import mx.loperstack.tokkiphealth.common.forensics.csi.dalvik.DalvikCheck
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.isInstalled
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import javax.inject.Inject

@Reusable
class DirNameCheck @Inject constructor(
    private val pkgRepo: PkgRepo,
) : DalvikCheck {

    suspend fun process(areaInfo: AreaInfo): DalvikCheck.Result {
        val potPkg = areaInfo.prefixFreeSegments.first().toPkgId()
        return if (pkgRepo.isInstalled(potPkg, areaInfo.userHandle)) {
            DalvikCheck.Result(setOf(Owner(potPkg, areaInfo.userHandle)))
        } else {
            return DalvikCheck.Result()
        }
    }
}