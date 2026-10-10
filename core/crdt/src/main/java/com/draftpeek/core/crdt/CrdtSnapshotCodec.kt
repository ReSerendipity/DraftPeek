package com.draftpeek.core.crdt

import com.draftpeek.core.sync.SyncDataCorruptException
import com.draftpeek.core.sync.SyncSnapshot

/**
 * 在 [CrdtDocument] 与 [SyncSnapshot] 之间转换。
 *
 * 传输层只认「路径 → 文本」（它不关心文档格式），本类负责把文档装进/取出那一个文件。
 * 这样**格式知识只存在于文档层**，换格式不会波及传输与协调器。
 */
class CrdtSnapshotCodec(private val documentPath: String = DEFAULT_DOCUMENT_PATH) {

    /** 文档 → 快照（用于推送）。 */
    fun encode(document: CrdtDocument): SyncSnapshot = SyncSnapshot(mapOf(documentPath to CrdtJson.write(document)))

    /**
     * 快照 → 文档（用于合并）。
     *
     * - 远端还没有这个文件（首次同步）→ 返回**空文档**，这是正常情况；
     * - 远端文档损坏 → **抛 [SyncDataCorruptException]**（成因是内部的 JsonParseException），
     *   绝不返回空文档。
     *
     * 后者是安全边界：若把损坏内容当成「空文档」，上层会认为远端没有任何数据，
     * 于是拿本地数据整份覆盖远端 —— 那等于**用一次解析失败销毁用户的数据**。
     * 抛异常则协调器把它归类为「数据损坏」（SyncFailureKind.CORRUPT），
     * 两侧数据都保持原样、UI 进入人工修复路径（设计 G7 · B8）。
     */
    fun decode(snapshot: SyncSnapshot): CrdtDocument {
        val text = snapshot.files[documentPath] ?: return CrdtDocument()
        return try {
            CrdtJson.read(text)
        } catch (e: JsonParseException) {
            throw SyncDataCorruptException("远端同步文档无法解析", e)
        }
    }

    /**
     * 文档 → 本地持久化文本（上层缓存「上次同步后的文档」用）。
     *
     * 与 [encode] 的差别只是产物为裸文本 —— 缓存层不必理解快照的文件结构，
     * 格式知识仍只留在本类（与 [restore] 成对使用）。
     */
    fun persist(document: CrdtDocument): String = CrdtJson.write(document)

    /**
     * 持久化文本 → 文档；损坏时抛 [JsonParseException]（与 [decode] 同策略：不猜）。
     *
     * 上层若把该缓存视为**可再生中间态**（真身在 Room 与远端仓库），可捕获异常按
     * 空文档降级 —— 但降级不得成为常态路径。
     */
    fun restore(text: String): CrdtDocument = CrdtJson.read(text)

    companion object {
        /** 默认文档文件名（位于仓库的同步目录下）。 */
        const val DEFAULT_DOCUMENT_PATH = "sync.json"
    }
}
