/**
 * 双向链接仓库接口。
 *
 * 定义双链索引的数据层契约，提供正向链接、反向链接查询与索引替换操作。
 * 遵循 Clean Architecture，实现类负责将业务逻辑与 DAO 层解耦。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * 双向链接数据仓库接口。
 */
interface LinkRepository {

    /**
     * 获取某文档引用的所有目标标题（正向链接）的响应式流。
     * @param sourceUri 引用来源文件 URI
     */
    fun getOutgoingTitles(sourceUri: String): Flow<List<String>>

    /**
     * 获取引用某目标标题的所有来源（反向链接 / backlink）的响应式流。
     * @param targetTitle 被引用目标标题
     */
    fun getBacklinks(targetTitle: String): Flow<List<String>>

    /**
     * 替换某文档的全部链接索引。
     *
     * 先删除该 sourceUri 的旧链接，再写入本次解析得到的 [targetTitles]。
     * 用于保存文档时重建其链接关系，保证索引与正文一致。
     *
     * @param sourceUri 引用来源文件 URI
     * @param targetTitles 本次解析出的全部目标标题
     */
    suspend fun replaceLinks(sourceUri: String, targetTitles: List<String>)

    /**
     * 删除某文档的全部链接记录（如文档被删除时）。
     * @param sourceUri 引用来源文件 URI
     */
    suspend fun removeSource(sourceUri: String)
}