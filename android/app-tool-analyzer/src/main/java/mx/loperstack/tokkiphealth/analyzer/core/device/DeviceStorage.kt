package mx.loperstack.tokkiphealth.analyzer.core.device

import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.storage.StorageId

data class DeviceStorage(
    val id: StorageId,
    val label: CaString,
    val type: Type,
    val hardware: Hardware,
    val spaceCapacity: Long,
    val spaceFree: Long,
    val setupIncomplete: Boolean
) {
    val spaceUsed: Long
        get() = spaceCapacity - spaceFree

    enum class Type {
        PRIMARY,
        SECONDARY,
        PORTABLE,
        ;
    }

    enum class Hardware {
        BUILT_IN,
        SDCARD,
        USB,
        ;
    }

}


