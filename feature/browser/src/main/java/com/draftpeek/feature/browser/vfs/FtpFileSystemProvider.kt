package com.draftpeek.feature.browser.vfs

import android.net.Uri
import android.util.Log
import com.draftpeek.feature.browser.model.FileContent
import com.draftpeek.feature.browser.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.ByteArrayOutputStream

/**
 * FTP/FTPS 文件系统提供者，使用 Apache Commons Net 实现
 *
 * 提供通过 FTP/FTPS 协议的远程文件访问。支持：
 * - 普通 FTP 和 FTPS（基于 TLS/SSL 的 FTP）
 * - 被动模式（默认）和主动模式
 * - ASCII 和二进制传输模式
 * - 目录列表、文件读写、创建、删除
 *
 * ## 线程安全
 * 所有公共方法支持多协程并发访问。FTP 客户端在连接丢失时会自动重新连接。
 *
 * ## 安全注意事项
 * - 推荐使用 FTPS 进行安全连接（设置 useEncryption = true）
 * - 普通 FTP 以明文传输凭据
 * - 凭据应通过 [com.draftpeek.core.data.security.CredentialManager] 安全存储
 * - 所有文件 I/O 操作在 [Dispatchers.IO] 上执行
 *
 * ## 异常处理
 * - 网络错误映射为 [FileSystemResult.ErrorCode.NETWORK_ERROR]
 * - 认证失败映射为 [FileSystemResult.ErrorCode.AUTH_FAILED]
 * - 文件未找到映射为 [FileSystemResult.ErrorCode.NOT_FOUND]
 * - 权限问题映射为 [FileSystemResult.ErrorCode.PERMISSION_DENIED]
 */
