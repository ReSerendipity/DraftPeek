package com.draftpeek.feature.browser.vfs

import com.draftpeek.feature.browser.model.FileContent
import com.draftpeek.feature.browser.model.FileItem
import kotlinx.coroutines.flow.Flow

/**
 * 文件系统提供者接口
 *
 * 为不同类型的文件系统（本地 SAF、FTP、SFTP 等）提供统一的抽象接口。
 * 借鉴 Squircle-CE 的多文件系统架构设计，无论底层存储后端如何，
 * 都提供一致的文件操作 API。每个实现内部负责连接管理、认证和协议细节。
 *
 * 线程安全：实现必须支持多协程并发访问，所有挂起函数应使用 [Dispatchers.IO]。
 */
interface FileSystemProvider {

    /** 唯一的方案标识符（如 "file"、"ftp"、"sftp"） */
    val scheme: String

    /** 人类可读的显示名称（如 "本地存储"、"FTP 服务器"） */
    val displayName: String

    /**
     * 检查此提供者是否能处理给定的 URI
     *
     * @param uri 要检查的 URI
     * @return 如果此提供者支持该 URI 的方案则返回 true
     */
    fun supportsUri(uri: String): Boolean

    /**
     * 列出指定 URI 目录下的文件
     *
     * @param uri 要列出的目录 URI
     * @return 发射目录文件列表的 Flow
     */
    fun listFiles(uri: String): Flow<List<FileItem>>

    /**
     * 读取文件的文本内容
     *
     * @param uri 要读取的文件 URI
     * @return FileSystemResult，包含内容或错误信息
     * @throws SecurityException 当没有读取权限时
     * @throws java.io.FileNotFoundException 当文件不存在时
     */
    suspend fun readFile(uri: String): FileSystemResult<FileContent>

    /**
     * 向文件写入内容
     *
     * @param uri 要写入的文件 URI
     * @param content 要写入的内容
     * @return FileSystemResult，指示成功或失败
     * @throws SecurityException 当没有写入权限时
     */
    suspend fun writeFile(uri: String, content: String): FileSystemResult<Unit>

    /**
     * 在指定 URI 创建目录
     *
     * @param parentUri 父目录 URI
     * @param dirName 新目录的名称
     * @return FileSystemResult，包含创建的目录 URI
     */
    suspend fun createDirectory(parentUri: String, dirName: String): FileSystemResult<String>

    /**
     * 删除文件或目录
     *
     * @param uri 要删除的文件/目录 URI
     * @return FileSystemResult，指示成功或失败
     */
    suspend fun delete(uri: String): FileSystemResult<Unit>

    /**
     * 检查文件或目录是否存在
     *
     * @param uri 要检查的 URI
     * @return 如果文件/目录存在则返回 true
     */
    suspend fun exists(uri: String): Boolean

    /**
     * 获取文件元数据（大小、最后修改时间等）
     *
     * @param uri 文件 URI
     * @return FileSystemResult，包含文件元数据或错误
     */
    suspend fun getFileInfo(uri: String): FileSystemResult<FileItem>

    /**
     * 测试与文件系统的连接性
     *
     * @return 如果文件系统可访问则返回 true
     */
    suspend fun testConnection(): Boolean

    /**
     * 释放此提供者持有的所有资源
     */
    fun close()
}

/**
 * 文件系统操作的密封结果类，遵循离线优先模式
 *
 * 强制调用者显式处理成功和错误两种情况。
 */
sealed class FileSystemResult<out T> {
    /**
     * 操作成功
     *
     * @property data 返回的数据
     */
    data class Success<T>(val data: T) : FileSystemResult<T>()

    /**
     * 操作失败
     *
     * @property message 错误消息
     * @property cause 原始异常（可选）
     * @property errorCode 错误代码
     */
    data class Error(val message: String, val cause: Throwable? = null, val errorCode: ErrorCode = ErrorCode.UNKNOWN) :
        FileSystemResult<Nothing>()

    /**
     * 文件系统操作错误码枚举
     */
    enum class ErrorCode {
        /** 文件或目录未找到 */
        NOT_FOUND,

        /** 认证失败（凭据错误、令牌过期） */
        AUTH_FAILED,

        /** 网络不可达或连接超时 */
        NETWORK_ERROR,

        /** 权限被拒绝（只读文件系统、权限不足） */
        PERMISSION_DENIED,

        /** 文件已存在（用于创建操作） */
        ALREADY_EXISTS,

        /** 此提供者不支持该操作 */
        NOT_SUPPORTED,

        /** 未知错误 */
        UNKNOWN
    }
}

/**
 * 远程文件系统连接配置
 *
 * @property host 主机地址
 * @property port 端口号，0 表示使用协议默认端口
 * @property username 用户名
 * @property password 密码（应通过 CredentialManager 安全存储）
 * @property basePath 基础路径，默认为 "/"
 * @property useEncryption 是否使用加密（FTP 的 TLS/SSL，SFTP 始终为 true）
 * @property connectionTimeoutMs 连接超时时间（毫秒）
 * @property readTimeoutMs 读取超时时间（毫秒）
 */
data class RemoteFileSystemConfig(
    val host: String,
    val port: Int = 0,
    val username: String = "",
    val password: String = "",
    val basePath: String = "/",
    val useEncryption: Boolean = true,
    val connectionTimeoutMs: Long = 10_000,
    val readTimeoutMs: Long = 30_000
)
