package com.draftpeek.core.sync

/**
 * 本地快照的读写接缝。
 *
 * 协调器只负责「读本地 → 拉远端 → 合并 → 写本地 → 推远端」这个流程，
 * **不关心本地数据存在哪、怎么序列化**（DataStore / 文件 / 数据库由上层决定）。
 */
interface LocalSnapshotSource {

    /** 读取当前本地快照。 */
    suspend fun read(): SyncSnapshot

    /** 把合并结果写回本地。 */
    suspend fun write(snapshot: SyncSnapshot)
}

/**
 * 合并策略接缝。
 *
 * 冲突怎么合（CRDT 自动合并 / 按时间戳后写胜 / 按字段优先）属于**文档层**的决策，
 * 与传输、重试、离线续传无关 —— 所以单独抽出来，让协调器保持与合并策略无关，
 * 也便于对协调器做单测（用假实现即可）。
 */
fun interface SyncMerger {

    /**
     * 合并本地与远端快照。
     *
     * @return 合并后的完整快照（协调器会把它写回本地并推到远端）
     */
    suspend fun merge(local: SyncSnapshot, remote: SyncSnapshot): SyncSnapshot
}
