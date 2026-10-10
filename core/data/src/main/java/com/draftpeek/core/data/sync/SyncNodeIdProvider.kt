/**
 * 同步节点标识提供者（CRDT 的 `nodeId`）。
 *
 * ## 为什么需要它
 *
 * CRDT 的戳是 `(物理毫秒, 计数器, 节点 id)` 三元组，其中节点 id 用于**完全并发
 * （同毫秒同计数）时确定性地打破平局** —— 保证所有设备合并出同一结果。
 * 没有它，两台设备在同一毫秒的写入在合并时结果不确定（不同设备算出不同结果）。
 *
 * ## 为什么不复用现有的匿名设备 ID
 *
 * `SecurityEventRepositoryImpl.anonymizedDeviceId` 是 `by lazy` 的**进程级缓存**
 * （每次应用启动重新生成、不持久化）。那对「安全事件打标」够用，但
 * [com.draftpeek.core.crdt.HybridLogicalClock] 的契约要求「首次生成后持久化」：
 * 持久化后「同一设备」的标识稳定，跨设备排查同步问题时能对上号。
 *
 * ## 生命周期
 *
 * 首次调用时生成 UUID 并写入 DataStore，之后**终身不变**；
 * 卸载重装会重新生成（符合 CRDT 对节点 id 的要求 —— 重装视为新节点）。
 */
package com.draftpeek.core.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.draftpeek.core.data.uiDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 节点标识来源接缝（供 [RoomSnapshotSource] 依赖，测试可注入假实现）。
 */
fun interface SyncNodeIdSource {

    /** 取本设备稳定标识；实现须保证「首次生成后终身不变」。 */
    suspend fun nodeId(): String
}

/**
 * 提供本设备的同步节点标识。
 *
 * @property context 应用上下文（用于访问 [uiDataStore]）
 */
@Singleton
class SyncNodeIdProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncNodeIdSource {

    /** 进程内缓存，避免每次同步都读 DataStore。 */
    @Volatile
    private var cached: String? = null

    /** 并发保护：首次可能被多个协程同时触发（同步启动 + UI 订阅）。 */
    private val lock = Mutex()

    /**
     * 取本设备标识；首次调用时生成并持久化。
     *
     * 并发安全：双检锁（先看缓存，再在锁内复查），保证只生成一次。
     */
    override suspend fun nodeId(): String {
        cached?.let { return it }
        return lock.withLock {
            cached?.let { return@withLock it }
            val existing = context.uiDataStore.data.first()[KEY]
            val id = existing ?: UUID.randomUUID().toString().also { generated ->
                context.uiDataStore.edit { prefs -> prefs[KEY] = generated }
            }
            cached = id
            id
        }
    }

    private companion object {
        /** DataStore 键名（`ui_prefs` 文件内）。 */
        val KEY = stringPreferencesKey("sync_node_id")
    }
}
