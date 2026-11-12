package mx.loperstack.tokkiphealth.common.shell.ipc;

import mx.loperstack.tokkiphealth.common.ipc.RemoteInputStream;
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsCmd;
import mx.loperstack.tokkiphealth.common.shell.ipc.ShellOpsResult;

interface ShellOpsConnection {

   ShellOpsResult execute(in ShellOpsCmd cmd);

   RemoteInputStream executeStream(in ShellOpsCmd cmd);

}