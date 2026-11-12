package mx.loperstack.tokkiphealth.common.forensics.csi.source

import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.Owner

interface AppSourceCheck {

    suspend fun process(areaInfo: AreaInfo): Result


    data class Result(
        val owners: Set<Owner> = emptySet(),
        val hasKnownUnknownOwner: Boolean = false,
    )
}