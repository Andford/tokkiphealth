package mx.loperstack.tokkiphealth.common.root.service;

import mx.loperstack.tokkiphealth.common.files.local.ipc.FileOpsConnection;
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.ipc.PkgOpsConnection;
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsConnection;

interface RootServiceConnection {
    String checkBase();

    FileOpsConnection getFileOps();

    PkgOpsConnection getPkgOps();

    ShellOpsConnection getShellOps();
}