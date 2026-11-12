package mx.loperstack.tokkiphealth.common.root.service

import android.content.Context
import androidx.annotation.Keep
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.flowshell.core.cmd.FlowCmd
import eu.darken.flowshell.core.cmd.execute
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.local.ipc.FileOpsConnection
import mx.loperstack.tokkiphealth.common.files.local.ipc.FileOpsHost
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.ipc.PkgOpsConnection
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.ipc.PkgOpsHost
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsConnection
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsHost
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Keep
class RootServiceHost @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileOpsHost: Lazy<FileOpsHost>,
    private val pkgOpsHost: Lazy<PkgOpsHost>,
    private val shellOpsHost: Lazy<ShellOpsHost>,
) : RootServiceConnection.Stub() {

    override fun checkBase(): String {
        val sb = StringBuilder()
        sb.append("Our pkg: ${context.packageName}\n")
        val ids = runBlocking { FlowCmd("id").execute() }
        sb.append("Shell ids are: ${ids.merged}\n")
        val result = sb.toString()
        log(TAG) { "checkBase(): $result" }
        return result
    }

    override fun getFileOps(): FileOpsConnection = fileOpsHost.get()

    override fun getPkgOps(): PkgOpsConnection = pkgOpsHost.get()

    override fun getShellOps(): ShellOpsConnection = shellOpsHost.get()

    companion object {
        private val TAG = logTag("Root", "Service", "Host")
    }
}