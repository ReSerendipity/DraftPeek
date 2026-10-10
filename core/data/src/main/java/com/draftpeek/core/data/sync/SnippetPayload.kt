/**
 * 片段在同步快照里的载荷编解码。
 *
 * 键是 `snippet:<syncId>`，值是这里编码出来的 JSON 对象。
 *
 * ## 字段选择的理由
 *
 * - **含** `title` / `content` / `language` / `category` / `createdAt` / `updatedAt`
 *   —— 片段是自包含内容，跨设备语义完整；
 * - **不含** `id` —— 它是本地自增主键，跨设备必撞（这正是要新增 `syncId` 的原因）；
 * - **不含** `syncId` —— 它已经在键里了，重复存一份只会带来「键与值不一致」的可能。
 *
 * 用完整字段名（而非缩写）是刻意的：数据落在**用户自己的仓库**里、长期可读，
 * 可读性本身就是价值（同 [com.draftpeek.core.crdt.CrdtStamp] 的取舍）。
 */
package com.draftpeek.core.data.sync

import com.draftpeek.core.crdt.JsonCodec
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.sync.SyncDataCorruptException

/** 片段载荷 ⇄ JSON 的编解码（纯函数，可单测）。 */
object SnippetPayload {

    private const val K_TITLE = "title"
    private const val K_CONTENT = "content"
    private const val K_LANGUAGE = "language"
    private const val K_CATEGORY = "category"
    private const val K_CREATED = "createdAt"
    private const val K_UPDATED = "updatedAt"

    /** 编码为 JSON 文本（`language` 可为 null）。 */
    fun encode(snippet: Snippet): String = JsonCodec.encodeStrings(
        mapOf(
            K_TITLE to snippet.title,
            K_CONTENT to snippet.content,
            K_LANGUAGE to snippet.language,
            K_CATEGORY to snippet.category,
            K_CREATED to snippet.createdAt.toString(),
            K_UPDATED to snippet.updatedAt.toString()
        )
    )

    /**
     * 解析 JSON 文本为片段。
     *
     * [syncId] 由**键**提供（载荷里不存）。**严格失败不猜**：必需字段缺失或时间戳非法即抛异常 ——
     * 静默降级成空串/0 会把损坏数据当成真实内容写回本地。
     */
    fun decode(text: String, syncId: String): Snippet {
        val fields = JsonCodec.decodeStrings(text)
        return Snippet(
            syncId = syncId,
            title = fields.require(K_TITLE),
            content = fields.require(K_CONTENT),
            language = fields[K_LANGUAGE],
            category = fields.require(K_CATEGORY),
            createdAt = fields.requireLong(K_CREATED),
            updatedAt = fields.requireLong(K_UPDATED)
        )
    }

    private fun Map<String, String?>.require(key: String): String =
        this[key] ?: throw SyncDataCorruptException("片段载荷缺少必需字段 '$key'")

    private fun Map<String, String?>.requireLong(key: String): Long =
        this[key]?.toLongOrNull() ?: throw SyncDataCorruptException("片段载荷字段 '$key' 不是合法整数")
}
