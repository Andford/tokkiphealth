package mx.loperstack.tokkiphealth.corpsefinder.core

import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.OwnerInfo
import mx.loperstack.tokkiphealth.corpsefinder.core.filter.CorpseFilter
import kotlin.reflect.KClass

data class Corpse(
    val filterType: KClass<out CorpseFilter>,
    val ownerInfo: OwnerInfo,
    val lookup: APathLookup<*>,
    val content: Collection<APathLookup<*>>,
    val isWriteProtected: Boolean = false,
    val riskLevel: RiskLevel = RiskLevel.NORMAL,
) {
    val identifier: CorpseIdentifier
        get() = lookup.lookedUp

    val areaInfo: AreaInfo
        get() = ownerInfo.areaInfo

    val size: Long
        get() = lookup.size + content.sumOf { it.size }

    override fun toString(): String =
        "Corpse(identifier=$identifier, type=${areaInfo.type}, owners=${ownerInfo.owners}, size=$size)"
}