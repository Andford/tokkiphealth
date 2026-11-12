package mx.loperstack.tokkiphealth.common.adb;

import mx.loperstack.tokkiphealth.common.files.local.ipc.FileOpsConnection;
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.ipc.PkgOpsConnection;
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsConnection;

interface AdbServiceConnection {
    String checkBase();

    FileOpsConnection getFileOps();

    PkgOpsConnection getPkgOps();

    ShellOpsConnection getShellOps();
}