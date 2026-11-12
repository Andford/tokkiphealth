package mx.loperstack.tokkiphealth.common.files.local

import android.system.Os
import mx.loperstack.tokkiphealth.common.files.Ownership
import mx.loperstack.tokkiphealth.common.files.Permissions
import java.io.File

fun File.toLocalPath(): LocalPath = LocalPath.build(this)

fun File.setPermissions(permissions: Permissions): Boolean {
    Os.chmod(path, permissions.mode)
    return true
}

fun File.setOwnership(ownership: Ownership): Boolean {
    Os.lchown(path, ownership.userId.toInt(), ownership.groupId.toInt())
    return true
}