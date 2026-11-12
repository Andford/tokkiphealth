package mx.loperstack.tokkiphealth.common.forensics.csi.dalvik.tools

import dagger.Reusable
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.exists
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.forensics.csi.dalvik.DalvikCheck
import javax.inject.Inject

@Reusable
class ExistCheck @Inject constructor(
    private val gatewaySwitch: GatewaySwitch
) : DalvikCheck {

    suspend fun check(candidates: Collection<LocalPath>): DalvikCheck.Result {
        return DalvikCheck.Result(
            hasKnownUnknownOwner = candidates.any {
                it.exists(gatewaySwitch)
            }
        )
    }
}