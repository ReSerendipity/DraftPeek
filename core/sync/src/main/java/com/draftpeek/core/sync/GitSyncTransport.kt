package com.draftpeek.core.sync

/**
 * 方案 C 的载体实现：把快照同步到**用户自己的 Git 仓库**。
 *
 * 走 Git Contents API（`GET/PUT /repos/{owner}/{repo}/contents/{path}`），
 * 不做本地 clone —— 同步数据量小（KB 级），逐文件读写足够，且避免了在设备上维护工作区的复杂度。
 *
 * **本类不含任何 UI 与业务数据模型**：它只搬运「路径 → 文本」，业务结构由上层决定。
 * HTTP 通过 [GitHttpClient] 注入，因此**全部成功/失败/冲突分支都能用假实现单测**。
 *
 * 失败语义（方案 C 特有）：
 * - 远端文件不存在（404）在 `pull` 里**不是错误**，而是「首次同步」的正常情况；
 * - `push` 前必须先取远端 `sha`（Contents API 更新已有文件必须带 sha），
 *   若在取 sha 与写入之间远端又被改动，服务端返回 **409 → [SyncFailureKind.CONFLICT]**。
 *
 * @param directory 同步数据所在目录，默认独立目录以免与用户自己的文件混在一起
 * @param fileNames 目录下参与同步的文件名；默认单文档，将来要拆多文件改这里即可
 */
class GitSyncTransport(
    private val host: GitHost,
    private val owner: String,
    private val repo: String,
    private val branch: String,
    private val token: String,
    private val http: GitHttpClient,
    private val directory: String = DEFAULT_DIRECTORY,
    private val fileNames: List<String> = DEFAULT_FILES,
    private val commitMessage: String = DEFAULT_COMMIT_MESSAGE
) : SyncTransport {

    private fun remotePath(name: String): String = "$directory/$name"

    override suspend fun pull(): PullOutcome {
        val files = LinkedHashMap<String, String>()
        for (name in fileNames) {
            val response = try {
                http.get(
                    host.contentsUrlWithRef(owner, repo, remotePath(name), branch),
                    host.headers(token)
                )
            } catch (throwable: Throwable) {
                return PullOutcome.Failure(SyncFailureMapper.fromThrowable(throwable), throwable)
            }
            when {
                // 远端还没有这个文件：首次同步的正常情况，不是错误
                response.statusCode == 404 -> continue
                response.statusCode in 200..299 -> {
                    val encoded = GitPayload.contentOf(response.body) ?: continue
                    files[name] = GitPayload.decodeBase64(encoded)
                }

                else -> return PullOutcome.Failure(
                    SyncFailureMapper.fromHttpStatus(response.statusCode, response.rateLimited)
                )
            }
        }
        return PullOutcome.Success(SyncSnapshot(files))
    }

    override suspend fun push(snapshot: SyncSnapshot): TransferResult {
        for (name in fileNames) {
            val content = snapshot.files[name] ?: continue

            val sha = when (val probe = fetchRemoteSha(name)) {
                is RemoteProbe.Failed -> return TransferResult.Failure(probe.kind, probe.cause)
                is RemoteProbe.Found -> probe.sha
                RemoteProbe.Missing -> null
            }

            val body = GitPayload.putBody(
                message = commitMessage,
                content = content,
                branch = branch,
                sha = sha
            )
            val response = try {
                http.put(host.contentsUrl(owner, repo, remotePath(name)), host.headers(token), body)
            } catch (throwable: Throwable) {
                return TransferResult.Failure(SyncFailureMapper.fromThrowable(throwable), throwable)
            }
            if (response.statusCode !in 200..299) {
                return TransferResult.Failure(
                    SyncFailureMapper.fromHttpStatus(response.statusCode, response.rateLimited)
                )
            }
        }
        return TransferResult.Success
    }

    /** 读取远端文件的 `sha`（更新已有文件必须带上它；新建时为 null）。 */
    private suspend fun fetchRemoteSha(name: String): RemoteProbe {
        val response = try {
            http.get(
                host.contentsUrlWithRef(owner, repo, remotePath(name), branch),
                host.headers(token)
            )
        } catch (throwable: Throwable) {
            return RemoteProbe.Failed(SyncFailureMapper.fromThrowable(throwable), throwable)
        }
        return when {
            response.statusCode == 404 -> RemoteProbe.Missing
            response.statusCode in 200..299 -> GitPayload.shaOf(response.body)
                ?.let { RemoteProbe.Found(it) }
                ?: RemoteProbe.Missing

            else -> RemoteProbe.Failed(
                SyncFailureMapper.fromHttpStatus(response.statusCode, response.rateLimited)
            )
        }
    }

    private sealed interface RemoteProbe {
        data class Found(val sha: String) : RemoteProbe
        data object Missing : RemoteProbe
        data class Failed(val kind: SyncFailureKind, val cause: Throwable? = null) : RemoteProbe
    }

    companion object {
        /** 同步数据存放的仓库目录。 */
        const val DEFAULT_DIRECTORY = ".draftpeek"

        /** 默认单文档同步；要拆多文件时改这里。 */
        val DEFAULT_FILES: List<String> = listOf("sync.json")

        const val DEFAULT_COMMIT_MESSAGE = "chore(draftpeek): sync"
    }
}
