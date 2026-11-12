package mx.loperstack.tokkiphealth.common.files.local.ipc;

import mx.loperstack.tokkiphealth.common.ipc.RemoteInputStream;
import mx.loperstack.tokkiphealth.common.files.local.LocalPath;

interface DetailedInputSource {
    LocalPath path();
    long length();
    RemoteInputStream input();
}