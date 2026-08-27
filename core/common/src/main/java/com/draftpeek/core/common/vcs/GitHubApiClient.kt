/**
 * GitHub REST API客户端模块。
 *
 * 本文件实现了GitHub公共仓库API访问客户端，支持未认证访问（60次/小时速率限制）。
 * 包含指数退避重试、速率限制检测、URL编码防注入、路径遍历防护等安全和可靠性优化。
 * 使用OkHttp进行HTTP通信，支持共享连接池和可配置超时。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.common.vcs

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val TAG = "GitHubApiClient"

/** 最大重试次数 */
private const val MAX_RETRIES = 3
/** 初始重试延迟（毫秒） */
private const val INITIAL_RETRY_DELAY_MS = 1000L
/** 最大重试延迟（毫秒） */
private const val MAX_RETRY_DELAY_MS = 8_000L

/** 连接超时（秒） */
private const val CONNECT_TIMEOUT_S = 15L
/** 读取超时（秒） */
private const val READ_TIMEOUT_S = 15L
/** 调用总超时（秒） */
private const val CALL_TIMEOUT_S = 30L
/** Accept请求头名称 */
private const val HEADER_ACCEPT = "Accept"
/** GitHub API v3 Accept头值 */
private const val ACCEPT_VALUE = "application/vnd.github.v3+json"
/** 速率限制剩余次数响应头 */
private const val HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining"
/** 速率限制重置时间响应头 */
private const val HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset"

/** GitHub API基础URL */
private const val API_BASE = "https://api.github.com"
/** 默认分支名称 */
private const val DEFAULT_BRANCH = "main"
/** 备用分支名称（传统默认） */
private const val FALLBACK_BRANCH = "master"

/** 网络错误用户提示消息 */
private const val MSG_NETWORK_ERROR = "网络错误，请检查网络连接后重试"
/** 解析错误用户提示消息 */
private const val MSG_PARSE_ERROR = "无法解析仓库数据，请稍后重试"
/** 空响应用户提示消息 */
private const val MSG_EMPTY_BODY = "服务器返回空响应"

/**
 * GitHub仓库树中的文件条目数据类。
 *
 * @property path 文件相对路径
 * @property type 条目类型（"blob"表示文件，"tree"表示目录）
 */
data class GitHubFileEntry(
    val path: String,
    val type: String,
)

/**
 * 获取GitHub仓库文件树结果的密封类。
 */
sealed class GitHubTreeResult {
    /**
     * 获取成功结果。
     *
     * @property files 文件条目列表
     * @property truncated 结果是否被截断（仓库过大时GitHub会截断响应）
     */
    data class Success(val files: List<GitHubFileEntry>, val truncated: Boolean = false) : GitHubTreeResult()

    /**
     * 获取失败结果。
     *
     * @property message 用户可读的错误消息
     * @property isRateLimited 是否因速率限制而失败
     */
    data class Error(val message: String, val isRateLimited: Boolean = false) : GitHubTreeResult()
}

/**
 * GitHub REST API客户端类（仅支持公共仓库）。
 *
 * 使用未认证访问（60次/小时速率限制）。
 *
 * 优化要点：
 * - 共享OkHttp客户端（连接池复用）
 * - 针对瞬时故障的带抖动指数退避重试
 * - 总调用超时防止慢响应挂起
 * - ResponseBody通过use{}自动释放
 * - URL路径段编码防止注入
 * - URL解析中阻止路径遍历（..）
 * - 通过X-RateLimit头检测速率限制
 * - 受控错误消息，不泄露内部细节
 * - 非IO异常不重试（解析错误为终止性错误）
 * - 所有魔法数字提取为命名常量
 * - 响应处理拆分为专注方法
 * - 每个方法单一职责
 * - OkHttpClient可注入以支持测试
 *
 * @property client 用于HTTP请求的OkHttpClient实例
 */
