package mx.loperstack.tokkiphealth.common.pkgs.features

import mx.loperstack.tokkiphealth.common.pkgs.Pkg

interface AppStore : Pkg {

    val storeLabel: String

    val urlGenerator: ((Pkg.Id) -> String)?
        get() = null
}