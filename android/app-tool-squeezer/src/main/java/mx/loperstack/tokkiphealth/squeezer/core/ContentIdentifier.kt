package mx.loperstack.tokkiphealth.squeezer.core

@JvmInline
value class ContentId(val value: String)

sealed interface ContentIdentifier {
    val contentId: ContentId

    data class ImageHash(override val contentId: ContentId) : ContentIdentifier
    data class VideoHash(override val contentId: ContentId) : ContentIdentifier
}
