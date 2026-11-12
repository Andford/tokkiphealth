package mx.loperstack.tokkiphealth.common.files.local.ipc;

import mx.loperstack.tokkiphealth.common.ipc.RemoteFileHandle;
import mx.loperstack.tokkiphealth.common.ipc.RemoteInputStream;
import mx.loperstack.tokkiphealth.common.ipc.RemoteOutputStream;
import mx.loperstack.tokkiphealth.common.files.local.LocalPath;
import mx.loperstack.tokkiphealth.common.files.local.LocalPathLookup;
import mx.loperstack.tokkiphealth.common.files.local.LocalPathLookupExtended;
import mx.loperstack.tokkiphealth.common.files.Ownership;
import mx.loperstack.tokkiphealth.common.files.Permissions;

interface FileOpsConnection {

    RemoteFileHandle file(in LocalPath path, boolean readWrite);

    boolean mkdirs(in LocalPath path);
    boolean createNewFile(in LocalPath path);

    boolean canRead(in LocalPath path);
    boolean canWrite(in LocalPath path);

    boolean exists(in LocalPath path);

    boolean delete(in LocalPath path, boolean recursive, boolean dryRun);

    RemoteInputStream listFilesStream(in LocalPath path);

    LocalPathLookup lookUp(in LocalPath path);
    RemoteInputStream lookupFilesStream(in LocalPath path);

    LocalPathLookupExtended lookUpExtended(in LocalPath path);
    List<LocalPathLookupExtended> lookupFilesExtended(in LocalPath path);
    RemoteInputStream lookupFilesExtendedStream(in LocalPath path);

    RemoteInputStream walkStream(in LocalPath path, in List<String> pathDoesNotContain, boolean followSymlinks);

    long du(in LocalPath path);

    boolean createSymlink(in LocalPath linkPath, in LocalPath targetPath);

    boolean setModifiedAt(in LocalPath path, in long modifiedAt);

    boolean setPermissions(in LocalPath path, in Permissions permissions);

    boolean setOwnership(in LocalPath path, in Ownership ownership);
}