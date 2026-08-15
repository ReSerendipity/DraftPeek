/**
 * 代码片段仓库接口。
 *
 * 提供代码片段的 CRUD 操作、分类/语言筛选、全文搜索（FTS4）和子串模糊搜索功能。
 * 代码片段是用户保存的常用代码模板，支持按分类和编程语言组织管理。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.repository

import com.draftpeek.core.data.entity.Snippet
import kotlinx.coroutines.flow.Flow

/**
 * 代码片段数据仓库接口。
 */
interface SnippetRepository {

    /**
     * 获取所有代码片段，按更新时间降序排列。
     * @return [Flow] 代码片段列表
     */
    fun getAllSnippets(): Flow<List<Snippet>>

    /**
     * 获取指定分类下的代码片段。
     * @param category 分类名称
     * @return [Flow] 该分类下的片段列表
     */
    fun getSnippetsByCategory(category: String): Flow<List<Snippet>>

    /**
     * 获取指定编程语言的代码片段。
     * @param language 语言标识
     * @return [Flow] 该语言的片段列表
     */
    fun getSnippetsByLanguage(language: String): Flow<List<Snippet>>

    /**
     * FTS4 全文搜索代码片段。
     * @param query FTS4 搜索查询
     * @return [Flow] 匹配的片段列表
     */
    fun searchSnippets(query: String): Flow<List<Snippet>>

    /**
     * FTS4 全文搜索并按语言过滤。
     * @param query FTS4 搜索查询
     * @param language 语言标识
     * @return [Flow] 匹配的片段列表
     */
    fun searchSnippetsByLanguage(query: String, language: String): Flow<List<Snippet>>

    /**
     * FTS4 全文搜索并按分类过滤。
     * @param query FTS4 搜索查询
     * @param category 分类名称
     * @return [Flow] 匹配的片段列表
     */
    fun searchSnippetsByCategory(query: String, category: String): Flow<List<Snippet>>

    /**
     * LIKE 子串模糊搜索（FTS 分词回退方案）。
     * @param query 子串关键词
     * @return [Flow] 匹配的片段列表
     */
    fun searchSnippetsSubstring(query: String): Flow<List<Snippet>>

    /**
     * 根据 ID 获取单个代码片段。
     * @param id 片段 ID
     * @return [Flow] 发射匹配的片段或 null
     */
    fun getSnippetById(id: Long): Flow<Snippet?>

    /**
     * 获取所有不重复的分类名称。
     * @return [Flow] 分类名称列表
     */
    fun getAllCategories(): Flow<List<String>>

    /**
     * 添加新代码片段。
     * @param snippet 要添加的片段实体（id=0 自动生成）
     * @return 新插入记录的 ID
     */
    suspend fun addSnippet(snippet: Snippet): Long

    /**
     * 更新已有的代码片段。
     * @param snippet 要更新的片段实体
     */
    suspend fun updateSnippet(snippet: Snippet)

    /**
     * 删除代码片段。
     * @param snippet 要删除的片段实体
     */
    suspend fun deleteSnippet(snippet: Snippet)
}
