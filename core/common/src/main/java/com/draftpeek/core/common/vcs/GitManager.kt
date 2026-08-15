/**
 * Git操作管理器模块。
 *
 * 本文件实现了基于JGit的纯Java Git操作管理器，提供仓库发现、克隆（支持认证）、
 * 状态查询、暂存、提交、分支管理、差异比较、变基、合并、拣选、Stash、远程管理等功能。
 * 所有操作均在后台线程执行，包含超时保护和异常处理，支持安全凭证存储。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.vcs

import android.content.Context
import android.util.Log
import com.draftpeek.core.common.R
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException as JGitApiTransportException
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.TransportHttp
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import java.net.UnknownHostException
import java.nio.file.NoSuchFileException
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLHandshakeException

private const val TAG = "GitManager"
/** 克隆操作默认超时时间（3分钟） */
private const val CLONE_TIMEOUT_MS = 3 * 60 * 1000L

/**
 * JGit进度监视器，将克隆/拉取进度消息转发到回调函数。
 *
 * 直接实现ProgressMonitor接口，将JGit内部任务进度转换为用户可读的消息。
 * 回调调用方负责通过OutputThrottler进行节流以避免UI flooding。
 *
 * @property onProgress 进度消息回调，在JGit工作线程上调用，调用方需自行切换到主线程
 */
private class CallbackProgressMonitor(
    private val onProgress: (String) -> Unit,
) : ProgressMonitor {
    private var currentTask: String = ""
    private var totalWork: Int = 0
    private var lastReportedPercent = -1

    override fun start(totalTasks: Int) {
        // Called at the beginning of the operation with total number of tasks
    }

    override fun beginTask(title: String?, totalWork: Int) {
        currentTask = title ?: ""
        this.totalWork = totalWork
        lastReportedPercent = -1
        if (totalWork == ProgressMonitor.UNKNOWN) {
            onProgress("$currentTask: starting...")
        } else {
            onProgress("$currentTask: 0/$totalWork (0%)")
        }
    }

    override fun update(completed: Int) {
        if (totalWork > 0) {
            val percent = (completed * 100 / totalWork).coerceIn(0, 100)
            if (percent != lastReportedPercent) {
                lastReportedPercent = percent
                onProgress("$currentTask: $completed/$totalWork ($percent%)")
            }
        }
    }

    override fun endTask() {
        onProgress("$currentTask: done")
    }

    override fun isCancelled(): Boolean = false

    override fun showDuration(enabled: Boolean) {
        // No-op; duration display not supported in our callback
    }
}

/**
 * Git文件状态数据类。
 *
 * @property filePath 文件相对路径
 * @property status 文件Git状态
 */
data class GitFileStatus(
    val filePath: String,
    val status: GitStatus,
)

/**
 * Git文件状态枚举。
 */
enum class GitStatus {
    /** 未跟踪的新文件 */
    UNTRACKED,
    /** 已修改的文件 */
    MODIFIED,
    /** 已添加到暂存区的文件 */
    ADDED,
    /** 已删除的文件 */
    DELETED,
    /** 未修改的文件 */
    UNMODIFIED,
    /** 存在冲突的文件 */
    CONFLICTED,
}

/**
 * Git仓库信息数据类。
 *
 * @property branchName 当前分支名称
 * @property remoteUrl 远程仓库URL，可为null
 * @property lastCommitMessage 最后一次提交消息，可为null
 * @property lastCommitDate 最后一次提交日期，可为null
 */
data class GitRepoInfo(
    val branchName: String,
    val remoteUrl: String?,
    val lastCommitMessage: String?,
    val lastCommitDate: Date?,
)

/**
 * 克隆操作结果密封类。
 */
sealed class CloneResult {
    /**
     * 克隆成功。
     *
     * @property directory 克隆到的本地目录
     */
    data class Success(val directory: File) : CloneResult()

    /**
     * 克隆失败。
     *
     * @property message 本地化错误消息
     */
    data class Error(val message: String) : CloneResult()
}

/**
 * 变基操作结果数据类。
 *
 * @property status 操作状态字符串（"OK"、"UP_TO_DATE"、"CONFLICTS"、"FAILED"、"ABORTED"等）
 * @property currentCommit 当前提交SHA，可为null
 * @property conflictedPaths 冲突文件路径列表
 * @property errorMessage 错误消息，可为null
 */
data class RebaseResult(
    val status: String,
    val currentCommit: String? = null,
    val conflictedPaths: List<String> = emptyList(),
    val errorMessage: String? = null,
) {
    /** 操作是否成功完成 */
    val isOk: Boolean get() = status == "OK" || status == "UP_TO_DATE"
    /** 是否存在冲突 */
    val hasConflicts: Boolean get() = conflictedPaths.isNotEmpty() || status == "CONFLICTS"
    /** 操作是否失败 */
    val isFailed: Boolean get() = status == "FAILED" || errorMessage != null
}

