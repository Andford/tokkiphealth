package mx.loperstack.tokkiphealth.squeezer.core

val Squeezer.Data?.hasData: Boolean
    get() = this?.media?.isNotEmpty() ?: false
