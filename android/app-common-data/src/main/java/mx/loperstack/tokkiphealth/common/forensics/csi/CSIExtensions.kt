package mx.loperstack.tokkiphealth.common.forensics.csi

import mx.loperstack.tokkiphealth.common.clutter.Marker
import mx.loperstack.tokkiphealth.common.forensics.AreaInfo
import mx.loperstack.tokkiphealth.common.forensics.Owner


fun Marker.Match.toOwners(areaInfo: AreaInfo) = this.packageNames.map {
    Owner(
        pkgId = it,
        flags = flags,
        userHandle = areaInfo.userHandle,
    )
}