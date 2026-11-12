package mx.loperstack.tokkiphealth.common.forensics.csi.dalvik

import mx.loperstack.tokkiphealth.common.forensics.Owner

interface DalvikCheck {

    data class Result(
        val owners: Set<Owner> = emptySet(),
        val hasKnownUnknownOwner: Boolean = false,
    ) {
        fun isEmpty() = owners.isEmpty() && !hasKnownUnknownOwner
    }
}