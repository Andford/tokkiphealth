package mx.loperstack.tokkiphealth.deduplicator.core

val Deduplicator.Data?.hasData: Boolean
    get() = this?.clusters?.isNotEmpty() ?: false
