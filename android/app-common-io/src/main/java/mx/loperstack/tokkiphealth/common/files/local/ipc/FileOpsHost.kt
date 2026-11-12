package mx.loperstack.tokkiphealth.common.files.local.ipc

import mx.loperstack.tokkiphealth.common.coroutine.AppScope
import mx.loperstack.tokkiphealth.common.coroutine.DispatcherProvider
import mx.loperstack.tokkiphealth.common.debug.Bugs
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.ERROR
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.Ownership
import mx.loperstack.tokkiphealth.common.files.Permissions
import mx.loperstack.tokkiphealth.common.files.asFile
import mx.loperstack.tokkiphealth.common.files.core.local.createSymlink
import mx.loperstack.tokkiphealth.common.files.core.local.deleteRecursivelySafe
import mx.loperstack.tokkiphealth.common.files.core.local.listFiles2
import mx.loperstack.tokkiphealth.common.files.core.local.listFilesStreaming
import mx.loperstack.tokkiphealth.common.files.local.DirectLocalWalker
import mx.loperstack.tokkiphealth.common.files.local.LocalPath
import mx.loperstack.tokkiphealth.common.files.local.LocalPathLookup
import mx.loperstack.tokkiphealth.common.files.local.LocalPathLookupExtended
import mx.loperstack.tokkiphealth.common.files.local.performLookup
import mx.loperstack.tokkiphealth.common.files.local.performLookupExtended
import mx.loperstack.tokkiphealth.common.files.local.setOwnership
import mx.loperstack.tokkiphealth.common.files.local.setPermissions
import mx.loperstack.tokkiphealth.common.funnel.IPCFunnel
import mx.loperstack.tokkiphealth.common.ipc.IpcHostModule
import mx.loperstack.tokkiphealth.common.ipc.RemoteFileHandle
import mx.loperstack.tokkiphealth.common.ipc.RemoteInputStream
import mx.loperstack.tokkiphealth.common.ipc.fileHandle
import mx.loperstack.tokkiphealth.common.ipc.remoteFileHandle
import mx.loperstack.tokkiphealth.common.pkgs.pkgops.LibcoreTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject

/**
 * ROOT-side
 */
