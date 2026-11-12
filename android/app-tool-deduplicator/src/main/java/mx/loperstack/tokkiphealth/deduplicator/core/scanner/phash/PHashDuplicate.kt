package mx.loperstack.tokkiphealth.deduplicator.core.scanner.phash

import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import mx.loperstack.tokkiphealth.deduplicator.core.scanner.phash.phash.PHasher

data class PHashDuplicate(
    override val lookup: APathLookup<*>,
    val hash: PHasher.Result,
    val similarity: Double,
) : Duplicate {

    override val type: Duplicate.Type
        get() = Duplicate.Type.PHASH

    data class Group(
        override val identifier: Duplicate.Group.Id,
        override val duplicates: Set<PHashDuplicate>,
        override val keeperIdentifier: Duplicate.Id? = null,
    ) : Duplicate.Group {
        override val type: Duplicate.Type
            get() = Duplicate.Type.PHASH
    }
}