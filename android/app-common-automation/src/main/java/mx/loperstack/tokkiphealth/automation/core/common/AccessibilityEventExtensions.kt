package mx.loperstack.tokkiphealth.automation.core.common

import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.toPkgId

val ACSNodeInfo.pkgId: Pkg.Id get() = packageName.toString().toPkgId()