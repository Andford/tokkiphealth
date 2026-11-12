package mx.loperstack.tokkiphealth.deduplicator.core.scanner.media

import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.deduplicator.core.Duplicate
import mx.loperstack.tokkiphealth.deduplicator.core.scanner.media.audiohash.AudioFingerprinter
import mx.loperstack.tokkiphealth.deduplicator.core.scanner.phash.phash.PHasher

data class MediaDuplicate(
    override val lookup: APathLookup<*>,
    val audioHash: AudioFingerprinter.Result?,
    val frameHashes: List<PHasher.Result>,
    val similarity: Double,
) : Duplicate {

    override val type: Duplicate.Type
        get() = Duplicate.Type.MEDIA

    data class Group(
        override val identifier: Duplicate.Group.Id,
        override val duplicates: Set<MediaDuplicate>,
        override val keeperIdentifier: Duplicate.Id? = null,
    ) : Duplicate.Group {
        override val type: Duplicate.Type
            get() = Duplicate.Type.MEDIA
    }
}
