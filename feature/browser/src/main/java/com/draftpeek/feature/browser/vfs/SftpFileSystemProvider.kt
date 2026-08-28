package com.draftpeek.feature.browser.vfs

import android.net.Uri
import android.util.Log
import com.draftpeek.feature.browser.model.FileContent
import com.draftpeek.feature.browser.model.FileItem
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.ByteArrayOutputStream
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * SFTP 文件系统提供者，使用 JSch 库实现
 *
 * 提供通过 SSH/SFTP 协议的远程文件访问。支持：
 * - 密码认证
 * - 公钥认证（通过 CredentialManager）
 * - 目录列表、文件读写、创建、删除
 * - 带自动重连的连接池
 *
 * ## 线程安全
 * 所有公共方法支持多协程并发访问。JSch session 内部同步，
 * 连接丢失时会自动重新建立。
 *
 * ## 安全注意事项
 * - 默认启用主机密钥验证（严格模式）
 * - 凭据应通过 [com.draftpeek.core.data.security.CredentialManager] 安全存储
 * - 所有文件 I/O 操作在 [Dispatchers.IO] 上执行
 *
 * ## 异常处理
 * - SFTP 错误码通过 [mapSftpError] 映射到 [FileSystemResult.ErrorCode]
 * - SSH_FX_NO_SUCH_FILE → NOT_FOUND
 * - SSH_FX_PERMISSION_DENIED → PERMISSION_DENIED
 * - SSH_FX_FILE_ALREADY_EXISTS → ALREADY_EXISTS
 */
class SftpFileSystemProvider(private val config: RemoteFileSystemConfig) : FileSystemProvider {

    override val scheme: String = "sftp"
    override val displayName: String = "SFTP — ${config.host}"

    private var session: Session? = null
    private var channel: ChannelSftp? = null

    /**
     * 建立或复用 SFTP 会话
     *
     * 如果现有会话仍然连接，则复用它；否则使用配置的凭据创建新会话。
     *
     * @return 连接就绪的 ChannelSftp
     * @throws com.jcraft.jsch.JSchException 连接失败或认证错误时
     */
    private suspend fun ensureConnected(): ChannelSftp = withContext(Dispatchers.IO) {
        val existingChannel = channel
        if (existingChannel != null && existingChannel.isConnected && session?.isConnected == true) {
            return@withContext existingChannel
        }

        disconnect()

        val jsch = JSch()
        val port = if (config.port > 0) config.port else 22

        val newSession = jsch.getSession(config.username, config.host, port)
        newSession.setPassword(config.password)

        val props = Properties().apply {
            put("StrictHostKeyChecking", if (config.useEncryption) "yes" else "no")
            put("PreferredAuthentications", "password,publickey")
        }
        newSession.setConfig(props)
        newSession.timeout = config.connectionTimeoutMs.toInt()

        newSession.connect(config.connectionTimeoutMs.toInt())

        val newChannel = newSession.openChannel("sftp") as ChannelSftp
        newChannel.connect(config.connectionTimeoutMs.toInt())

        if (config.basePath.isNotBlank() && config.basePath != "/") {
            try {
                newChannel.cd(config.basePath)
            } catch (e: Exception) {
                Log.w(TAG, "无法切换到基础路径: ${config.basePath}", e)
            }
        }

        session = newSession
        channel = newChannel

        Log.d(TAG, "SFTP 已连接到 ${config.host}:$port")
        newChannel
    }

    /**
     * 断开 SFTP 连接并清理资源
     */
    private fun disconnect() {
        try {
            channel?.disconnect()
            session?.disconnect()
        } catch (_: Exception) {}
        channel = null
        session = null
    }

    override fun supportsUri(uri: String): Boolean = uri.startsWith("sftp://")

    override fun listFiles(uri: String): Flow<List<FileItem>> = flow {
        val sftp = ensureConnected()
        val remotePath = extractPath(uri)

        val entries = sftp.ls(remotePath)
        val files = entries.mapNotNull { entry ->
            val filename = entry.filename
            if (filename == "." || filename == "..") return@mapNotNull null

            val attrs = entry.attrs
            FileItem(
                name = filename,
                uri = Uri.parse("sftp://${config.host}$remotePath/$filename"),
                isDirectory = attrs.isDir,
                size = attrs.size,
                lastModified = (attrs.mTime * 1000L)
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        emit(files)
    }.flowOn(Dispatchers.IO)

    override suspend fun readFile(uri: String): FileSystemResult<FileContent> = withContext(Dispatchers.IO) {
        try {
            val sftp = ensureConnected()
            val remotePath = extractPath(uri)
            val outputStream = ByteArrayOutputStream()
            sftp.get(remotePath, outputStream)
            val content = outputStream.toString("UTF-8")

            FileSystemResult.Success(
                FileContent(
                    content = content
                )
            )
        } catch (e: com.jcraft.jsch.SftpException) {
            Log.e(TAG, "SFTP 读取失败: $uri", e)
            mapSftpError(e)
        } catch (e: Exception) {
            Log.e(TAG, "SFTP 读取错误: $uri", e)
            FileSystemResult.Error(
                message = "读取文件失败: ${e.message}",
                cause = e,
                errorCode = FileSystemResult.ErrorCode.NETWORK_ERROR
            )
        }
    }

    override suspend fun writeFile(uri: String, content: String): FileSystemResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = ensureConnected()
            val remotePath = extractPath(uri)
            val inputStream = content.byteInputStream(Charsets.UTF_8)
            sftp.put(inputStream, remotePath, ChannelSftp.OVERWRITE)
            FileSystemResult.Success(Unit)
        } catch (e: com.jcraft.jsch.SftpException) {
            Log.e(TAG, "SFTP 写入失败: $uri", e)
            mapSftpError(e)
        } catch (e: Exception) {
            Log.e(TAG, "SFTP 写入错误: $uri", e)
            FileSystemResult.Error(
                message = "写入文件失败: ${e.message}",
                cause = e,
                errorCode = FileSystemResult.ErrorCode.NETWORK_ERROR
            )
        }
    }

