package mx.loperstack.tokkiphealth.common.forensics

import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.Segments
import mx.loperstack.tokkiphealth.common.user.UserHandle2
import kotlinx.parcelize.IgnoredOnParcel

data class AreaInfo(
    val file: APath,
    val prefix: APath,
    val dataArea: DataArea,
    val isBlackListLocation: Boolean,
) {

    val type: DataArea.Type
        get() = dataArea.type

    val userHandle: UserHandle2
        get() = dataArea.userHandle

    // TODO would subList have noticeably better performance?
    @IgnoredOnParcel internal var prefixCache: Segments? = null

    val prefixFreeSegments: Segments
        //        get() = file.segments.drop(prefix.segments.size)
        get() = prefixCache
            ?: run {
                file.segments.subList(prefix.segments.size, file.segments.size)
            }.also { prefixCache = it }

}