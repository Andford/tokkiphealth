package mx.loperstack.tokkiphealth.setup.shizuku

import mx.loperstack.tokkiphealth.R
import javax.inject.Inject

/** Tokkip Health points to Shizuku: on a Pixel without root it's the way to ADB access. */
class ShizukuInstallGuide @Inject constructor() : AdbManagerInstallGuide {
    override val labelRes: Int = R.string.setup_shizuku_install_manager_label
    override val url: String = "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"
    override val porterHelpUrl: String = "https://sdmse.darken.eu/setup#porter"
}
