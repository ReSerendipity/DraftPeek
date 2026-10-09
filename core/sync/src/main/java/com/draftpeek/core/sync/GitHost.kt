package com.draftpeek.core.sync

/**
 * Git 托管商的接入差异（方案 C 首发：GitHub + Gitee）。
 *
 * 把这些差异收敛成**数据**而不是散落在代码里的 if：换/加托管商只加一个枚举值。
 *
 * ⚠️ **Gitee 的鉴权头形态待实测确认**：官方示例多用 `access_token` 查询参数，
 * 这里按「参照 GitHub」的说法用 `Authorization: token <token>`。
 * 首次真实接入 Gitee 时以实测为准——这是本枚举里唯一未经验证的字段。
 */
enum class GitHost(
    /** REST API 根地址。 */
    val apiBaseUrl: String,
    /** `Authorization` 头里的方案名。 */
    private val authScheme: String,
    /** `Accept` 头。 */
    val acceptHeader: String,
    /** 可选的 API 版本头（GitHub 要求，Gitee 无）。 */
    private val apiVersionHeader: Pair<String, String>?
) {
    GITHUB(
        apiBaseUrl = "https://api.github.com",
        authScheme = "Bearer",
        acceptHeader = "application/vnd.github+json",
        apiVersionHeader = "X-GitHub-Api-Version" to "2022-11-28"
    ),

    GITEE(
        apiBaseUrl = "https://gitee.com/api/v5",
        authScheme = "token",
        acceptHeader = "application/json",
        apiVersionHeader = null
    );

    /** 构造该托管商所需的请求头。 */
    fun headers(token: String): Map<String, String> = buildMap {
        put("Authorization", "$authScheme $token")
        put("Accept", acceptHeader)
        put("Content-Type", "application/json")
        apiVersionHeader?.let { (name, value) -> put(name, value) }
    }

    /** 仓库内容接口的 URL（不含文件路径）。 */
    fun contentsUrl(owner: String, repo: String, path: String): String = "$apiBaseUrl/repos/$owner/$repo/contents/$path"

    /** 仓库内容接口的 URL（带 ref，用于读取指定分支）。 */
    fun contentsUrlWithRef(owner: String, repo: String, path: String, branch: String): String =
        "${contentsUrl(owner, repo, path)}?ref=$branch"
}