class FileOpsHost @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    private val libcoreTool: LibcoreTool,
    private val ipcFunnel: IPCFunnel,
) : FileOpsConnection.Stub(), IpcHostModule {

    private fun listFiles(path: LocalPath): List<LocalPath> = path.asFile().listFiles2().map { LocalPath.build(it) }

    override fun listFilesStream(path: LocalPath): RemoteInputStream = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "listFilesStream($path)..." }
        // Enumerate the directory lazily (NIO) and stream paths chunk-by-chunk, so a single huge
        // directory is never fully materialized on the memory-constrained privileged host.
        val paths = path.asFile().listFilesStreaming().map { LocalPath.build(it) }
        paths.toRemoteInputStream(appScope + dispatcherProvider.IO)
    } catch (e: Exception) {
        log(TAG, ERROR) { "listFilesStream(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun lookUp(path: LocalPath): LocalPathLookup = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "lookUp($path)..." }
        path.performLookup().also {
            if (Bugs.isTrace) log(TAG, VERBOSE) { "lookUp($path): $it" }
        }
    } catch (e: Exception) {
        log(TAG, ERROR) { "lookUp(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun lookupFilesStream(path: LocalPath): RemoteInputStream = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "lookupFilesStream($path)..." }
        // Enumerate the directory lazily (NIO) and stream lookups chunk-by-chunk, so neither the
        // directory listing nor the lookups are fully materialized on the memory-constrained host.
        // A concurrent multi-tool scan could otherwise pile up whole-directory payloads and OOM the
        // process, which the client then sees as a dead binder (ServiceConnectionLostException).
        val lookups = path.asFile().listFilesStreaming().map { LocalPath.build(it).performLookup() }
        lookups.toRemoteInputStream(appScope + dispatcherProvider.IO)
    } catch (e: Exception) {
        log(TAG, ERROR) { "lookupFiles(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun lookUpExtended(path: LocalPath): LocalPathLookupExtended = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "lookUpExtended($path)..." }
        path.performLookupExtended(ipcFunnel, libcoreTool).also {
            if (Bugs.isTrace) log(TAG, VERBOSE) { "lookUpExtended($path): $it" }
        }
    } catch (e: Exception) {
        log(TAG, ERROR) { "lookUpExtended(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun lookupFilesExtended(path: LocalPath): List<LocalPathLookupExtended> = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "lookupFilesExtended($path)..." }
        listFiles(path)
            .mapIndexed { index, item ->
                if (Bugs.isTrace) log(TAG, VERBOSE) { "Looking up extended $index: $item" }
                item.performLookupExtended(ipcFunnel, libcoreTool)
            }
            .also { if (Bugs.isTrace) log(TAG, VERBOSE) { "lookupFilesExtended($path) done: ${it.size} items" } }
    } catch (e: Exception) {
        log(TAG, ERROR) { "lookupFilesExtended(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun walkStream(
        path: LocalPath,
        pathDoesNotContain: List<String>,
        followSymlinks: Boolean,
    ): RemoteInputStream = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "walkStream($path, followSymlinks=$followSymlinks)..." }
        DirectLocalWalker(
            start = path,
            onFilter = { lookup ->
                pathDoesNotContain.none { lookup.path.contains(it) }
            },
            followSymlinks = followSymlinks,
        ).toRemoteInputStream(appScope + dispatcherProvider.IO)
    } catch (e: Exception) {
        log(TAG, ERROR) { "walkStream(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun lookupFilesExtendedStream(path: LocalPath): RemoteInputStream = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "lookupFilesExtendedStream($path)..." }
        // Lazy NIO enumeration + chunked streaming — see lookupFilesStream for the memory rationale.
        val lookups = path.asFile().listFilesStreaming()
            .map { LocalPath.build(it).performLookupExtended(ipcFunnel, libcoreTool) }
        lookups.toRemoteInputStream(appScope + dispatcherProvider.IO)
    } catch (e: Exception) {
        log(TAG, ERROR) { "lookupFilesExtendedStream(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun du(path: LocalPath): Long = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "du($path)..." }
        runBlocking { path.asFile().walkTopDown().map { it.length() }.sum() }
    } catch (e: Exception) {
        log(TAG, ERROR) { "exists(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun file(path: LocalPath, readWrite: Boolean): RemoteFileHandle = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "file($path, $readWrite)..." }
        path.asFile().fileHandle(readWrite).remoteFileHandle()
    } catch (e: Exception) {
        log(TAG, ERROR) { "file(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun mkdirs(path: LocalPath): Boolean = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "mkdirs($path)..." }
        path.asFile().mkdirs()
    } catch (e: Exception) {
        log(TAG, ERROR) { "mkdirs(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun createNewFile(path: LocalPath): Boolean = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "createNewFile($path)..." }
        val file = path.asFile()

        if (file.exists() && file.isDirectory) {
            throw IOException("Can't create file, path exists and is directory: $path")
        }

        file.parentFile?.let {
            if (!it.exists()) {
                if (!it.mkdirs()) {
                    log(TAG, WARN) { "Failed to create parents for $path" }
                }
            }
        }

        file.createNewFile()
    } catch (e: Exception) {
        log(TAG, ERROR) { "mkdirs(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun canRead(path: LocalPath): Boolean = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "canRead($path)..." }
        path.asFile().canRead()
    } catch (e: Exception) {
        log(TAG, ERROR) { "path(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun canWrite(path: LocalPath): Boolean = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "canWrite($path)..." }
        path.asFile().canWrite()
    } catch (e: Exception) {
        log(TAG, ERROR) { "canWrite(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun exists(path: LocalPath): Boolean = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "exists($path)..." }
        path.asFile().exists()
    } catch (e: Exception) {
        log(TAG, ERROR) { "exists(path=$path) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun delete(path: LocalPath, recursive: Boolean, dryRun: Boolean): Boolean = try {
        log(TAG, VERBOSE) { "delete($path,recursive=$recursive,dryRun=$dryRun)..." }
        val javaFile = path.asFile()

        var success = when {
            dryRun -> javaFile.canWrite()
            recursive -> javaFile.deleteRecursivelySafe()
            else -> javaFile.delete()
        }

        if (!success) {
            success = !javaFile.exists()
            if (success) log(TAG, WARN) { "Tried to delete file, but it's already gone: $path" }
        }

        if (!success) log(TAG, WARN) { "Failed to delete: $path (canWrite=${javaFile.canWrite()})" }

        success
    } catch (e: Exception) {
        log(TAG, ERROR) { "delete(path=$path,recursive=$recursive,dryRun=$dryRun) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun createSymlink(linkPath: LocalPath, targetPath: LocalPath): Boolean = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "createSymlink($linkPath,$targetPath)..." }
        linkPath.asFile().createSymlink(targetPath.asFile())
    } catch (e: Exception) {
        log(TAG, ERROR) { "createSymlink(linkPath=$linkPath, targetPath=$targetPath) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun setModifiedAt(path: LocalPath, modifiedAt: Long): Boolean = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "setModifiedAt($path,$modifiedAt)..." }
        path.asFile().setLastModified(modifiedAt)
    } catch (e: Exception) {
        log(TAG, ERROR) { "setModifiedAt(path=$path, modifiedAt=$modifiedAt) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun setPermissions(path: LocalPath, permissions: Permissions): Boolean = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "setPermissions($path,$permissions)..." }
        path.asFile().setPermissions(permissions)
    } catch (e: Exception) {
        log(TAG, ERROR) { "setModifiedAt(path=$path, permissions=$permissions) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    override fun setOwnership(path: LocalPath, ownership: Ownership): Boolean = try {
        if (Bugs.isTrace) log(TAG, VERBOSE) { "setPermissions($path,$ownership)..." }
        path.asFile().setOwnership(ownership)
    } catch (e: Exception) {
        log(TAG, ERROR) { "setModifiedAt(path=$path, ownership=$ownership) failed\n${e.asLog()}" }
        throw e.wrapToPropagate()
    }

    companion object {
        val TAG = logTag("FileOps", "Service", "Host", Bugs.processTag)
    }
}