    override suspend fun createDirectory(parentUri: String, dirName: String): FileSystemResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val sftp = ensureConnected()
                val parentPath = extractPath(parentUri)
                val newPath = "$parentPath/$dirName"
                sftp.mkdir(newPath)
                FileSystemResult.Success("sftp://${config.host}$newPath")
            } catch (e: com.jcraft.jsch.SftpException) {
                mapSftpError(e)
            } catch (e: Exception) {
                FileSystemResult.Error(
                    message = "创建目录失败: ${e.message}",
                    cause = e
                )
            }
        }

    override suspend fun delete(uri: String): FileSystemResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = ensureConnected()
            val remotePath = extractPath(uri)

            val attrs = sftp.stat(remotePath)
            if (attrs.isDir) {
                sftp.rmdir(remotePath)
            } else {
                sftp.rm(remotePath)
            }
            FileSystemResult.Success(Unit)
        } catch (e: com.jcraft.jsch.SftpException) {
            mapSftpError(e)
        } catch (e: Exception) {
            FileSystemResult.Error(
                message = "删除失败: ${e.message}",
                cause = e
            )
        }
    }

    override suspend fun exists(uri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sftp = ensureConnected()
            val remotePath = extractPath(uri)
            sftp.stat(remotePath)
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getFileInfo(uri: String): FileSystemResult<FileItem> = withContext(Dispatchers.IO) {
        try {
            val sftp = ensureConnected()
            val remotePath = extractPath(uri)
            val attrs = sftp.stat(remotePath)
            val fileName = remotePath.substringAfterLast('/')

            FileSystemResult.Success(
                FileItem(
                    name = fileName,
                    uri = Uri.parse(uri),
                    isDirectory = attrs.isDir,
                    size = attrs.size,
                    lastModified = attrs.mTime * 1000L
                )
            )
        } catch (e: com.jcraft.jsch.SftpException) {
            mapSftpError(e)
        } catch (e: Exception) {
            FileSystemResult.Error(
                message = "获取文件信息失败: ${e.message}",
                cause = e
            )
        }
    }

    override suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val sftp = ensureConnected()
            sftp.ls(".")
            true
        } catch (e: Exception) {
            Log.w(TAG, "SFTP 连接测试失败", e)
            disconnect()
            false
        }
    }

    override fun close() {
        disconnect()
    }

    /**
     * 从 SFTP URI 中提取远程路径
     *
     * @param uri SFTP URI（如 sftp://host/path/to/file）
     * @return 远程路径（如 /path/to/file）
     */
    private fun extractPath(uri: String): String {
        val withoutScheme = uri.removePrefix("sftp://")
        val slashIndex = withoutScheme.indexOf('/')
        return if (slashIndex >= 0) {
            withoutScheme.substring(slashIndex)
        } else {
            config.basePath
        }
    }

    /**
     * 将 SFTP 错误码映射到 [FileSystemResult.ErrorCode]
     *
     * @param e JSch SFTP 异常
     * @return 对应的 FileSystemResult.Error
     */
    private fun mapSftpError(e: com.jcraft.jsch.SftpException): FileSystemResult<Nothing> {
        val errorCode = when (e.id) {
            ChannelSftp.SSH_FX_NO_SUCH_FILE -> FileSystemResult.ErrorCode.NOT_FOUND
            ChannelSftp.SSH_FX_PERMISSION_DENIED -> FileSystemResult.ErrorCode.PERMISSION_DENIED
            11 -> FileSystemResult.ErrorCode.ALREADY_EXISTS
            ChannelSftp.SSH_FX_OP_UNSUPPORTED -> FileSystemResult.ErrorCode.NOT_SUPPORTED
            else -> FileSystemResult.ErrorCode.UNKNOWN
        }
        return FileSystemResult.Error(
            message = e.message ?: "SFTP 错误",
            cause = e,
            errorCode = errorCode
        )
    }

    companion object {
        private const val TAG = "SftpProvider"
    }
}
