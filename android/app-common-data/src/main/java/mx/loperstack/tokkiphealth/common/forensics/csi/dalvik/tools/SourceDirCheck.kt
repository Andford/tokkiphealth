package mx.loperstack.tokkiphealth.common.forensics.csi.dalvik.tools

import dagger.Reusable
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.Owner
import mx.loperstack.tokkiphealth.common.forensics.csi.dalvik.DalvikCheck
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.current
import mx.loperstack.tokkiphealth.common.pkgs.features.SourceAvailable
import javax.inject.Inject

@Reusable
class SourceDirCheck @Inject constructor(
    private val pkgRepo: PkgRepo,
) : DalvikCheck {

    suspend fun check(areaInfo: AreaInfo, candidates: Collection<LocalPath>): DalvikCheck.Result {
        val ownerPkg = pkgRepo.current()
            .filterIsInstance<SourceAvailable>()
            .filter { it.sourceDir != null }
            .firstOrNull { pkg -> candidates.any { it.path == pkg.sourceDir?.path } }

        return DalvikCheck.Result(
            owners = setOfNotNull(ownerPkg?.id?.let { Owner(it, areaInfo.userHandle) })
        )
    }
}