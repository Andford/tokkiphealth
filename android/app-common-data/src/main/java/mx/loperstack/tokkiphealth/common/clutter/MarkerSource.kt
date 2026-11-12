package mx.loperstack.tokkiphealth.common.clutter

import mx.loperstack.tokkiphealth.common.areas.DataArea
import mx.loperstack.tokkiphealth.common.pkgs.Pkg

interface MarkerSource {
    suspend fun getMarkerForLocation(areaType: DataArea.Type): Collection<Marker>
    suspend fun getMarkerForPkg(pkgId: Pkg.Id): Collection<Marker>
    suspend fun match(areaType: DataArea.Type, prefixFreeBasePath: List<String>): Collection<Marker.Match>
}