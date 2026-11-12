package mx.loperstack.tokkiphealth.squeezer.core

import mx.loperstack.tokkiphealth.common.files.APathLookup

data class CompressibleVideo(
    override val lookup: APathLookup<*>,
    override val mimeType: String,
    override val estimatedCompressedSize: Long? = null,
    override val priorCompression: PriorCompression? = null,
    val durationMs: Long,
    val bitrateBps: Long,
) : CompressibleMedia {

    companion object {
        const val MIME_TYPE_MP4 = "video/mp4"

        val SUPPORTED_MIME_TYPES = setOf(MIME_TYPE_MP4)
    }
}
