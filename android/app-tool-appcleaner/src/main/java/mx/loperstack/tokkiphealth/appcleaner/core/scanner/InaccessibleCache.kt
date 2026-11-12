package mx.loperstack.tokkiphealth.appcleaner.core.scanner

import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.pkgs.Pkg
import mx.loperstack.tokkiphealth.common.pkgs.features.InstallId
import mx.loperstack.tokkiphealth.common.user.UserHandle2

data class InaccessibleCache(
    val identifier: InstallId,
    val isSystemApp: Boolean,
    val itemCount: Int,
    val totalSize: Long,
    val publicSize: Long?,
    val theoreticalPaths: Set<APath>,
) {

    val pkgId: Pkg.Id
        get() = identifier.pkgId

    val userHandle: UserHandle2
        get() = identifier.userHandle

    val privateSize: Long = totalSize - (publicSize ?: 0L)

    val isEmpty: Boolean = totalSize == 0L
}