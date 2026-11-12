package mx.loperstack.tokkiphealth.common.clutter


fun Marker.hasFlags(vararg flag: Marker.Flag): Boolean {
    return flag.any { flags.contains(it) }
}