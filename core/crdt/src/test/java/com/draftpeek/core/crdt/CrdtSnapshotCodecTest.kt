package com.draftpeek.core.crdt

import com.draftpeek.core.sync.SyncSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CrdtSnapshotCodec 文档⇄快照⇄持久化")
class CrdtSnapshotCodecTest {

    private val codec = CrdtSnapshotCodec()
    private val clock = HybridLogicalClock("test-node")

    private fun sampleDocument(): CrdtDocument {
        var document = CrdtDocument()
        document = document.put("snippet:a", """{"title":"A"}""", clock.next())
        document = document.put("snippet:b", """{"title":"B"}""", clock.next())
        document = document.remove("snippet:a", clock.next())
        return document
    }

    @Test
    @DisplayName("encode→decode 保留可见值与墓碑")
    fun snapshotRoundTripPreservesEntriesAndTombstones() {
        val original = sampleDocument()
        val restored = codec.decode(codec.encode(original))
        assertEquals(original.visibleValues(), restored.visibleValues())
        assertEquals(original.tombstones(), restored.tombstones())
    }

    @Test
    @DisplayName("远端缺文件（首次同步）→ 空文档，这是正常情况不是错误")
    fun missingFileDecodesToEmptyDocument() {
        val empty = codec.decode(SyncSnapshot(mapOf("other.json" to "{}")))
        assertTrue(empty.visibleValues().isEmpty())
        assertTrue(empty.tombstones().isEmpty())
    }

    @Test
    @DisplayName("persist→restore 往返一致；损坏文本抛解析异常绝不返回空")
    fun persistRestoreRoundTripAndStrictFailure() {
        val original = sampleDocument()
        val restored = codec.restore(codec.persist(original))
        assertEquals(original.visibleValues(), restored.visibleValues())
        assertEquals(original.tombstones(), restored.tombstones())
        assertThrows(JsonParseException::class.java) { codec.restore("corrupt{") }
    }

    @Test
    @DisplayName("远端文档损坏 decode → SyncDataCorruptException（供 UI 走人工修复，不反复重试）")
    fun corruptRemoteDocumentClassifiedAsCorrupt() {
        val corrupt = SyncSnapshot(mapOf("sync.json" to "corrupt{{"))
        assertThrows(com.draftpeek.core.sync.SyncDataCorruptException::class.java) {
            codec.decode(corrupt)
        }
    }

    @Test
    @DisplayName("自定义路径与默认路径互不串读")
    fun customPathDoesNotReadDefaultFile() {
        val custom = CrdtSnapshotCodec("custom.json")
        val snapshot = custom.encode(sampleDocument())
        // 用默认路径的 codec 解自定义路径的快照 → 视作「远端还没有文件」→ 空文档
        assertTrue(codec.decode(snapshot).visibleValues().isEmpty())
        assertEquals(2, custom.decode(snapshot).let { it.visibleValues().size + it.tombstones().size })
    }
}
