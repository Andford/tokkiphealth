package mx.loperstack.tokkiphealth.common.areas.modules

import mx.loperstack.tokkiphealth.common.areas.DataArea

interface DataAreaModule {

    suspend fun firstPass(): Collection<DataArea> = emptySet()

    suspend fun secondPass(firstPass: Collection<DataArea>): Collection<DataArea> = emptySet()
}