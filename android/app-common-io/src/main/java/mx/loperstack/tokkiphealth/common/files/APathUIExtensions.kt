package mx.loperstack.tokkiphealth.common.files

import androidx.annotation.StringRes

@get:StringRes
val FileType.labelRes: Int
    get() = when (this) {
        FileType.DIRECTORY -> mx.loperstack.tokkiphealth.common.R.string.file_type_directory
        FileType.SYMBOLIC_LINK -> mx.loperstack.tokkiphealth.common.R.string.file_type_symbolic_link
        FileType.FILE -> mx.loperstack.tokkiphealth.common.R.string.file_type_file
        FileType.UNKNOWN -> mx.loperstack.tokkiphealth.common.R.string.file_type_unknown
    }