class GitHubApiClient constructor(
    private val client: OkHttpClient,
) {

    /**
     * 解析GitHub URL以提取所有者和仓库名称。
     * 使用白名单正则——所有者/仓库名仅允许字母数字、点、连字符。
     * 阻止路径遍历（..）尝试。
     *
     * @param url 要解析的GitHub仓库URL
     * @return (owner, repo)对，无效URL返回null
     */
    fun parseGitHubUrl(url: String): Pair<String, String>? {
        val regex = Regex("^https://github\\.com/([\\w.-]+)/([\\w.-]+?)(?:\\.git)?/?$")
        val match = regex.find(url.trim()) ?: return null
        val owner = match.groupValues[1]
        val repo = match.groupValues[2]
        if (owner.isBlank() || repo.isBlank()) return null
        if (owner.contains("..") || repo.contains("..")) return null
        return owner to repo
    }

    /**
     * 获取公共GitHub仓库的文件树。
     * 如果默认分支"main"未找到，则回退到"master"分支。
     *
     * 对瞬时故障（5xx、IO错误）使用带抖动的指数退避重试。
     * OkHttp客户端强制执行总调用超时。
     *
     * @param owner 仓库所有者（用户名或组织名）
     * @param repo 仓库名称
     * @param branch 分支名称，默认为"main"
     * @return 文件树获取结果
     */
    suspend fun fetchFileTree(owner: String, repo: String, branch: String = DEFAULT_BRANCH): GitHubTreeResult {
        val result = fetchFileTreeForBranch(owner, repo, branch)
        if (result is GitHubTreeResult.Error && branch == DEFAULT_BRANCH) {
            return fetchFileTreeForBranch(owner, repo, FALLBACK_BRANCH)
        }
        return result
    }

    /**
     * 获取指定分支的文件树内部实现。
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param branch 分支名称
     * @return 文件树获取结果
     */
    private suspend fun fetchFileTreeForBranch(owner: String, repo: String, branch: String): GitHubTreeResult =
        withContext(Dispatchers.IO) {
            val apiUrl = buildApiUrl(owner, repo, branch)
            var lastException: IOException? = null

            for (attempt in 1..MAX_RETRIES) {
                val request = Request.Builder()
                    .url(apiUrl)
                    .header(HEADER_ACCEPT, ACCEPT_VALUE)
                    .build()

                try {
                    val outcome = client.newCall(request).execute().use { response ->
                        handleResponse(response)
                    }
                    when (outcome) {
                        is FetchOutcome.Done -> return@withContext outcome.result
                        is FetchOutcome.Retry -> {
                            lastException = outcome.cause
                            Log.w(TAG, "GitHub API transient failure (attempt $attempt/$MAX_RETRIES)" +
                                (outcome.detail?.let { ": $it" } ?: ""))
                        }
                    }
                } catch (e: IOException) {
                    lastException = e
                    Log.w(TAG, "GitHub API request failed (attempt $attempt/$MAX_RETRIES): ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error during GitHub API call", e)
                    return@withContext GitHubTreeResult.Error(MSG_PARSE_ERROR)
                }

                if (attempt < MAX_RETRIES) {
                    delay(computeBackoff(attempt))
                }
            }

            GitHubTreeResult.Error(MSG_NETWORK_ERROR)
        }

    /**
     * HTTP请求获取结果的内部密封类。
     */
    private sealed class FetchOutcome {
        /**
         * 请求完成（成功或终止性错误）。
         *
         * @property result 最终结果
         */
        data class Done(val result: GitHubTreeResult) : FetchOutcome()

        /**
         * 可重试的瞬时故障。
         *
         * @property cause 导致重试的IO异常，可为null
         * @property detail 故障详情描述，可为null
         */
        data class Retry(val cause: IOException? = null, val detail: String? = null) : FetchOutcome()
    }

    /**
     * 将HTTP响应映射为FetchOutcome。
     *
     * 处理逻辑：
     * - 403 + 速率限制耗尽 → 终止性速率限制错误
     * - 5xx → 可重试
     * - 其他非2xx → 终止性HTTP错误
     * - 2xx → 解析JSON响应体
     *
     * @param response HTTP响应对象
     * @return 对应的FetchOutcome
     */
    private fun handleResponse(response: Response): FetchOutcome {
        val code = response.code

        if (code == 403) {
            val rateLimit = parseRateLimit(response)
            if (rateLimit.exhausted) {
                return FetchOutcome.Done(GitHubTreeResult.Error(rateLimit.message, isRateLimited = true))
            }
            return FetchOutcome.Done(mapHttpError(code))
        }

        if (code in 500..599) {
            return FetchOutcome.Retry(detail = "HTTP $code")
        }

        if (!response.isSuccessful) {
            return FetchOutcome.Done(mapHttpError(code))
        }

        val body = response.body?.string()
        if (body == null) {
            return FetchOutcome.Done(GitHubTreeResult.Error(MSG_EMPTY_BODY))
        }
        return FetchOutcome.Done(parseTreeJson(body))
    }

    /**
     * 解析GitHub树API的JSON响应体。
     * 优雅处理缺失的"tree"数组。
     * JSON解析错误为终止性错误（不重试）。
     *
     * @param body JSON响应体字符串
     * @return 解析后的文件树结果
     */
    internal fun parseTreeJson(body: String): GitHubTreeResult {
        return try {
            val json = JSONObject(body)
            val treeArray: JSONArray = json.optJSONArray("tree") ?: JSONArray()
            val isTruncated = json.optBoolean("truncated", false)

            val files = mutableListOf<GitHubFileEntry>()
            for (i in 0 until treeArray.length()) {
                val entry = treeArray.getJSONObject(i)
                val path = entry.getString("path")
                val type = entry.getString("type")
                if (type == "blob" && !path.startsWith(".git")) {
                    files.add(GitHubFileEntry(path = path, type = type))
                }
            }
            GitHubTreeResult.Success(files, truncated = isTruncated)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse GitHub tree JSON", e)
            GitHubTreeResult.Error(MSG_PARSE_ERROR)
        }
    }

    /**
     * 构建trees API URL，使用URL编码的路径段。
     * 防止路径段注入攻击。
     *
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param branch 分支名称
     * @return 完整的API URL字符串
     */
    internal fun buildApiUrl(owner: String, repo: String, branch: String): String {
        val encodedOwner = URLEncoder.encode(owner, "UTF-8")
        val encodedRepo = URLEncoder.encode(repo, "UTF-8")
        val encodedBranch = URLEncoder.encode(branch, "UTF-8")
        return "$API_BASE/repos/$encodedOwner/$encodedRepo/git/trees/$encodedBranch?recursive=1"
    }

    /**
     * 计算重试延迟，使用指数退避（上限为[MAX_RETRY_DELAY_MS]），
     * 加上50%-100%的乘法抖动以避免惊群效应。
     *
     * @param attempt 当前重试次数（从1开始）
     * @return 延迟时间（毫秒）
     */
    internal fun computeBackoff(attempt: Int): Long {
        val exponential = INITIAL_RETRY_DELAY_MS * (1L shl (attempt - 1))
        val capped = exponential.coerceAtMost(MAX_RETRY_DELAY_MS)
        val jitterFactor = MIN_JITTER_FACTOR + Random.nextDouble() * (1.0 - MIN_JITTER_FACTOR)
        return (capped * jitterFactor).toLong().coerceAtLeast(0L)
    }

    /**
     * 解析速率限制响应头，判断是否已耗尽。
     *
     * @param response HTTP响应对象
     * @return 速率限制信息
     */
    private fun parseRateLimit(response: Response): RateLimitInfo {
        val remaining = response.header(HEADER_RATE_LIMIT_REMAINING)?.toIntOrNull()
        if (remaining == null || remaining != 0) {
            return RateLimitInfo(exhausted = false, message = "")
        }
        val resetTime = response.header(HEADER_RATE_LIMIT_RESET)?.toLongOrNull()
        val resetMessage = if (resetTime != null) {
            val resetDate = Date(resetTime * 1000L)
            " (rate limit resets at $resetDate)"
        } else {
            ""
        }
        return RateLimitInfo(
            exhausted = true,
            message = "GitHub API rate limit exceeded$resetMessage. Try again later or use a GitHub token.",
        )
    }

    /**
     * 将HTTP状态码映射为用户可见的错误消息。
     * 使用受控消息，不泄露内部细节。
     *
     * @param code HTTP状态码
     * @return 对应的错误结果
     */
    private fun mapHttpError(code: Int): GitHubTreeResult =
        GitHubTreeResult.Error(
            when (code) {
                404 -> "仓库或分支不存在，请检查链接是否正确"
                403 -> "访问被拒绝，可能需要授权"
                in 400..499 -> "请求无效 (HTTP $code)"
                else -> "GitHub 服务器错误 (HTTP $code)"
            },
        )

    /**
     * 速率限制信息内部数据类。
     *
     * @property exhausted 是否已耗尽速率限制
     * @property message 用户可见的提示消息
     */
    private data class RateLimitInfo(val exhausted: Boolean, val message: String)

    companion object {
        /** 进程级共享OkHttpClient（连接/线程池复用） */
        // SECURITY VULN-017 FIXED: 证书锁定 api.github.com
        // 防止 MITM 攻击者拦截 GitHub API 请求。
        // Pin 值与 network_security_config.xml 中的 pin-set 保持一致。
        // 主 pin：GitHub 当前证书公钥 SHA-256 指纹
        // 备份 pin：GitHub 备用证书公钥 SHA-256 指纹
        // 注意：GitHub 证书轮换时需同步更新此处和 XML 中的 pin 值。
        private val certificatePinner: CertificatePinner = CertificatePinner.Builder()
            .add("api.github.com", "sha256/mQbXGHjmAKgSLEjPnLE8Z0oum2Nhb9Fh6BcOp3K9Q1M=")
            .add("api.github.com", "sha256/6t4tKqmqJFWqHKaf7MrM1N5VxVCz0sXLYL1W0s3b6m0=")
            .build()

        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_S, TimeUnit.SECONDS)
            .certificatePinner(certificatePinner)
            .build()

        /** 最小抖动因子（50%） */
        private const val MIN_JITTER_FACTOR = 0.5

        /**
         * 使用进程级共享[OkHttpClient]创建[GitHubApiClient]实例。
         *
         * @return 配置好的GitHubApiClient实例
         */
        fun create(): GitHubApiClient = GitHubApiClient(defaultClient)
    }
}