/**
 * 合并操作结果数据类。
 *
 * @property status 合并状态字符串
 * @property mergeCommitSha 合并提交SHA，可为null
 * @property isConflicting 是否存在冲突
 * @property conflictedPaths 冲突文件路径列表
 * @property errorMessage 错误消息，可为null
 */
data class MergeResult(
    val status: String,
    val mergeCommitSha: String? = null,
    val isConflicting: Boolean = false,
    val conflictedPaths: List<String> = emptyList(),
    val errorMessage: String? = null,
) {
    /** 合并是否成功 */
    val isOk: Boolean get() = status.startsWith("MERGED") || status == "FAST_FORWARD"
    /** 合并是否失败 */
    val isFailed: Boolean get() = status == "FAILED" || errorMessage != null
}

/**
 * Cherry-pick操作结果数据类。
 *
 * @property status 操作状态字符串
 * @property newCommitSha 新提交SHA，可为null
 * @property conflictedPaths 冲突文件路径列表
 * @property errorMessage 错误消息，可为null
 */
data class CherryPickResult(
    val status: String,
    val newCommitSha: String? = null,
    val conflictedPaths: List<String> = emptyList(),
    val errorMessage: String? = null,
) {
    /** 操作是否成功 */
    val isOk: Boolean get() = status == "OK" && errorMessage == null
    /** 操作是否失败 */
    val isFailed: Boolean get() = status == "FAILED" || errorMessage != null
}

/**
 * Git远程仓库信息数据类。
 *
 * @property name 远程名称（通常为"origin"）
 * @property url 远程拉取URL
 * @property pushUrl 远程推送URL，可为null
 */
data class RemoteInfo(
    val name: String,
    val url: String,
    val pushUrl: String? = null,
)

/**
 * Git操作管理器类。
 *
 * 使用JGit提供纯Java Git实现。提供只读查看功能（状态、差异、信息）和克隆操作。
 *
 * @property context 应用上下文，用于解析本地化错误字符串
 * @property credentialProvider 可选的安全凭证提供者，用于认证Git操作。
 *           提供后，clone/push/pull/fetch将自动使用远程主机存储的凭证。
 *           为null时仅支持公共（未认证）操作。
 */
