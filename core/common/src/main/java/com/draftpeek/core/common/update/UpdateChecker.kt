/**
 * 应用内更新检查模块。
 *
 * 通过 GitHub Releases API 检查最新发布版本，对比当前版本号判断是否有可用更新。
 * 设计为离线优先：无网络时静默返回无更新，不阻塞用户使用。
 *
 * 说明（1.0.34 起）：本模块此前是死代码——`UpdateChecker` 在仓内零调用，
 * 用户看不到任何更新入口。本版把它接进「关于与安全」页的「检查更新」行，
 * 并修掉三处会让入口不可用的问题：缓存回填缺下载地址、5xx 重试分支是空操作、
 * 版本比较把 `1.0.33-dev` 当低版本。版本比较已抽到纯逻辑 [UpdateVersion] 便于单测。
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

/** 缓存有效期（6 小时） */
private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L

/** 默认仓库所有者 */
const val DEFAULT_OWNER: String = "ReSerendipity"

/** 默认仓库名 */
const val DEFAULT_REPO: String = "DraftPeek"

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
 *     currentVersion = "1.0.33",
 * )
 * when (result) {
 *     is UpdateResult.UpdateAvailable -> { /* 显示更新弹窗 */ }
 *     is UpdateResult.UpToDate -> { /* 无需更新 */ }
 *     is UpdateResult.Error -> { /* 提示可稍后再试 */ }
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
    private const val KEY_LAST_DOWNLOAD_URL = "last_download_url"
    private const val KEY_LAST_HTML_URL = "last_html_url"
    private const val KEY_LAST_NOTES = "last_notes"

    /**
     * 检查是否有新版本可用。
     *
     * 使用 GitHub Releases API 获取最新 Release（非 pre-release/draft）。
     * 结果缓存 6 小时，避免频繁请求消耗速率配额。
     *
     * @param context 应用上下文
     * @param currentVersion 当前应用版本（如 "1.0.33"）
     * @param owner GitHub 仓库所有者，默认 [DEFAULT_OWNER]
     * @param repo GitHub 仓库名，默认 [DEFAULT_REPO]
     * @param forceRefresh 是否强制刷新缓存
     * @return 更新检查结果
     */
    suspend fun checkForUpdate(
        context: Context,
        currentVersion: String,
        owner: String = DEFAULT_OWNER,
        repo: String = DEFAULT_REPO,
        forceRefresh: Boolean = false
    ): UpdateResult = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 命中缓存时把下载地址与说明一并回填，否则「有新版本」的弹窗点不出下载页
        if (!forceRefresh) {
            val cachedVersion = prefs.getString(KEY_LAST_VERSION, null)
            val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0)
            if (cachedVersion != null && System.currentTimeMillis() - lastCheck < CACHE_TTL_MS) {
                return@withContext buildResult(
                    latestVersion = cachedVersion,
                    currentVersion = currentVersion,
                    downloadUrl = prefs.getString(KEY_LAST_DOWNLOAD_URL, null),
                    htmlUrl = prefs.getString(KEY_LAST_HTML_URL, null),
                    releaseNotes = prefs.getString(KEY_LAST_NOTES, null) ?: ""
                )
            }
        }

        val apiUrl = "$API_BASE/repos/$owner/$repo/releases/latest"
        var lastError = "Network error"
        var attempt = 1

        while (attempt <= MAX_RETRIES) {
            try {
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = client.newCall(request).execute()
                val code = response.code
                val body = response.use { if (it.isSuccessful) it.body?.string() else null }

                when {
                    code == 404 -> return@withContext UpdateResult.Error("No releases found")
                    code == 403 || code == 429 ->
                        return@withContext UpdateResult.Error("GitHub API rate limit exceeded")
                    code in 500..599 -> {
                        lastError = "Server error (HTTP $code)"
                        Log.w(TAG, "$lastError (attempt $attempt/$MAX_RETRIES)")
                    }
                    !response.isSuccessful ->
                        return@withContext UpdateResult.Error("Server error (HTTP $code)")
                    else -> {
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
                        val downloadUrl = extractApkDownloadUrl(json)

                        prefs.edit()
                            .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
                            .putString(KEY_LAST_VERSION, tagName)
                            .putString(KEY_LAST_DOWNLOAD_URL, downloadUrl)
                            .putString(KEY_LAST_HTML_URL, htmlUrl)
                            .putString(KEY_LAST_NOTES, releaseBody)
                            .apply()

                        return@withContext buildResult(
                            latestVersion = tagName,
                            currentVersion = currentVersion,
                            downloadUrl = downloadUrl,
                            htmlUrl = htmlUrl,
                            releaseNotes = releaseBody
                        )
                    }
                }
            } catch (e: IOException) {
                lastError = e.message ?: "Network error"
                Log.w(TAG, "Update check failed (attempt $attempt/$MAX_RETRIES): $lastError")
            }

            if (attempt < MAX_RETRIES) delay(INITIAL_RETRY_DELAY_MS * attempt)
            attempt++
        }

        UpdateResult.Error(lastError)
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
        return if (UpdateVersion.compare(latestVersion, currentVersion) > 0) {
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
}
