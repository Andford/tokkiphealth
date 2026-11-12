package mx.loperstack.tokkiphealth.corpsefinder.core

val CorpseFinder.Data?.hasData: Boolean
    get() = this?.corpses?.isNotEmpty() ?: false