class GitManager(
    private val context: Context,
    private val credentialProvider: CredentialProvider? = null,
) {

    /**
     * JGit HTTPS传输发送的User-Agent字符串。
     * 从安装包的versionName动态构建，自动与应用版本保持同步。
     * 某些Git托管服务会拒绝JGit默认UA的请求。
     */
    @Suppress("PrivatePropertyName")
    private val GIT_USER_AGENT: String by lazy {
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
        "DraftPeek/${versionName ?: "unknown"}"
    }

    companion object {
        private val threadNumber = AtomicInteger(1)

        private val gitThreadFactory = ThreadFactory { runnable ->
            val thread = Thread(runnable, "GitManager-${threadNumber.getAndIncrement()}")
            thread.isDaemon = true
            thread.priority = Thread.NORM_PRIORITY - 1
            thread
        }

        @Volatile
        private var executor: java.util.concurrent.ExecutorService? = null
        private val executorLock = Any()

        /**
         * 获取Git操作专用的单线程Executor。
         * 使用懒加载初始化，daemon线程确保应用退出时不会阻止JVM关闭。
         */
        private fun getExecutor(): java.util.concurrent.ExecutorService {
            return executor ?: synchronized(executorLock) {
                executor ?: Executors.newSingleThreadExecutor(gitThreadFactory).also {
                    executor = it
                }
            }
        }

        /**
         * 关闭Git线程池，释放资源。
         * 应在应用终止时调用（如Application.onTerminate()）。
         * 停止接受新任务，等待现有任务完成（最多5秒），然后强制关闭。
         */
        fun shutdown() {
            synchronized(executorLock) {
                val exec = executor ?: return
                executor = null
                exec.shutdown()
                try {
                    if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                        exec.shutdownNow()
                    }
                } catch (e: InterruptedException) {
                    exec.shutdownNow()
                    Thread.currentThread().interrupt()
                }
            }
        }
    }

    /**
     * 在给定目录路径或其上级目录中查找Git仓库。
     * 向上遍历父目录直到找到.git目录或到达文件系统根。
     *
     * @param directoryPath 起始目录路径
     * @return GitRepository包装对象，未找到Git仓库返回null
     */
    fun findRepository(directoryPath: String): GitRepository? {
        var dir: File? = File(directoryPath)
        while (dir != null) {
            val gitDir = File(dir, ".git")
            if (gitDir.exists()) {
                return try {
                    val repository = FileRepositoryBuilder()
                        .setGitDir(gitDir)
                        .readEnvironment()
                        .findGitDir()
                        .build()
                    GitRepository(Git(repository))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to open Git repository at ${gitDir.path}", e)
                    null
                }
            }
            dir = dir.parentFile
        }
        return null
    }

    /**
     * 克隆远程仓库，支持浅克隆和可选的稀疏检出。
     *
     * @param remoteUrl 远程仓库URL（如 https://github.com/owner/repo.git）
     * @param targetDirectory 克隆到的本地目录（必须不存在或为空）
     * @param depth 浅克隆深度（默认1，对应 --depth 1）
     * @param sparsePaths 稀疏检出的文件/目录路径列表，为null或空时执行完整浅克隆
     * @param timeoutMs 克隆超时时间（毫秒）
     * @param onProgress 可选的进度回调，在JGit工作线程上调用（接收进度文本）。
     *                   建议配合[com.draftpeek.core.common.util.OutputThrottler]使用以避免UI flooding。
     * @return CloneResult指示成功或失败
     */
    fun cloneRepository(
        remoteUrl: String,
        targetDirectory: File,
        depth: Int = 1,
        sparsePaths: List<String>? = null,
        timeoutMs: Long = CLONE_TIMEOUT_MS,
        onProgress: ((String) -> Unit)? = null,
    ): CloneResult {
        return executeClone(remoteUrl, targetDirectory, depth, sparsePaths, timeoutMs, null, onProgress)
    }

    /**
     * 使用安全存储的凭证克隆远程仓库。
     *
     * 这是[cloneRepository]的感知凭证版本。从[CredentialManager]查找远程主机的存储凭证，
     * 并相应配置JGit的[CredentialsProvider]。
     *
     * 支持：
     * - 基于令牌的认证（GitHub个人访问令牌）：username = "x-access-token"，password = token
     * - 基本认证：直接使用 username + password
     *
     * @param remoteUrl 远程仓库URL（如 https://github.com/owner/repo.git）
     * @param targetDirectory 克隆到的本地目录
     * @param depth 浅克隆深度（默认1）
     * @param sparsePaths 可选的稀疏检出路径
     * @param timeoutMs 克隆超时时间（毫秒）
     * @param onProgress 可选的进度回调，在JGit工作线程上调用
     * @return CloneResult指示成功或失败
     */
    suspend fun cloneRepositoryWithCredentials(
        remoteUrl: String,
        targetDirectory: File,
        depth: Int = 1,
        sparsePaths: List<String>? = null,
        timeoutMs: Long = CLONE_TIMEOUT_MS,
        onProgress: ((String) -> Unit)? = null,
    ): CloneResult {
        val credentialsProvider = resolveCredentials(remoteUrl)
        return executeClone(remoteUrl, targetDirectory, depth, sparsePaths, timeoutMs, credentialsProvider, onProgress)
    }

    /**
     * 执行克隆操作的内部辅助方法，包含目录准备、超时控制、错误处理和清理逻辑。
     *
     * @param remoteUrl 远程仓库URL
     * @param targetDirectory 目标本地目录
     * @param depth 浅克隆深度
     * @param sparsePaths 稀疏检出路径列表
     * @param timeoutMs 超时时间（毫秒）
     * @param credentialsProvider 可选的凭证提供者
     * @param onProgress 可选的进度回调
     * @return CloneResult指示成功或失败
     */
    private fun executeClone(
        remoteUrl: String,
        targetDirectory: File,
        depth: Int,
        sparsePaths: List<String>?,
        timeoutMs: Long,
        credentialsProvider: CredentialsProvider?,
        onProgress: ((String) -> Unit)? = null,
    ): CloneResult {
        return try {
            if (targetDirectory.exists() && File(targetDirectory, ".git").exists()) {
                return CloneResult.Error(
                    context.getString(R.string.error_git_repo_exists)
                )
            }

            if (targetDirectory.exists()) {
                targetDirectory.deleteRecursively()
            }

            targetDirectory.mkdirs()

            val progressMonitor = onProgress?.let { CallbackProgressMonitor(it) }

            try {
                val future = getExecutor().submit {
                    if (!sparsePaths.isNullOrEmpty()) {
                        cloneWithSparseCheckout(remoteUrl, targetDirectory, depth, sparsePaths, credentialsProvider, progressMonitor)
                    } else {
                        val cloneCommand = Git.cloneRepository()
                            .setURI(remoteUrl)
                            .setDirectory(targetDirectory)

                        if (depth > 0) {
                            cloneCommand.setDepth(depth)
                        }

                        credentialsProvider?.let { cloneCommand.setCredentialsProvider(it) }

                        progressMonitor?.let { cloneCommand.setProgressMonitor(it) }

                        cloneCommand.setTransportConfigCallback { transport ->
                            if (transport is TransportHttp) {
                                transport.setAdditionalHeaders(
                                    mapOf("User-Agent" to GIT_USER_AGENT)
                                )
                            }
                        }

                        val git = cloneCommand.call()
                        git.close()
                    }
                }

                future.get(timeoutMs, TimeUnit.MILLISECONDS)
                CloneResult.Success(targetDirectory)
            } catch (e: TimeoutException) {
                Log.w(TAG, "Clone timed out after ${timeoutMs}ms")
                CloneResult.Error(context.getString(R.string.git_error_timeout))
            } catch (e: java.util.concurrent.ExecutionException) {
                throw e.cause ?: e
            }
        } catch (e: Exception) {
            try {
                targetDirectory.deleteRecursively()
            } catch (cleanupException: Exception) {
                Log.w(TAG, "Failed to clean up partial clone directory", cleanupException)
            }
            CloneResult.Error(localizeCloneError(e))
        }
    }

    /**
     * 为远程Git URL解析存储的凭证。
     *
     * 从URL提取主机名，通过[CredentialManager]查找凭证。
     * 如果找到凭证则返回JGit [CredentialsProvider]，否则返回null用于未认证访问。
     *
     * 基于令牌的认证（GitHub推荐）使用约定：
     * - username = "x-access-token"（GitHub标准）
     * - password = 令牌值
     *
     * @param remoteUrl Git远程URL
     * @return [CredentialsProvider]，没有存储凭证返回null
     */
    private suspend fun resolveCredentials(remoteUrl: String): CredentialsProvider? {
        val manager = credentialProvider ?: return null

        val host = try {
            URI(remoteUrl).host
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse host from remote URL: $remoteUrl", e)
            return null
        }

        if (host.isBlank()) return null

        return try {
            val credential = manager.getCredential(host)
            if (credential == null) {
                Log.d(TAG, "No stored credentials for host: $host")
                return null
            }

            if (credential.hasToken) {
                Log.i(TAG, "Using token-based auth for host: $host")
                UsernamePasswordCredentialsProvider("x-access-token", credential.token)
            } else if (credential.hasBasicAuth) {
                Log.i(TAG, "Using basic auth for host: $host")
                UsernamePasswordCredentialsProvider(credential.username, credential.password)
            } else {
                null
            }
        } catch (e: CredentialProvider.CredentialLockedException) {
            Log.w(TAG, "Keystore key invalidated; cannot retrieve credentials for $host", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve credentials for host: $host", e)
            null
        }
    }

    /**
     * 使用稀疏检出克隆仓库的内部实现。
     *
     * @param remoteUrl 远程仓库URL
     * @param targetDirectory 目标目录
     * @param depth 浅克隆深度
     * @param sparsePaths 稀疏检出路径列表
     * @param credentialsProvider 可选的凭证提供者
     * @param progressMonitor 可选的进度监视器
     */
    private fun cloneWithSparseCheckout(
        remoteUrl: String,
        targetDirectory: File,
        depth: Int,
        sparsePaths: List<String>,
        credentialsProvider: CredentialsProvider?,
        progressMonitor: ProgressMonitor? = null,
    ) {
        val cloneCommand = Git.cloneRepository()
            .setURI(remoteUrl)
            .setDirectory(targetDirectory)
            .setNoCheckout(true)

        if (depth > 0) {
            cloneCommand.setDepth(depth)
        }

        credentialsProvider?.let { cloneCommand.setCredentialsProvider(it) }

        progressMonitor?.let { cloneCommand.setProgressMonitor(it) }

        cloneCommand.setTransportConfigCallback { transport ->
            if (transport is TransportHttp) {
                transport.setAdditionalHeaders(
                    mapOf("User-Agent" to GIT_USER_AGENT)
                )
            }
        }

        val git = cloneCommand.call()

        try {
            val config = git.repository.config
            config.setBoolean("core", null, "sparseCheckout", true)
            config.save()

            val infoDir = File(git.repository.directory, "info")
            infoDir.mkdirs()
            val sparseCheckoutFile = File(infoDir, "sparse-checkout")
            sparseCheckoutFile.writeText(sparsePaths.joinToString("\n"))

            git.checkout()
                .setName("HEAD")
                .call()
        } finally {
            git.close()
        }
    }

    /**
     * 将克隆失败异常映射为用户友好的本地化消息。
     *
     * JGit的原始异常消息（如"cannot open git-upload-pack"）仅为英文且会泄露传输内部细节。
     * 此分类器根据异常类型和消息返回稳定、可翻译的字符串。
     *
     * @param e 克隆失败异常
     * @return 本地化错误消息
     */
    private fun localizeCloneError(e: Exception): String {
        val message = e.message.orEmpty().lowercase()
        val unknown = context.getString(R.string.git_error_unknown)
        return when {
            e is UnknownHostException ->
                context.getString(R.string.git_error_network)
            e is TimeoutException ->
                context.getString(R.string.git_error_timeout)
            e is TransportException -> when {
                message.contains("upload pack") ->
                    context.getString(R.string.git_error_upload_pack)
                message.contains("not authorized") ||
                    message.contains("403") || message.contains("401") ->
                    context.getString(R.string.git_error_auth)
                message.contains("not found") ->
                    context.getString(R.string.git_error_not_found)
                else ->
                    context.getString(R.string.git_error_transport, e.localizedMessage ?: unknown)
            }
            e is InvalidRemoteException ->
                context.getString(R.string.git_error_not_found)
            e is JGitApiTransportException -> when {
                message.contains("upload pack") ->
                    context.getString(R.string.git_error_upload_pack)
                message.contains("not authorized") ||
                    message.contains("403") || message.contains("401") ->
                    context.getString(R.string.git_error_auth)
                else ->
                    context.getString(R.string.git_error_transport, e.localizedMessage ?: unknown)
            }
            e is SSLHandshakeException ->
                context.getString(R.string.git_error_ssl)
            e is FileNotFoundException || e is NoSuchFileException ->
                context.getString(R.string.git_error_path)
            else ->
                context.getString(R.string.git_error_generic, e.localizedMessage ?: unknown)
        }
    }
}

