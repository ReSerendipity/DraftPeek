/**
 * 代码片段 FTS4 全文搜索虚拟表实体。
 *
 * 使用 SQLite FTS4（Full-Text Search）引擎为 [Snippet] 的标题和内容列建立全文索引，
 * 支持高效的关键词搜索。通过 `contentEntity = Snippet::class` 与主表建立关联，
 * FTS 表仅存储索引数据，实际内容存储在主表中以节省空间。
 *
 * ## FTS4 搜索语法
 * 支持标准 FTS4 MATCH 语法：
 * - 隐式 AND：`hello world` 匹配同时包含两个词的记录
 * - OR：`hello OR world` 匹配包含任一的记录
 * - NOT：`hello NOT world` 匹配含 hello 但不含 world 的记录
 * - 短语：`"hello world"` 匹配精确短语
 * - 列限定：`title:hello` 仅在标题列搜索
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * 代码片段全文搜索虚拟表。
 *
 * 此实体映射到 `snippets_fts` FTS4 虚拟表，与 [Snippet] 主表通过 rowid 关联。
 * 当主表数据变更时，需通过 `INSERT INTO snippets_fts(snippets_fts) VALUES('rebuild')` 重建索引。
 *
 * @property title 片段标题（被索引列）
 * @property content 片段代码内容（被索引列）
 */
@Fts4(contentEntity = Snippet::class)
@Entity(tableName = "snippets_fts")
data class SnippetFts(
    val title: String,
    val content: String,
)
