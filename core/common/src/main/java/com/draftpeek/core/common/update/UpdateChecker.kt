/**
 * 应用内更新检查模块。
 *
 * 通过 GitHub Releases API 检查最新发布版本，对比当前版本号判断是否有可用更新。
 * 设计为离线优先：无网络时静默返回无更新，不阻塞用户使用。
 *
 * 复用 GitHubApiClient 的 OkHttp 基础设施（证书锁定、超时配置）。
 *
 * @author DraftPeek Team
 * @since 1.0.31
 */
package com.draftpeek.core.common.update

import android.content.Context
import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val TAG = "UpdateChecker"

/** 连接超时（秒） */
private const val CONNECT_TIMEOUT_S = 10L

/** 读取超时（秒） */
private const val READ_TIMEOUT_S = 10L

/** 最大重试次数 */
private const val MAX_RETRIES = 2

/** 初始重试延迟（毫秒） */
private const val INITIAL_RETRY_DELAY_MS = 2000L

/** GitHub API 基础 URL */
private const val API_BASE = "https://api.github.com"

/**
 * 版本比较结果。
 */
sealed class UpdateResult {
    /** 有新版本可用。 */
    data class UpdateAvailable(
        val latestVersion: String,
        val downloadUrl: String?,
        val releaseNotes: String,
        val htmlUrl: String
    ) : UpdateResult()

    /** 当前版本已是最新。 */
    data object UpToDate : UpdateResult()

    /** 检查失败（无网络/速率限制等）。 */
    data class Error(val message: String) : UpdateResult()
}

/**
 * 应用内更新检查器。
 *
 * 使用 GitHub Releases API 获取最新发布版本信息。
 * 不依赖 Firebase Remote Config 或自建后端，完全基于 GitHub 基础设施。
 *
 * **使用方式**：
 * ```
 * val result = UpdateChecker.checkForUpdate(
 *     context = context,
 *     owner = "Doro",
 *     repo = "DraftPeek",
 *     currentVersion = "1.0.30",
 * )
 * when (result) {
 *     is UpdateResult.UpdateAvailable -> { /* 显示更新弹窗 */ }
 *     is UpdateResult.UpToDate -> { /* 无需更新 */ }
 *     is UpdateResult.Error -> { /* 静默忽略 */ }
 * }
 * ```
 */
object UpdateChecker {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
            .build()
    }

    /** SharedPreferences 缓存 key */
    private const val PREFS_NAME = "draftpeek_update_check"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"
    private const val KEY_LAST_VERSION = "last_version"
    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L // 6 小时缓存

    /**
     * 检查是否有新版本可用。
     *
     * 使用 GitHub Releases API 获取最新 Release（非 pre-release/draft）。
     * 结果缓存 6 小时，避免频繁请求消耗速率配额。
     *
     * @param context 应用上下文
     * @param owner GitHub 仓库所有者
     * @param repo GitHub 仓库名
     * @param currentVersion 当前应用版本（如 "1.0.30"）
     * @param forceRefresh 是否强制刷新缓存
     * @return 更新检查结果
     */
    suspend fun checkForUpdate(
        context: Context,
        owner: String,
        repo: String,
        currentVersion: String,
        forceRefresh: Boolean = false
    ): UpdateResult = withContext(Dispatchers.IO) {
        // 检查缓存
        if (!forceRefresh) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0)
            val cachedVersion = prefs.getString(KEY_LAST_VERSION, null)
            if (cachedVersion != null && System.currentTimeMillis() - lastCheck < CACHE_TTL_MS) {
                return@withContext buildResult(cachedVersion, currentVersion, null, null)
            }
        }

        val apiUrl = "$API_BASE/repos/$owner/$repo/releases/latest"
        var lastException: IOException? = null

        for (attempt in 1..MAX_RETRIES) {
            try {
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = client.newCall(request).execute()
                response.use { res ->
                    if (res.code == 404) {
                        return@withContext UpdateResult.Error("No releases found")
                    }
                    if (res.code == 403) {
                        return@withContext UpdateResult.Error("GitHub API rate limit exceeded")
                    }
                    if (!res.isSuccessful) {
                        Log.w(TAG, "GitHub API returned HTTP ${res.code}")
                        if (res.code in 500..599 && attempt < MAX_RETRIES) {
                            delay(INITIAL_RETRY_DELAY_MS * attempt)
                            return@use
                        }
                        return@withContext UpdateResult.Error("Server error (HTTP ${res.code})")
                    }

                    val body = res.body?.string()
                    if (body.isNullOrBlank()) {
                        return@withContext UpdateResult.Error("Empty response")
                    }

                    val json = JSONObject(body)
                    val tagName = json.optString("tag_name", "").removePrefix("v")
                    if (tagName.isBlank()) {
                        return@withContext UpdateResult.Error("Invalid release data: missing tag_name")
                    }

                    val htmlUrl = json.optString("html_url", "")
                    val releaseBody = json.optString("body", "")

                    // 提取第一个 APK 下载链接
                    val downloadUrl = extractApkDownloadUrl(json)

                    // 缓存结果
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit()
                        .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
                        .putString(KEY_LAST_VERSION, tagName)
                        .apply()

                    return@withContext buildResult(tagName, currentVersion, downloadUrl, htmlUrl, releaseBody)
                }

                if (attempt < MAX_RETRIES) {
                    delay(INITIAL_RETRY_DELAY_MS * attempt)
                }
            } catch (e: IOException) {
                lastException = e
                Log.w(TAG, "Update check failed (attempt $attempt/$MAX_RETRIES): ${e.message}")
                if (attempt < MAX_RETRIES) {
                    delay(INITIAL_RETRY_DELAY_MS * attempt)
                }
            }
        }

        UpdateResult.Error(lastException?.message ?: "Network error")
    }

    /**
     * 从 Release JSON 中提取 APK 下载 URL。
     */
    private fun extractApkDownloadUrl(json: JSONObject): String? {
        return try {
            val assets = json.optJSONArray("assets") ?: return null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk")) {
                    return asset.optString("browser_download_url", null)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 构建更新检查结果。
     */
    private fun buildResult(
        latestVersion: String,
        currentVersion: String,
        downloadUrl: String?,
        htmlUrl: String?,
        releaseNotes: String = ""
    ): UpdateResult {
        val comparison = compareVersions(latestVersion, currentVersion)
        return if (comparison > 0) {
            UpdateResult.UpdateAvailable(
                latestVersion = latestVersion,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes,
                htmlUrl = htmlUrl ?: ""
            )
        } else {
            UpdateResult.UpToDate
        }
    }

    /**
     * 比较两个语义化版本号。
     *
     * @return 正数表示 v1 > v2，负数表示 v1 < v2，0 表示相等
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }
}
