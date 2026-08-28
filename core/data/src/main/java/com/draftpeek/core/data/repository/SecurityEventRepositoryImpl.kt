/**
 * 安全事件仓库实现类。
 *
 * 通过 [SecurityEventDao] 实现安全事件数据的持久化操作。
 *
 * @author DraftPeek Team
 * @since 1.0.30
 */
package com.draftpeek.core.data.repository

import com.draftpeek.core.data.dao.SecurityEventDao
import com.draftpeek.core.data.entity.SecurityEventEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * [SecurityEventRepository] 的 Room 实现。
 *
 * @property dao 安全事件 DAO 实例，由 Hilt 注入
 */
@Singleton
class SecurityEventRepositoryImpl @Inject constructor(private val dao: SecurityEventDao) : SecurityEventRepository {

    /** 匿名设备 ID（进程级缓存，每次应用启动时首次生成并缓存） */
    private val anonymizedDeviceId: String by lazy {
        UUID.randomUUID().toString()
    }

    override val allEvents: Flow<List<SecurityEventEntity>> = dao.getAllEvents()

    override suspend fun record(eventType: String, threatLevel: String, signalsMask: Int, responseLevel: String) {
        dao.insert(
            SecurityEventEntity(
                eventType = eventType,
                threatLevel = threatLevel,
                signalsMask = signalsMask,
                responseLevel = responseLevel,
                timestampEpochMs = System.currentTimeMillis(),
                anonymizedDeviceId = anonymizedDeviceId,
                appVersionCode = getAppVersionCode()
            )
        )
    }

    override suspend fun getRecentEvents(limit: Int): List<SecurityEventEntity> = dao.getRecentEvents(limit)

    override suspend fun getEventCount(): Int = dao.getEventCount()

    override suspend fun deleteAll() = dao.deleteAll()

    /**
     * 获取应用版本号。
     * 通过反射读取 BuildConfig 以避免 core/data 模块对 app 模块的依赖。
     */
    private fun getAppVersionCode(): Int = try {
        val buildConfigClass = Class.forName("com.draftpeek.BuildConfig")
        val field = buildConfigClass.getField("VERSION_CODE")
        field.getInt(null)
    } catch (_: Exception) {
        0
    }
}
