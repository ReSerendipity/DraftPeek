/**
 * 代码片段数据访问对象（DAO）接口。
 *
 * 提供对 `snippets` 表和 `snippets_fts` FTS4 虚拟表的操作，支持代码片段的 CRUD、
 * 分类/语言筛选、全文搜索（FTS4 MATCH 语法）、子串模糊搜索（LIKE 回退）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.draftpeek.core.data.entity.Snippet
import kotlinx.coroutines.flow.Flow

/**
 * 代码片段表 DAO 接口。
 *
 * 全文搜索使用 SQLite FTS4 虚拟表，支持标准 FTS4 语法（AND/OR/NOT/短语查询）。
 * 对于分词器无法匹配的部分词查询，提供 LIKE 子串搜索作为回退方案。
 */
@Dao
interface SnippetDao {

    /**
     * 获取所有代码片段，按更新时间降序排列。
     * @return [Flow] 代码片段列表
     */
    @Query("SELECT * FROM snippets ORDER BY updatedAt DESC")
    fun getAllSnippets(): Flow<List<Snippet>>

    /**
     * 获取指定分类下的代码片段，按更新时间降序排列。
     * @param category 分类名称（如 "Kotlin", "Compose"）
     * @return [Flow] 该分类下的片段列表
     */
    @Query("SELECT * FROM snippets WHERE category = :category ORDER BY updatedAt DESC")
    fun getSnippetsByCategory(category: String): Flow<List<Snippet>>

    /**
     * 获取指定编程语言的代码片段，按更新时间降序排列。
     * @param language 语言标识（如 "kotlin", "python"）
     * @return [Flow] 该语言的片段列表
     */
    @Query("SELECT * FROM snippets WHERE language = :language ORDER BY updatedAt DESC")
    fun getSnippetsByLanguage(language: String): Flow<List<Snippet>>

    /**
     * 使用 FTS4 全文搜索代码片段（标题和内容）。
     *
     * 查询字符串直接传递给 FTS 引擎，支持标准 FTS4 语法：
     * - "hello world" → 两个词都需出现（隐式 AND）
     * - "hello OR world" → 任一出现即可
     * - '"hello world"' → 精确短语匹配（调用方需加双引号）
     * - "hello NOT world" → 包含 hello 但不包含 world
     *
     * @param query FTS4 搜索查询字符串
     * @return [Flow] 匹配的代码片段列表，按更新时间降序
     */
    @Query("""
        SELECT s.* FROM snippets s
        JOIN snippets_fts fts ON s.id = fts.rowid
        WHERE snippets_fts MATCH :query
        ORDER BY s.updatedAt DESC
    """)
    fun searchSnippets(query: String): Flow<List<Snippet>>

    /**
     * 使用 FTS4 全文搜索并按分类过滤。
     * @param query FTS4 搜索查询字符串
     * @param category 分类名称过滤条件
     * @return [Flow] 匹配的代码片段列表
     */
    @Query("""
        SELECT s.* FROM snippets s
        JOIN snippets_fts fts ON s.id = fts.rowid
        WHERE snippets_fts MATCH :query AND s.category = :category
        ORDER BY s.updatedAt DESC
    """)
    fun searchSnippetsByCategory(query: String, category: String): Flow<List<Snippet>>

    /**
     * 使用 FTS4 全文搜索并按编程语言过滤。
     * @param query FTS4 搜索查询字符串
     * @param language 语言标识过滤条件
     * @return [Flow] 匹配的代码片段列表
     */
    @Query("""
        SELECT s.* FROM snippets s
        JOIN snippets_fts fts ON s.id = fts.rowid
        WHERE snippets_fts MATCH :query AND s.language = :language
        ORDER BY s.updatedAt DESC
    """)
    fun searchSnippetsByLanguage(query: String, language: String): Flow<List<Snippet>>

    /**
     * 使用 FTS4 全文搜索并同时按分类和语言过滤（最严格的组合筛选）。
     * @param query FTS4 搜索查询字符串
     * @param category 分类名称过滤条件
     * @param language 语言标识过滤条件
     * @return [Flow] 匹配的代码片段列表
     */
    @Query("""
        SELECT s.* FROM snippets s
        JOIN snippets_fts fts ON s.id = fts.rowid
        WHERE snippets_fts MATCH :query AND s.category = :category AND s.language = :language
        ORDER BY s.updatedAt DESC
    """)
    fun searchSnippetsByCategoryAndLanguage(query: String, category: String, language: String): Flow<List<Snippet>>

    /**
     * FTS4 标题-only 搜索。
     *
     * 注意：当前实现搜索 title 和 content 列（与 searchSnippets 相同），
     * 如需严格的列范围搜索，应在 FTS 查询中使用 "title:词" 语法。
     * 保留此方法以保持 API 一致性。
     *
     * @param query FTS4 搜索查询字符串
     * @return [Flow] 匹配的代码片段列表
     */
    @Query("""
        SELECT s.* FROM snippets s
        JOIN snippets_fts fts ON s.id = fts.rowid
        WHERE snippets_fts MATCH :query
        ORDER BY s.updatedAt DESC
    """)
    fun searchSnippetsByTitle(query: String): Flow<List<Snippet>>

    /**
     * LIKE 子串模糊搜索（FTS 分词无法匹配部分词时的回退方案）。
     *
     * 在标题和内容中执行 %query% 子串匹配，比 FTS4 慢但能处理
     * FTS 分词器遗漏的部分词匹配（如搜索 "hello" 匹配 "helloworld"）。
     *
     * @param query 子串搜索关键词
     * @return [Flow] 匹配的代码片段列表
     */
    @Query("""
        SELECT * FROM snippets
        WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchSnippetsSubstring(query: String): Flow<List<Snippet>>

    /**
     * 根据 ID 获取单个代码片段的响应式流。
     * @param id 代码片段主键 ID
     * @return [Flow] 发射匹配的片段或 null
     */
    @Query("SELECT * FROM snippets WHERE id = :id")
    fun getSnippetById(id: Long): Flow<Snippet?>

    /**
     * 插入新的代码片段。
     * @param snippet 要插入的片段实体（id 留 0 自动生成）
     * @return 新插入记录的自增 ID
     */
    @Insert
    suspend fun insert(snippet: Snippet): Long

    /**
     * 更新已有的代码片段（按主键匹配）。
     * @param snippet 要更新的片段实体
     */
    @Update
    suspend fun update(snippet: Snippet)

    /**
     * 删除代码片段（按主键匹配）。
     * @param snippet 要删除的片段实体
     */
    @Delete
    suspend fun delete(snippet: Snippet)

    /**
     * 获取所有不重复的分类名称，按字母序排列。
     * @return [Flow] 分类名称列表
     */
    @Query("SELECT DISTINCT category FROM snippets ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    /**
     * 获取所有存在代码片段的不重复编程语言标识，按字母序排列。
     * 排除 language 为 NULL 的记录。
     * @return [Flow] 语言标识列表
     */
    @Query("SELECT DISTINCT language FROM snippets WHERE language IS NOT NULL ORDER BY language")
    fun getAllLanguages(): Flow<List<String>>
}
