package mx.loperstack.tokkiphealth.systemcleaner.core

val SystemCleaner.Data?.hasData: Boolean
    get() = this?.filterContents?.isNotEmpty() ?: false