/**
 * 基于 Room 的本地快照源（阶段 B 第一批：**仅片段**）。
 *
 * ## 键与载荷
 *
 * - 键：`snippet:<syncId>`（`syncId` 是跨设备稳定的 UUID 列，见规格 v1.2 §1）；
 * - 载荷：片段字段的 JSON 对象（[SnippetPayload]）。
 *
 * `pos:`（阅读进度）与 `stat:`（统计）**同为保留字，本类不读不写** —— 第一批只同步片段。
 *
 * ## 三条必须守住的纪律
 *
 * 1. **写回前先推进本地时钟**（`observeAll`）。不这么做的话，本机时钟落后时产生的戳
 *    会永远小于远端已有的戳，本地新写入在合并中**持续落败**（表现为「改了但同步不上去」）。
 * 2. **只认 `snippet:` 前缀**。将来别的数据集合进来时各写各的前缀，本类不应越界处理，
 *    否则会把不认识的数据当成片段解析（损坏数据的温床）。
 * 3. **删除必须留下墓碑** —— 见下「删除语义」。
 *
 * ## 删除语义（为什么需要本地缓存文档）
 *
 * Room 里「行没了」这件事本身不携带信息：若 [read] 每次只把**现存行**装进文档，
 * 本机删除的片段在快照里就只是「不存在」——远端仍保留它，下次拉取会把它**复活**。
 * 因此本类持久化「上次同步后的文档」（`filesDir/sync/snippet-doc.json`），
 * [read] 时对比缓存与现存行：缓存里可见、但本地已消失的键 → 打上**墓碑**（[CrdtDocument.remove]）。
 *
 * 缓存是**可再生中间态**（真身在 Room 与远端仓库）：损坏时按空文档降级并继续，
 * 代价只是「一次删除可能复活」，而不是同步永久卡死。
 */
package com.draftpeek.core.data.sync

import android.content.Context
import com.draftpeek.core.crdt.CrdtDocument
import com.draftpeek.core.crdt.CrdtSnapshotCodec
import com.draftpeek.core.crdt.HybridLogicalClock
import com.draftpeek.core.data.entity.Snippet
import com.draftpeek.core.data.repository.SnippetRepository
import com.draftpeek.core.sync.LocalSnapshotSource
import com.draftpeek.core.sync.SyncSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * [LocalSnapshotSource] 的 Room 实现。
 *
 * @property snippets 片段仓库
 * @property nodeIdSource 设备标识来源（构造 CRDT 时钟需要；接口注入便于测试）
 * @property context 应用上下文（缓存文档落盘位置）
 */
@Singleton
class RoomSnapshotSource @Inject constructor(
    private val snippets: SnippetRepository,
    private val nodeIdSource: SyncNodeIdSource,
    @ApplicationContext private val context: Context
) : LocalSnapshotSource {

    private val codec = CrdtSnapshotCodec()

    /** 时钟需在首次使用时按需创建（nodeId 是挂起读取的），故缓存 + 加锁。 */
    @Volatile
    private var cachedClock: HybridLogicalClock? = null

    private val clockLock = Mutex()

    private suspend fun clock(): HybridLogicalClock {
        cachedClock?.let { return it }
        return clockLock.withLock {
            cachedClock?.let { return@withLock it }
            HybridLogicalClock(nodeIdSource.nodeId()).also { cachedClock = it }
        }
    }

    override suspend fun read(): SyncSnapshot {
        val clock = clock()
        val current = snippets.getAllSnippets().first()
        val document = cachedDocument()

        // 纪律 3：缓存里可见、本地已消失的键 → 墓碑（否则删除会被远端复活）
        val currentKeys = current.mapTo(mutableSetOf()) { keyOf(it.syncId) }
        var withTombstones = document
        document.visibleValues().keys.forEach { key ->
            if (key.startsWith(PREFIX) && key !in currentKeys) {
                withTombstones = withTombstones.remove(key, clock.next())
            }
        }

        // 现存行全量入档（同值+新戳对 LWW 无害；换来自洽的「以本地现状为准」语义）
        var result = withTombstones
        current.forEach { snippet ->
            result = result.put(keyOf(snippet.syncId), SnippetPayload.encode(snippet), clock.next())
        }
        return codec.encode(result)
    }

    override suspend fun write(snapshot: SyncSnapshot) {
        val merged = codec.decode(snapshot)

        // 纪律 1：先把本地时钟推到远端之后，否则本地新写入会持续在合并中落败
        clock().observeAll(merged)

        val local = snippets.getAllSnippets().first().associateBy { it.syncId }

        merged.visibleValues().forEach { (key, payload) ->
            val syncId = syncIdOf(key) ?: return@forEach
            val incoming = SnippetPayload.decode(payload, syncId)
            val existing = local[syncId]
            when {
                existing == null -> snippets.addSnippet(incoming)
                incoming.contentEquals(existing) -> Unit // 同值跳过，避免无意义写
                else -> snippets.updateSnippet(incoming.copy(id = existing.id))
            }
        }

        merged.tombstones().forEach { key ->
            val syncId = syncIdOf(key) ?: return@forEach
            local[syncId]?.let { snippets.deleteSnippet(it) }
        }

        // 落盘「上次同步后的文档」，供下次 read() 计算删除墓碑
        saveCached(merged)
    }

    /** 读缓存文档；缺失或损坏 → 空文档（可再生中间态，见文件头「删除语义」）。 */
    private suspend fun cachedDocument(): CrdtDocument = withContext(Dispatchers.IO) {
        val file = docFile()
        if (!file.exists()) return@withContext CrdtDocument()
        val text = file.readText()
        try {
            codec.restore(text)
        } catch (e: Exception) {
            // 解析异常类型是 core:crdt 的 internal，这里按「缓存损坏」统一降级；
            // 协程取消必须原样上抛，否则吞掉取消会破坏结构化并发。
            if (e is CancellationException) throw e
            CrdtDocument()
        }
    }

    private suspend fun saveCached(document: CrdtDocument) = withContext(Dispatchers.IO) {
        val file = docFile()
        file.parentFile?.mkdirs()
        val text = codec.persist(document)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            // rename 失败（跨文件系统/厂商差异）：直写回退，缓存不值得抛错
            file.writeText(text)
            tmp.delete()
        }
    }

    private fun docFile(): File = File(context.filesDir, "sync/snippet-doc.json")

    private fun keyOf(syncId: String): String = PREFIX + syncId

    private fun syncIdOf(key: String): String? =
        if (key.startsWith(PREFIX)) key.removePrefix(PREFIX).takeIf { it.isNotEmpty() } else null

    private companion object {
        /** 片段键前缀（规格 v1.2 §1）。 */
        const val PREFIX = "snippet:"
    }
}

/** 内容等价比较（除本地自增 [Snippet.id] 外全字段），用于跳过无意义更新。 */
private fun Snippet.contentEquals(other: Snippet): Boolean =
    syncId == other.syncId && title == other.title && content == other.content &&
        language == other.language && category == other.category &&
        createdAt == other.createdAt && updatedAt == other.updatedAt
