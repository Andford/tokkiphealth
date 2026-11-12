package mx.loperstack.tokkiphealth.appcleaner.core.forensics.filter

import mx.loperstack.tokkiphealth.common.areas.DataArea

data class WhatsAppMapping(
    val location: DataArea.Type,
    val pkg: String,
    val folder1: String,
    val folder2: String
)