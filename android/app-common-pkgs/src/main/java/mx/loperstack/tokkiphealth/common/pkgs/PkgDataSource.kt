package mx.loperstack.tokkiphealth.common.pkgs

import mx.loperstack.tokkiphealth.common.pkgs.features.Installed

interface PkgDataSource {
    suspend fun getPkgs(): Collection<Installed>
}