package mx.loperstack.tokkiphealth.common.forensics

import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.files.APath

interface CSIProcessor {

    suspend fun hasJurisdiction(type: DataArea.Type): Boolean

    suspend fun identifyArea(target: APath): AreaInfo?

    suspend fun findOwners(areaInfo: AreaInfo): Result

    data class Result(
        val owners: Set<Owner> = emptySet(),
        val hasKnownUnknownOwner: Boolean = false,
    )

}