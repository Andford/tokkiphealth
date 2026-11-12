package mx.loperstack.tokkiphealth.common.forensics.csi.source.tools

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.Owner
import mx.loperstack.tokkiphealth.common.forensics.csi.source.AppSourceCheck
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.PkgOps
import javax.inject.Inject

@Reusable
class DirectApkCheck @Inject constructor(
    private val pkgOps: PkgOps,
) : AppSourceCheck {

    override suspend fun process(areaInfo: AreaInfo): AppSourceCheck.Result {
        if (!areaInfo.file.name.endsWith(".apk")) return AppSourceCheck.Result()

        val info = pkgOps.viewArchive(areaInfo.file, 0)
        val userHandle = areaInfo.userHandle

        val owners = if (info != null) {
            setOf(Owner(info.id, userHandle))
        } else {
            emptySet()
        }
        return AppSourceCheck.Result(owners)
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: DirectApkCheck): AppSourceCheck
    }
}