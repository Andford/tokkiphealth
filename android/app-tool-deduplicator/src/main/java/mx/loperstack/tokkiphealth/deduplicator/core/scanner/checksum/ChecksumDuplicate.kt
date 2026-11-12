package mx.loperstack.tokkiphealth.deduplicator.core.scanner.checksum

import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.hashing.Hasher
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate

data class ChecksumDuplicate(
    override val lookup: APathLookup<*>,
    val hash: Hasher.Result,
) : Duplicate {

    override val type: Duplicate.Type
        get() = Duplicate.Type.CHECKSUM

    data class Group(
        override val duplicates: Set<ChecksumDuplicate>,
        override val identifier: Duplicate.Group.Id,
        override val keeperIdentifier: Duplicate.Id? = null,
    ) : Duplicate.Group {

        override val type: Duplicate.Type
            get() = Duplicate.Type.CHECKSUM

        val preview: APathLookup<*>
            get() = duplicates.first().lookup
    }
}