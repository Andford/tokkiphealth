package mx.loperstack.tokkiphealth.common.forensics

import mx.loperstack.tokkiphealth.common.files.APathLookup

suspend fun FileForensics.identifyArea(lookup: APathLookup<*>) = identifyArea(lookup.lookedUp)