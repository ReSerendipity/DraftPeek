/**
 * 安全事件数据访问对象（DAO）接口。
 *
 * 提供对 `security_events` 表的插入、查询和清除操作。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.draftpeek.core.data.entity.SecurityEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * 安全事件表 DAO 接口。
 */
@Dao
interface SecurityEventDao {

    /**
     * 插入安全事件记录。
     * @param event 安全事件实体
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SecurityEventEntity)

    /**
     * 获取所有安全事件，按时间倒序排列。
     * @return [Flow] 包含所有安全事件的列表
     */
    @Query("SELECT * FROM security_events ORDER BY timestampEpochMs DESC")
    fun getAllEvents(): Flow<List<SecurityEventEntity>>

    /**
     * 获取最近 N 条安全事件。
     * @param limit 最大返回数量
     * @return 安全事件列表
     */
    @Query("SELECT * FROM security_events ORDER BY timestampEpochMs DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<SecurityEventEntity>

    /**
     * 获取安全事件总数。
     * @return 事件总数
     */
    @Query("SELECT COUNT(*) FROM security_events")
    suspend fun getEventCount(): Int

    /**
     * 清空所有安全事件日志。
     */
    @Query("DELETE FROM security_events")
    suspend fun deleteAll()
}
