package mx.loperstack.tokkiphealth.squeezer.ui.preview

import mx.loperstack.tokkiphealth.common.files.FileType
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.files.local.LocalPathLookup
import mx.loperstack.tokkiphealth.squeezer.core.CompressibleImage
import mx.loperstack.tokkiphealth.squeezer.core.CompressibleVideo
import mx.loperstack.tokkiphealth.squeezer.core.PriorCompression
import java.time.Instant

internal fun previewLocalPathLookup(
    pathSegments: Array<String> = arrayOf("storage", "emulated", "0", "DCIM", "Camera", "IMG_20260401.jpg"),
    fileType: FileType = FileType.FILE,
    size: Long = 6L * 1024 * 1024,
    modifiedAt: Instant = Instant.parse("2026-04-01T12:00:00Z"),
): LocalPathLookup = LocalPathLookup(
    lookedUp = LocalPath.build(*pathSegments),
    fileType = fileType,
    size = size,
    modifiedAt = modifiedAt,
    target = null,
)

internal fun previewCompressibleImage(
    lookup: LocalPathLookup = previewLocalPathLookup(),
    mimeType: String = CompressibleImage.MIME_TYPE_JPEG,
    estimatedCompressedSize: Long? = 4L * 1024 * 1024,
    priorCompression: PriorCompression? = null,
    hasLossyAux: Boolean = false,
    hasMotionVideo: Boolean = false,
    willDownscale: Boolean = false,
): CompressibleImage = CompressibleImage(
    lookup = lookup,
    mimeType = mimeType,
    estimatedCompressedSize = estimatedCompressedSize,
    priorCompression = priorCompression,
    hasLossyAux = hasLossyAux,
    hasMotionVideo = hasMotionVideo,
    willDownscale = willDownscale,
)

internal fun previewCompressibleVideo(
    lookup: LocalPathLookup = previewLocalPathLookup(
        pathSegments = arrayOf("storage", "emulated", "0", "DCIM", "Camera", "VID_20260401.mp4"),
        size = 96L * 1024 * 1024,
    ),
    mimeType: String = CompressibleVideo.MIME_TYPE_MP4,
    estimatedCompressedSize: Long? = 64L * 1024 * 1024,
    priorCompression: PriorCompression? = null,
    durationMs: Long = 42_000L,
    bitrateBps: Long = 18_000_000L,
): CompressibleVideo = CompressibleVideo(
    lookup = lookup,
    mimeType = mimeType,
    estimatedCompressedSize = estimatedCompressedSize,
    priorCompression = priorCompression,
    durationMs = durationMs,
    bitrateBps = bitrateBps,
)