/**
 * Git仓库包装类。
 *
 * 封装JGit Git对象，提供仓库级别的操作方法。
 *
 * @property git JGit Git实例
 */
class GitRepository(private val git: Git) {

    /** 获取底层JGit Repository对象 */
    private val repository: Repository
        get() = git.repository

    /**
     * 获取当前分支名称。
     *
     * @return 当前分支名称，失败时返回"HEAD"
     */
    fun getCurrentBranch(): String {
        return try {
            repository.branch ?: Constants.HEAD
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get current branch", e)
            Constants.HEAD
        }
    }

    /**
     * 获取仓库中所有文件的状态。
     *
     * @return GitFileStatus列表，失败时返回空列表
     */
    fun getFileStatuses(): List<GitFileStatus> {
        return try {
            val status = git.status().call()
            buildList {
                status.untracked.forEach { add(GitFileStatus(it, GitStatus.UNTRACKED)) }
                status.untrackedFolders.forEach { add(GitFileStatus(it, GitStatus.UNTRACKED)) }
                status.modified.forEach { add(GitFileStatus(it, GitStatus.MODIFIED)) }
                status.added.forEach { add(GitFileStatus(it, GitStatus.ADDED)) }
                status.removed.forEach { add(GitFileStatus(it, GitStatus.DELETED)) }
                status.changed.forEach { add(GitFileStatus(it, GitStatus.MODIFIED)) }
                status.conflicting.forEach { add(GitFileStatus(it, GitStatus.CONFLICTED)) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get file statuses", e)
            emptyList()
        }
    }

    /**
     * 获取特定文件的状态。
     *
     * @param filePath 文件相对路径
     * @return GitFileStatus，文件未找到返回null
     */
    fun getFileStatus(filePath: String): GitFileStatus? {
        return getFileStatuses().find { it.filePath == filePath }
    }

    /**
     * 获取仓库基本信息。
     *
     * @return GitRepoInfo包含分支名、远程URL、最后提交信息
     */
    fun getRepoInfo(): GitRepoInfo {
        val branchName = getCurrentBranch()
        val remoteUrl = try {
            val config = repository.config
            val remote = config.getSubsections("remote").firstOrNull()
            remote?.let { config.getString("remote", it, "url") }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get remote URL", e)
            null
        }
        val lastCommit = try {
            val headId = repository.resolve(Constants.HEAD)
            if (headId != null) {
                RevWalk(repository).use { revWalk ->
                    revWalk.parseCommit(headId)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get last commit info", e)
            null
        }
        return GitRepoInfo(
            branchName = branchName,
            remoteUrl = remoteUrl,
            lastCommitMessage = lastCommit?.shortMessage,
            lastCommitDate = lastCommit?.let {
                Date(it.commitTime.toLong() * 1000L)
            }
        )
    }

    /**
     * 暂存文件并使用给定消息提交。
     *
     * @param message 提交消息
     * @param filePaths 要提交的文件路径列表
     */
    fun commit(message: String, filePaths: List<String>) {
        val addCommand = git.add()
        for (path in filePaths) {
            addCommand.addFilepattern(path)
        }
        addCommand.call()
        git.commit().setMessage(message).call()
    }

    /**
     * 暂存指定文件（git add）。
     *
     * @param filePaths 相对于仓库根的文件路径列表
     */
    fun stageFiles(filePaths: List<String>) {
        val addCommand = git.add()
        for (path in filePaths) {
            addCommand.addFilepattern(path)
        }
        addCommand.call()
    }

    /**
     * 取消暂存指定文件（git reset HEAD）。
     *
     * @param filePaths 相对于仓库根的文件路径列表
     */
    fun unstageFiles(filePaths: List<String>) {
        val resetCommand = git.reset()
        for (path in filePaths) {
            resetCommand.addPath(path)
        }
        resetCommand.call()
    }

    /**
     * 暂存所有更改（git add -A）。
     */
    fun stageAll() {
        git.add().addFilepattern(".").call()
    }

    /**
     * 丢弃已跟踪文件的所有未暂存更改（git checkout -- .）。
     * 警告：这是破坏性操作，无法撤销。
     */
    fun discardUnstagedChanges() {
        git.checkout().addPath(".").call()
    }

    /**
     * 从当前HEAD创建新分支。
     *
     * @param branchName 新分支名称
     * @param startPoint 可选起始点（提交SHA或分支名），默认为HEAD
     */
    fun createBranch(branchName: String, startPoint: String? = null) {
        val command = git.branchCreate().setName(branchName)
        startPoint?.let { command.setStartPoint(it) }
        command.call()
    }

    /**
     * 删除本地分支。
     *
     * @param branchName 要删除的分支名
     * @param force 是否强制删除（即使未完全合并）
     * @return 分支成功删除返回true
     */
    fun deleteBranch(branchName: String, force: Boolean = false): Boolean {
        return try {
            val command = git.branchDelete().setBranchNames(branchName)
            if (force) command.setForce(true)
            command.call().isNotEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete branch $branchName", e)
            false
        }
    }

    /**
     * 列出所有本地分支名称。
     *
     * @param includeRemote 是否包含远程跟踪分支
     * @return 分支名称列表
     */
    fun listAllBranches(includeRemote: Boolean = false): List<String> {
        return try {
            val command = git.branchList()
            if (includeRemote) command.setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL)
            command.call().map { it.name }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to list branches", e)
            emptyList()
        }
    }

    /**
     * 检出指定分支，可选地在分支不存在时创建。
     *
     * @param branchName 要检出的分支名
     * @param createBranch 如果分支不存在则创建，默认为false
     */
    fun checkoutBranch(branchName: String, createBranch: Boolean = false) {
        val command = git.checkout().setName(branchName)
        if (createBranch) command.setCreateBranch(true)
        command.call()
    }

    /**
     * 获取修改文件的差异（未暂存更改 vs 工作树）。
     *
     * @param filePath 相对于仓库根的文件路径
     * @return 统一差异字符串，没有更改返回null
     */
    fun getFileDiff(filePath: String): String? {
        return try {
            val pathFilter = PathFilter.create(filePath)
            val outputStream = ByteArrayOutputStream()

            DiffFormatter(outputStream).use { diffFormatter ->
                diffFormatter.setRepository(repository)
                diffFormatter.setPathFilter(pathFilter)

                val diffs = git.diff()
                    .setPathFilter(pathFilter)
                    .call()

                for (diffEntry in diffs) {
                    diffFormatter.format(diffEntry)
                }
                diffFormatter.flush()
            }

            val result = outputStream.toString("UTF-8")
            if (result.isBlank()) null else result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get file diff for $filePath", e)
            null
        }
    }

    /**
     * 获取特定文件已暂存更改的差异（索引 vs HEAD）。
     *
     * @param filePath 相对于仓库根的文件路径
     * @return 统一差异字符串，没有已暂存更改返回null
     */
    fun getStagedDiff(filePath: String): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            DiffFormatter(outputStream).use { diffFormatter ->
                diffFormatter.setRepository(repository)

                val diffs = git.diff()
                    .setCached(true)
                    .setPathFilter(PathFilter.create(filePath))
                    .call()

                for (diffEntry in diffs) {
                    diffFormatter.format(diffEntry)
                }
                diffFormatter.flush()
            }

            val result = outputStream.toString("UTF-8")
            if (result.isBlank()) null else result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get staged diff for $filePath", e)
            null
        }
    }

    /**
     * 获取仓库中所有未暂存更改的完整差异。
     *
     * @return 统一差异字符串，没有更改返回null
     */
    fun getFullDiff(): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            DiffFormatter(outputStream).use { diffFormatter ->
                diffFormatter.setRepository(repository)

                val diffs = git.diff().call()
                for (diffEntry in diffs) {
                    diffFormatter.format(diffEntry)
                }
                diffFormatter.flush()
            }

            val result = outputStream.toString("UTF-8")
            if (result.isBlank()) null else result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get full diff", e)
            null
        }
    }

    /**
     * 提交日志条目数据类。
     *
     * @property sha 完整提交SHA
     * @property shortSha 短SHA（7位）
     * @property message 提交消息
     * @property authorName 作者名称
     * @property authorDate 作者日期
     */
    data class CommitEntry(
        val sha: String,
        val shortSha: String,
        val message: String,
        val authorName: String,
        val authorDate: Date,
    )

    /**
     * 获取当前分支的提交日志。
     *
     * @param maxCount 返回的最大提交数（默认20）
     * @return 提交条目列表，最新的在前
     */
    fun getCommitLog(maxCount: Int = 20): List<CommitEntry> {
        return try {
            git.log()
                .setMaxCount(maxCount)
                .call()
                .map { commit ->
                    CommitEntry(
                        sha = commit.getId().name(),
                        shortSha = commit.getId().abbreviate(7).name(),
                        message = commit.shortMessage,
                        authorName = commit.authorIdent.name,
                        authorDate = commit.authorIdent.getWhen(),
                    )
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get commit log", e)
            emptyList()
        }
    }

    /**
     * 获取两个提交之间的差异。
     *
     * @param fromSha 起始提交SHA
     * @param toSha 结束提交SHA（默认为HEAD）
     * @return 统一差异字符串，失败返回null
     */
    fun getDiffBetweenCommits(fromSha: String, toSha: String = Constants.HEAD): String? {
        return try {
            val fromId = repository.resolve(fromSha)
            val toId = repository.resolve(toSha)
            if (fromId == null || toId == null) return null

            val outputStream = ByteArrayOutputStream()
            DiffFormatter(outputStream).use { diffFormatter ->
                diffFormatter.setRepository(repository)
                val oldTreeParser = org.eclipse.jgit.treewalk.CanonicalTreeParser()
                val newTreeParser = org.eclipse.jgit.treewalk.CanonicalTreeParser()
                repository.newObjectReader().use { reader ->
                    oldTreeParser.reset(reader, RevWalk(repository).use { rw ->
                        rw.parseCommit(fromId).tree
                    })
                    newTreeParser.reset(reader, RevWalk(repository).use { rw ->
                        rw.parseCommit(toId).tree
                    })
                }
                val diffs = git.diff()
                    .setOldTree(oldTreeParser)
                    .setNewTree(newTreeParser)
                    .call()
                for (diffEntry in diffs) {
                    diffFormatter.format(diffEntry)
                }
                diffFormatter.flush()
            }

            val result = outputStream.toString("UTF-8")
            if (result.isBlank()) null else result
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get diff between commits", e)
            null
        }
    }

    /**
     * 暂存当前工作目录更改（git stash）。
     *
     * @param message 可选的stash消息
     * @return stash成功返回true
     */
    fun stash(message: String? = null): Boolean {
        return try {
            val command = git.stashCreate()
            message?.let { command.setWorkingDirectoryMessage(it) }
            command.call()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stash changes", e)
            false
        }
    }

    /**
     * 弹出最近的stash，应用其更改并从stash列表移除。
     *
     * @return stash pop成功返回true
     */
    fun stashPop(): Boolean {
        return try {
            git.stashApply().setStashRef("stash@{0}").call()
            git.stashDrop().setStashRef(0).call()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pop stash", e)
            false
        }
    }

    /**
     * 列出所有stash。
     *
     * @return stash条目消息列表
     */
    fun listStashes(): List<String> {
        return try {
            val stashWalk = org.eclipse.jgit.revwalk.RevWalk(repository)
            val stashRefs = repository.refDatabase.getRefsByPrefix("refs/stash/")
            stashRefs.mapNotNull { ref ->
                try {
                    val commit = stashWalk.parseCommit(ref.objectId)
                    commit.shortMessage
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to list stashes", e)
            emptyList()
        }
    }

    /**
     * 仓库状态摘要数据类（带计数）。
     *
     * @property modified 已修改文件数
     * @property added 已添加文件数
     * @property deleted 已删除文件数
     * @property untracked 未跟踪文件数
     * @property conflicted 冲突文件数
     */
    data class StatusSummary(
        val modified: Int = 0,
        val added: Int = 0,
        val deleted: Int = 0,
        val untracked: Int = 0,
        val conflicted: Int = 0,
    ) {
        /** 更改总数 */
        val totalChanges: Int get() = modified + added + deleted + untracked + conflicted
        /** 是否有更改 */
        val hasChanges: Boolean get() = totalChanges > 0
    }

    /**
     * 获取仓库状态的基于计数的摘要。
     *
     * @return StatusSummary实例
     */
    fun getStatusSummary(): StatusSummary {
        return try {
            val status = git.status().call()
            StatusSummary(
                modified = status.modified.size + status.changed.size,
                added = status.added.size,
                deleted = status.removed.size,
                untracked = status.untracked.size + status.untrackedFolders.size,
                conflicted = status.conflicting.size,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get status summary", e)
            StatusSummary()
        }
    }

    /**
     * 推送到远程仓库。
     */
    fun push() {
        git.push().call()
    }

    /**
     * 从远程仓库拉取。
     */
    fun pull() {
        git.pull().call()
    }

    /**
     * 从远程仓库获取更新。
     */
    fun fetch() {
        git.fetch().call()
    }

    /**
     * 检出分支。
     *
     * @param branchName 分支名称
     */
    fun checkout(branchName: String) {
        git.checkout().setName(branchName).call()
    }

    /**
     * 列出所有本地分支。
     *
     * @return 分支名称列表
     */
    fun listBranches(): List<String> {
        return git.branchList().call().map { it.name }
    }

    /**
     * 将当前分支变基到[upstreamBranch]上。
     *
     * @param upstreamBranch 要变基到的分支（如 "main"、"origin/main"）
     * @return RebaseResult包含状态（OK、CONFLICTS、ABORTED等）
     */
    fun rebase(upstreamBranch: String): RebaseResult {
        return try {
            val result = git.rebase()
                .setUpstream(upstreamBranch)
                .call()
            Log.d(TAG, "Rebase onto $upstreamBranch: ${result.status}")
            RebaseResult(
                status = result.status.name,
                currentCommit = result.currentCommit?.name,
                conflictedPaths = result.conflicts?.toList() ?: emptyList(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Rebase failed", e)
            RebaseResult(
                status = "FAILED",
                errorMessage = e.message,
            )
        }
    }

    /**
     * 中止进行中的变基操作。
     *
     * @return 中止成功返回true
     */
    fun rebaseAbort(): Boolean {
        return try {
            git.rebase()
                .setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.ABORT)
                .call()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Rebase abort failed", e)
            false
        }
    }

    /**
     * 解决冲突后继续进行中的变基。
     *
     * @return RebaseResult包含最新状态
     */
    fun rebaseContinue(): RebaseResult {
        return try {
            val result = git.rebase()
                .setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.CONTINUE)
                .call()
            RebaseResult(
                status = result.status.name,
                currentCommit = result.currentCommit?.name,
                conflictedPaths = result.conflicts?.toList() ?: emptyList(),
            )
        } catch (e: Exception) {
            RebaseResult(status = "FAILED", errorMessage = e.message)
        }
    }

    /**
     * 将[branch]合并到当前分支。
     *
     * @param branch 要合并的分支名
     * @param fastForwardOnly 如果为true，仅允许快进合并（不创建合并提交）
     * @return MergeResult包含状态和合并提交SHA
     */
    fun merge(branch: String, fastForwardOnly: Boolean = false): MergeResult {
        return try {
            val mergeCommand = git.merge()
                .include(git.repository.resolve(branch))

            if (fastForwardOnly) {
                mergeCommand.setFastForward(
                    org.eclipse.jgit.api.MergeCommand.FastForwardMode.FF_ONLY
                )
            }

            val result = mergeCommand.call()
            Log.d(TAG, "Merge $branch: ${result.mergeStatus}")
            MergeResult(
                status = result.mergeStatus.name,
                mergeCommitSha = result.newHead?.name,
                isConflicting = result.mergeStatus.isSuccessful.not() &&
                    result.conflicts != null,
                conflictedPaths = result.conflicts?.keys?.toList() ?: emptyList(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Merge failed", e)
            MergeResult(status = "FAILED", errorMessage = e.message)
        }
    }

    /**
     * 将特定提交拣选（cherry-pick）到当前分支。
     *
     * @param commitSha 要拣选的提交SHA
     * @return CherryPickResult包含状态
     */
    fun cherryPick(commitSha: String): CherryPickResult {
        return try {
            val objectId = git.repository.resolve(commitSha)
                ?: throw IllegalArgumentException("Commit not found: $commitSha")

            val result = git.cherryPick()
                .include(objectId)
                .call()

            val cherryPickStatus = result.getStatus()
            Log.d(TAG, "Cherry-pick $commitSha: $cherryPickStatus")

            CherryPickResult(
                status = cherryPickStatus.name,
                newCommitSha = result.newHead?.name,
                conflictedPaths = emptyList(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Cherry-pick failed", e)
            CherryPickResult(status = "FAILED", errorMessage = e.message)
        }
    }

    /**
     * 列出所有远程仓库。
     *
     * @return RemoteInfo列表
     */
    fun listRemotes(): List<RemoteInfo> {
        return try {
            val config = git.repository.config
            val remoteSections = config.getSubsections("remote")
            remoteSections.map { name ->
                RemoteInfo(
                    name = name,
                    url = config.getString("remote", name, "url") ?: "",
                    pushUrl = config.getString("remote", name, "pushurl"),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "List remotes failed", e)
            emptyList()
        }
    }

    /**
     * 添加新的远程仓库。
     *
     * @param name 远程名称（如"origin"）
     * @param url 远程仓库URL
     * @return 添加成功返回true
     */
    fun addRemote(name: String, url: String): Boolean {
        return try {
            git.remoteAdd()
                .setName(name)
                .setUri(org.eclipse.jgit.transport.URIish(url))
                .call()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Add remote failed", e)
            false
        }
    }

    /**
     * 移除远程仓库。
     *
     * @param name 要移除的远程名称
     * @return 移除成功返回true
     */
    fun removeRemote(name: String): Boolean {
        return try {
            git.remoteRemove()
                .setRemoteName(name)
                .call()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Remove remote failed", e)
            false
        }
    }

    /**
     * 关闭仓库，释放所有资源。
     */
    fun close() {
        git.close()
    }
}
