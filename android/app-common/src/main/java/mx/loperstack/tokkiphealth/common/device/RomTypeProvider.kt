package mx.loperstack.tokkiphealth.common.device

fun interface RomTypeProvider {
    suspend fun getRomType(): RomType
}
