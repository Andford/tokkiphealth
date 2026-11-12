package mx.loperstack.tokkiphealth.common.pkgs.container

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import mx.loperstack.tokkiphealth.common.ca.CaString
import mx.loperstack.tokkiphealth.common.ca.caString
import mx.loperstack.tokkiphealth.common.ca.cache
import mx.loperstack.tokkiphealth.common.io.R
import mx.loperstack.tokkiphealth.common.pkgs.AKnownPkg
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.features.PermissionDetails
import mx.loperstack.tokkiphealth.common.pkgs.features.PkgInfo
import mx.loperstack.tokkiphealth.common.pkgs.getIcon2
import mx.loperstack.tokkiphealth.common.pkgs.getLabel2

data class PkgArchive(
    override val id: Pkg.Id,
    override val packageInfo: PackageInfo
) : PkgInfo, PermissionDetails {

    override val label: CaString = caString { context ->
        context.packageManager.getLabel2(id)?.let { return@caString it }

        AKnownPkg.values
            .singleOrNull { it.id == id }
            ?.labelRes
            ?.let { return@caString context.getString(it) }

        id.name
    }.cache()

    override val icon: ((Context) -> Drawable)? = { context ->
        context.packageManager.getIcon2(id)
            ?: AKnownPkg.values.singleOrNull { it.id == id }?.icon?.invoke(context)
            ?: ContextCompat.getDrawable(context, R.drawable.ic_default_app_icon_24)!!
    }
}
