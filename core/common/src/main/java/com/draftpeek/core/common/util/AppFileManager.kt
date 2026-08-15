/**
 * 应用内部文件管理模块。
 *
 * 负责在应用内部存储和外部存储中读写用户文件，提供文件创建、读取、写入、删除、列表查询等操作，
 * 并包含内部URI与文件的相互转换工具方法，含路径遍历安全防护。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.util

import android.content.Context
import java.io.File

/**
 * 应用核心文件管理工具对象。
 *
 * 提供用户文件的CRUD操作、内部URI验证与转换等功能，包含路径遍历攻击防护（..段拒绝、
 * 完整路径段匹配校验），防止恶意构造的URI访问应用沙箱外的文件。
 */
object AppFileManager {

    private const val USER_FILES_DIR = "user_files"

    /**
     * 获取用户文件存储目录。
     *
     * 目录位于应用内部存储的 `files/user_files/`，如不存在则自动创建。
     *
     * @param context 应用上下文
     * @return 用户文件目录File对象
     */
    fun getUserFilesDir(context: Context): File {
        val dir = File(context.filesDir, USER_FILES_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 在用户文件目录下创建新文件。
     *
     * @param context 应用上下文
     * @param filename 文件名（不含扩展名）
     * @param extension 文件扩展名（不含点）
     * @param initialContent 初始内容，默认为空字符串
     * @return 创建的（或已存在的）文件File对象
     * @throws java.io.IOException 如果文件创建失败
     */
    fun createUserFile(context: Context, filename: String, extension: String, initialContent: String = ""): File {
        val dir = getUserFilesDir(context)
        val file = File(dir, "$filename.$extension")
        if (!file.exists()) {
            file.createNewFile()
            if (initialContent.isNotEmpty()) {
                file.writeText(initialContent, Charsets.UTF_8)
            }
        }
        return file
    }

    /**
     * 读取用户文件内容（UTF-8编码）。
     *
     * @param file 要读取的文件
     * @return 文件内容字符串
     * @throws java.io.IOException 如果文件读取失败
     */
    fun readUserFile(file: File): String {
        return file.readText(Charsets.UTF_8)
    }

    /**
     * 写入内容到用户文件（UTF-8编码，覆盖原有内容）。
     *
     * @param file 要写入的文件
     * @param content 要写入的内容
     * @throws java.io.IOException 如果文件写入失败
     */
    fun writeUserFile(file: File, content: String) {
        file.writeText(content, Charsets.UTF_8)
    }

    /**
     * 删除用户文件。
     *
     * @param file 要删除的文件
     * @return 删除成功返回 `true`，失败返回 `false`
     */
    fun deleteUserFile(file: File): Boolean {
        return file.delete()
    }

    /**
     * 列出用户文件目录下的所有文件和子目录。
     *
     * 结果按最后修改时间降序排列（最新修改的在前）。
     *
     * @param context 应用上下文
     * @return 文件列表，按修改时间降序排列
     */
    fun listUserFiles(context: Context): List<File> {
        val dir = getUserFilesDir(context)
        return dir.listFiles()?.filter { it.isFile || it.isDirectory }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * 验证URI是否为应用内部安全URI（指向user_files目录）。
     *
     * 安全校验包含：
     * 1. 必须以file://开头
     * 2. 预先拒绝包含..段的路径（防止canonicalPath在路径不存在时无法解析符号链接）
     * 3. 使用canonicalPath解析符号链接
     * 4. 校验路径段完整性（必须前接/后接/或处于路径边界，而非子串匹配）
     *
     * @param uri 待验证的URI字符串
     * @return 如果是安全的内部URI返回 `true`
     */
    fun isInternalUri(uri: String): Boolean {
        if (!uri.startsWith("file://")) return false
        val path = uri.removePrefix("file://")
        if (path.contains("..")) return false
        return try {
            val canonicalPath = File(path).canonicalPath
            isPathSegment(canonicalPath, USER_FILES_DIR) || isPathSegment(path, USER_FILES_DIR)
        } catch (e: Exception) {
            // canonicalPath 可能因设备 SELinux 策略或文件系统限制而失败，
            // 回退到检查原始路径（已通过 .. 段拒绝和 scheme 校验）
            isPathSegment(path, USER_FILES_DIR)
        }
    }

    /**
     * 校验segment是否为path中的完整路径段，而非其他段名的子串。
     *
     * 例：path="/data/user_files/foo" segment="user_files" → true
     *     path="/data/attacker_user_files/foo" segment="user_files" → false
     *
     * @param path 规范化后的绝对路径
     * @param segment 要检查的路径段名称
     * @return 如果segment是path中的完整路径段返回 `true`
     */
    private fun isPathSegment(path: String, segment: String): Boolean {
        // marker 包含前导分隔符（如 "/user_files"），因此匹配位置已经是分隔符边界，
        // 只需验证后缀是否为完整段（后接分隔符或处于路径末尾）。
        val marker = File.separator + segment
        val idx = path.indexOf(marker)
        if (idx < 0) return false
        val endIdx = idx + marker.length
        val isAtEnd = endIdx == path.length
        val isFollowedBySep = endIdx < path.length && path[endIdx] == File.separatorChar
        return isAtEnd || isFollowedBySep
    }

    /**
     * 从内部安全URI获取对应的File对象。
     *
     * @param context 应用上下文
     * @param uri 内部文件URI
     * @return 对应的File对象；URI无效或文件不存在时返回 `null`
     */
    fun getInternalFileFromUri(context: Context, uri: String): File? {
        if (!isInternalUri(uri)) return null
        return try {
            val path = uri.removePrefix("file://")
            val file = File(path).canonicalFile
            val userFilesDir = getUserFilesDir(context).canonicalPath
            if (file.absolutePath.startsWith(userFilesDir) && file.exists() && file.isFile) file else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 在用户文件目录下创建新文件夹。
     *
     * @param context 应用上下文
     * @param folderName 文件夹名称
     * @return 创建的文件夹File对象；名称已存在时返回已存在的文件夹
     */
    fun createUserFolder(context: Context, folderName: String): File {
        val dir = getUserFilesDir(context)
        val folder = File(dir, folderName)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    /**
     * 将内部文件移动到指定目录。
     *
     * @param context 应用上下文
     * @param sourceUri 源文件内部URI
     * @param targetDirUri 目标目录内部URI
     * @return 移动成功返回true，失败返回false
     */
    fun moveInternalFile(context: Context, sourceUri: String, targetDirUri: String): Boolean {
        if (!isInternalUri(sourceUri) || !isInternalUri(targetDirUri)) return false
        return try {
            val sourceFile = getInternalFileFromUri(context, sourceUri) ?: return false
            val targetDir = getInternalDirectoryFromUri(context, targetDirUri) ?: return false
            if (!targetDir.isDirectory) return false
            val destFile = File(targetDir, sourceFile.name)
            if (destFile.exists()) return false
            sourceFile.renameTo(destFile)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从内部安全URI获取对应的目录File对象。
     *
     * @param context 应用上下文
     * @param uri 内部目录URI
     * @return 对应的File对象；URI无效或目录不存在时返回 null
     */
    fun getInternalDirectoryFromUri(context: Context, uri: String): File? {
        if (!isInternalUri(uri)) return null
        return try {
            val path = uri.removePrefix("file://")
            val file = File(path).canonicalFile
            val userFilesDir = getUserFilesDir(context).canonicalPath
            if (file.absolutePath.startsWith(userFilesDir) && file.exists() && file.isDirectory) file else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 列出用户文件目录下的所有子目录。
     *
     * @param context 应用上下文
     * @return 子目录列表
     */
    fun listUserFolders(context: Context): List<File> {
        val dir = getUserFilesDir(context)
        return dir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    /**
     * 将File对象转换为内部URI字符串。
     *
     * @param file 要转换的文件
     * @return file://开头的绝对路径URI
     */
    fun fileToInternalUri(file: File): String {
        return "file://${file.absolutePath}"
    }
}