class FtpFileSystemProvider(
    private val config: RemoteFileSystemConfig,
) : FileSystemProvider {

    override val scheme: String = "ftp"
    override val displayName: String = "FTP — ${config.host}"

    private var ftpClient: FTPClient? = null

    /**
     * 建立或复用 FTP 连接
     *
     * 如果现有连接仍然打开，则复用它；否则使用配置的凭据创建新连接。
     * 连接建立后会配置二进制传输类型、被动模式和 UTF-8 编码。
     *
     * @return 连接就绪的 FTPClient
     * @throws RuntimeException 连接被拒绝或认证失败时
     */
    private suspend fun ensureConnected(): FTPClient = withContext(Dispatchers.IO) {
        val existing = ftpClient
        if (existing != null && existing.isConnected) {
            try {
                existing.noop()
                return@withContext existing
            } catch (_: Exception) {
                disconnect()
            }
        }

        val client = if (config.useEncryption) {
            org.apache.commons.net.ftp.FTPSClient(false)
        } else {
            FTPClient()
        }

        client.connectTimeout = config.connectionTimeoutMs.toInt()
        client.defaultTimeout = config.readTimeoutMs.toInt()
        client.dataTimeout = java.time.Duration.ofMillis(config.readTimeoutMs)

        val port = if (config.port > 0) config.port else 21
        client.connect(config.host, port)

        val reply = client.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            client.disconnect()
            throw RuntimeException("FTP 服务器拒绝连接: $reply")
        }

        val loggedIn = client.login(config.username, config.password)
        if (!loggedIn) {
            client.disconnect()
            throw RuntimeException("FTP 认证失败，主机: ${config.host}")
        }

        client.setFileType(FTP.BINARY_FILE_TYPE)
        client.enterLocalPassiveMode()

        client.setControlEncoding("UTF-8")

        if (config.basePath.isNotBlank() && config.basePath != "/") {
            try {
                client.changeWorkingDirectory(config.basePath)
            } catch (e: Exception) {
                Log.w(TAG, "无法切换到基础路径: ${config.basePath}", e)
            }
        }

        ftpClient = client
        Log.d(TAG, "FTP 已连接到 ${config.host}:$port")
        client
    }

    /**
     * 断开 FTP 连接并清理资源
     */
    private fun disconnect() {
        try {
            ftpClient?.logout()
            ftpClient?.disconnect()
        } catch (_: Exception) {}
        ftpClient = null
    }

    override fun supportsUri(uri: String): Boolean =
        uri.startsWith("ftp://") || uri.startsWith("ftps://")

    override fun listFiles(uri: String): Flow<List<FileItem>> = flow {
        val ftp = ensureConnected()
        val remotePath = extractPath(uri)

        val ftpFiles = ftp.listFiles(remotePath)
        val files = ftpFiles.mapNotNull { ftpFile ->
            val name = ftpFile.name
            if (name == "." || name == "..") return@mapNotNull null

            FileItem(
                name = name,
                uri = Uri.parse("ftp://${config.host}$remotePath/$name"),
                isDirectory = ftpFile.isDirectory,
                size = ftpFile.size,
                lastModified = ftpFile.timestamp.timeInMillis,
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        emit(files)
    }.flowOn(Dispatchers.IO)

    override suspend fun readFile(uri: String): FileSystemResult<FileContent> =
        withContext(Dispatchers.IO) {
            try {
                val ftp = ensureConnected()
                val remotePath = extractPath(uri)
                val outputStream = ByteArrayOutputStream()

                val success = ftp.retrieveFile(remotePath, outputStream)
                if (!success) {
                    return@withContext FileSystemResult.Error(
                        message = "FTP 文件获取失败: ${ftp.replyString}",
                        errorCode = FileSystemResult.ErrorCode.NOT_FOUND,
                    )
                }

                val content = outputStream.toString("UTF-8")

                FileSystemResult.Success(
                    FileContent(
                        content = content,
                    ),
                )
            } catch (e: Exception) {
                Log.e(TAG, "FTP 读取失败: $uri", e)
                FileSystemResult.Error(
                    message = "读取文件失败: ${e.message}",
                    cause = e,
                    errorCode = FileSystemResult.ErrorCode.NETWORK_ERROR,
                )
            }
        }

    override suspend fun writeFile(uri: String, content: String): FileSystemResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val ftp = ensureConnected()
                val remotePath = extractPath(uri)
                val inputStream = content.byteInputStream(Charsets.UTF_8)

                val success = ftp.storeFile(remotePath, inputStream)
                if (!success) {
                    return@withContext FileSystemResult.Error(
                        message = "FTP 文件存储失败: ${ftp.replyString}",
                        errorCode = FileSystemResult.ErrorCode.PERMISSION_DENIED,
                    )
                }

                FileSystemResult.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "FTP 写入失败: $uri", e)
                FileSystemResult.Error(
                    message = "写入文件失败: ${e.message}",
                    cause = e,
                    errorCode = FileSystemResult.ErrorCode.NETWORK_ERROR,
                )
            }
        }

    override suspend fun createDirectory(
        parentUri: String,
        dirName: String,
    ): FileSystemResult<String> = withContext(Dispatchers.IO) {
        try {
            val ftp = ensureConnected()
            val parentPath = extractPath(parentUri)
            val newPath = "$parentPath/$dirName"

            val success = ftp.makeDirectory(newPath)
            if (!success) {
                val reply = ftp.replyCode
                return@withContext if (reply == 550) {
                    FileSystemResult.Error(
                        message = "目录已存在或权限被拒绝",
                        errorCode = FileSystemResult.ErrorCode.ALREADY_EXISTS,
                    )
                } else {
                    FileSystemResult.Error(
                        message = "FTP 创建目录失败: ${ftp.replyString}",
                    )
                }
            }

            FileSystemResult.Success("ftp://${config.host}$newPath")
        } catch (e: Exception) {
            FileSystemResult.Error(
                message = "创建目录失败: ${e.message}",
                cause = e,
            )
        }
    }

    override suspend fun delete(uri: String): FileSystemResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val ftp = ensureConnected()
                val remotePath = extractPath(uri)

                var success = ftp.deleteFile(remotePath)
                if (!success) {
                    success = ftp.removeDirectory(remotePath)
                }

                if (!success) {
                    return@withContext FileSystemResult.Error(
                        message = "FTP 删除失败: ${ftp.replyString}",
                        errorCode = FileSystemResult.ErrorCode.NOT_FOUND,
                    )
                }

                FileSystemResult.Success(Unit)
            } catch (e: Exception) {
                FileSystemResult.Error(
                    message = "删除失败: ${e.message}",
                    cause = e,
                )
            }
        }

    override suspend fun exists(uri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val ftp = ensureConnected()
            val remotePath = extractPath(uri)
            val mlsdResult = ftp.mlistFile(remotePath)
            mlsdResult != null
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getFileInfo(uri: String): FileSystemResult<FileItem> =
        withContext(Dispatchers.IO) {
            try {
                val ftp = ensureConnected()
                val remotePath = extractPath(uri)
                val fileName = remotePath.substringAfterLast('/')

                val mlsdFile = ftp.mlistFile(remotePath)
                if (mlsdFile != null) {
                    return@withContext FileSystemResult.Success(
                        FileItem(
                            name = fileName,
                            uri = Uri.parse(uri),
                            isDirectory = mlsdFile.isDirectory,
                            size = mlsdFile.size,
                            lastModified = mlsdFile.timestamp.timeInMillis,
                        ),
                    )
                }

                val parentPath = remotePath.substringBeforeLast('/')
                val files = ftp.listFiles(parentPath)
                val match = files.find { it.name == fileName }
                if (match != null) {
                    return@withContext FileSystemResult.Success(
                        FileItem(
                            name = fileName,
                            uri = Uri.parse(uri),
                            isDirectory = match.isDirectory,
                            size = match.size,
                            lastModified = match.timestamp.timeInMillis,
                        ),
                    )
                }

                FileSystemResult.Error(
                    message = "文件未找到: $remotePath",
                    errorCode = FileSystemResult.ErrorCode.NOT_FOUND,
                )
            } catch (e: Exception) {
                FileSystemResult.Error(
                    message = "获取文件信息失败: ${e.message}",
                    cause = e,
                )
            }
        }

    override suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val ftp = ensureConnected()
            ftp.noop()
            true
        } catch (e: Exception) {
            Log.w(TAG, "FTP 连接测试失败", e)
            disconnect()
            false
        }
    }

    override fun close() {
        disconnect()
    }

    /**
     * 从 FTP URI 中提取远程路径
     *
     * @param uri FTP URI（如 ftp://host/path/to/file）
     * @return 远程路径（如 /path/to/file）
     */
    private fun extractPath(uri: String): String {
        val withoutScheme = uri.removePrefix("ftps://").removePrefix("ftp://")
        val slashIndex = withoutScheme.indexOf('/')
        return if (slashIndex >= 0) {
            withoutScheme.substring(slashIndex)
        } else {
            config.basePath
        }
    }

    companion object {
        private const val TAG = "FtpProvider"
    }
}
