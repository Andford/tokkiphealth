package mx.loperstack.tokkiphealth.common.forensics.csi.source.tools

import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mx.loperstack.tokkiphealth.common.clutter.Marker
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.Owner
import mx.loperstack.tokkiphealth.common.forensics.csi.source.AppSourceCheck
import mx.loperstack.tokkiphealth.common.pkgs.PkgRepo
import mx.loperstack.tokkiphealth.common.pkgs.isInstalled
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId
import javax.inject.Inject

@Reusable
class LuckyPatcherCheck @Inject constructor(
    private val pkgRepo: PkgRepo,
) : AppSourceCheck {

    override suspend fun process(areaInfo: AreaInfo): AppSourceCheck.Result {
        val name: String = areaInfo.file.name

        if (!name.endsWith(".odex") && !name.endsWith(".dex")) return AppSourceCheck.Result()

        val pkgName = LUCKYPATCHER_ODDONES.matchEntire(name)
            ?.groupValues?.get(1)
            ?.toPkgId()
            ?: return AppSourceCheck.Result()

        val userHandle = areaInfo.userHandle

        val owners = mutableSetOf<Owner>()
        if (pkgRepo.isInstalled(pkgName, userHandle)) {
            owners.add(Owner(pkgName, userHandle))
        }

        BAD_UNCLES
            .map { it.toPkgId() }
            .filter { pkgRepo.isInstalled(it, userHandle) }
            .forEach {
                owners.add(Owner(it, userHandle, setOf(Marker.Flag.CUSTODIAN)))
            }

        return AppSourceCheck.Result(owners)
    }

    @Module @InstallIn(SingletonComponent::class)
    abstract class DIM {
        @Binds @IntoSet abstract fun mod(mod: LuckyPatcherCheck): AppSourceCheck
    }

    companion object {
        private val LUCKYPATCHER_ODDONES by lazy { Regex("^(.+?)-[0-9]{1,4}\\.o?dex$") }
        private val BAD_UNCLES = setOf(
            "com.forpda.lp",
            "com.chelpus.lackypatch",
            "com.dimonvideo.luckypatcher",
            "com.android.vending.billing.InAppBillingService.LUCK"
        )
    }
}