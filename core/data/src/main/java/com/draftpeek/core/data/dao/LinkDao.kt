/**
 * 双向链接数据访问对象（DAO）接口。
 *
 * 提供对 `links` 表的增删查操作。查询均返回 [Flow] 以支持数据库变化的
 * 自动响应式更新（例如某文档重新保存后其反向链接列表即时刷新）。
 *
 * @author DraftPeek Team
 * @since 1.0.0
 */
package com.draftpeek.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.draftpeek.core.data.entity.LinkEntity
import kotlinx.coroutines.flow.Flow

/**
 * 链接表 DAO 接口。
 */
@Dao
interface LinkDao {

    /**
     * 获取某文档引用的所有目标标题（正向链接）。
     * @param sourceUri 引用来源文件 URI
     * @return [Flow] 目标标题列表，按更新时间升序
     */
    @Query("SELECT targetTitle FROM links WHERE sourceUri = :sourceUri ORDER BY updatedAt ASC")
    fun getOutgoingTitles(sourceUri: String): Flow<List<String>>

    /**
     * 获取引用某目标标题的所有来源（反向链接 / backlink）。
     * @param targetTitle 被引用目标标题
     * @return [Flow] 引用来源文件的 URI 列表，按更新时间升序
     */
    @Query("SELECT sourceUri FROM links WHERE targetTitle = :targetTitle ORDER BY updatedAt ASC")
    fun getBacklinks(targetTitle: String): Flow<List<String>>

    /**
     * 获取某个文档当前引用的全部链接实体（用于替换式重建索引）。
     * @param sourceUri 引用来源文件 URI
     * @return 该文档的全部链接实体列表
     */
    @Query("SELECT * FROM links WHERE sourceUri = :sourceUri")
    suspend fun getLinksForSource(sourceUri: String): List<LinkEntity>

    /**
     * 替换某文档的全部链接索引：删除旧记录后插入新记录。
     * @param sourceUri 引用来源文件 URI
     */
    @Query("DELETE FROM links WHERE sourceUri = :sourceUri")
    suspend fun deleteForSource(sourceUri: String)

    /**
     * 批量插入链接记录。冲突时替换（同一 sourceUri+targetTitle 去重）。
     * @param links 要写入的链接实体列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<LinkEntity>)
}