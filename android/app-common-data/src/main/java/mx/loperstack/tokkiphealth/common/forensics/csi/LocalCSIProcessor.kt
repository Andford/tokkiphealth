package mx.loperstack.tokkiphealth.common.forensics.csi

import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.CSIProcessor
import mx.loperstack.tokkiphealth.common.forensics.Owner
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.isInstalled

interface LocalCSIProcessor : CSIProcessor {

    suspend fun PkgRepo.isInstalled(owner: Owner): Boolean = this.isInstalled(owner.pkgId, owner.userHandle)

    override suspend fun findOwners(areaInfo: AreaInfo): CSIProcessor.Result {
        require(hasJurisdiction(areaInfo.type)) { "Wrong jurisdiction: ${areaInfo.type}" }
        return CSIProcessor.Result()
    }
}