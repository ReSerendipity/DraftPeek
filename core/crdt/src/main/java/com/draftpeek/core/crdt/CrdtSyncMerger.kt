package com.draftpeek.core.crdt

import com.draftpeek.core.sync.SyncMerger
import com.draftpeek.core.sync.SyncSnapshot

/**
 * 用 CRDT 文档合并两份快照 —— 实现 `core:sync` 的 [SyncMerger] 接缝。
 *
 * 合并语义：两侧都解析成 [CrdtDocument] 后**逐键取戳大者**。
 * 因为戳里带节点 id，多设备乱序同步也会收敛到同一结果（幂等 / 可交换 / 可结合）。
 *
 * **失败语义（重要）**：任一侧文档损坏时**抛出异常**，不猜、不返回空文档。
 * 协调器会把它当成一次同步失败 —— **不写本地、不推远端**，两侧数据都保持原样。
 * 用户可以在自己的仓库里直接查看并修复那个文件（数据放用户仓库的好处之一）。
 *
 * **调用方需注意**：合并结果里含有远端带来的戳，本地时钟应据其推进
 * （[HybridLogicalClock.observeAll]），否则本机时钟落后时新写入会持续在合并中落败。
 * 时钟归属写入方（本地数据层），故本类不代持。
 */
class CrdtSyncMerger(private val codec: CrdtSnapshotCodec = CrdtSnapshotCodec()) : SyncMerger {

    override suspend fun merge(local: SyncSnapshot, remote: SyncSnapshot): SyncSnapshot {
        val localDocument = codec.decode(local)
        val remoteDocument = codec.decode(remote)
        return codec.encode(localDocument.merge(remoteDocument))
    }
}
