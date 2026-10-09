package com.draftpeek.core.crdt

import com.draftpeek.core.sync.SyncSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CrdtSnapshotCodec / CrdtSyncMerger · 文档层与同步层的桥接")
class CrdtSyncMergerTest {

    private val codec = CrdtSnapshotCodec()
    private val merger = CrdtSyncMerger(codec)

    private fun snapshotOf(document: CrdtDocument): SyncSnapshot = codec.encode(document)

    @Nested
    @DisplayName("编解码")
    inner class Codec {

        @Test
        @DisplayName("文档 → 快照 → 文档：原样往返")
        fun roundTrip() {
            val doc = CrdtDocument()
                .put("pos:a.md", "42", CrdtStamp(1_000L, 0, "nodeA"))
                .remove("snippet:x", CrdtStamp(2_000L, 0, "nodeA"))
            assertEquals(doc, codec.decode(codec.encode(doc)))
        }

        @Test
        @DisplayName("快照里没有该文件（首次同步）→ 空文档，不是错误")
        fun missingFileIsEmpty() {
            assertEquals(CrdtDocument(), codec.decode(SyncSnapshot.EMPTY))
        }

        @Test
        @DisplayName("文件内容损坏 → 抛异常（**绝不返回空文档**）")
        fun corruptFileThrows() {
            val corrupt = SyncSnapshot(mapOf("sync.json" to "{ this is not json"))
            assertThrows(JsonParseException::class.java) { codec.decode(corrupt) }
        }
    }

    @Nested
    @DisplayName("合并")
    inner class Merge {

        @Test
        @DisplayName("同一键：戳大者胜；不同键互不影响")
        fun basicMerge() = runTest {
            val local = CrdtDocument()
                .put("k", "local-old", CrdtStamp(1_000L, 0, "nodeA"))
                .put("onlyLocal", "L", CrdtStamp(1_000L, 0, "nodeA"))
            val remote = CrdtDocument()
                .put("k", "remote-new", CrdtStamp(2_000L, 0, "nodeB"))
                .put("onlyRemote", "R", CrdtStamp(1_000L, 0, "nodeB"))

            val merged = codec.decode(merger.merge(snapshotOf(local), snapshotOf(remote)))

            assertEquals("remote-new", merged.value("k"))
            assertEquals("L", merged.value("onlyLocal"))
            assertEquals("R", merged.value("onlyRemote"))
        }

        @Test
        @DisplayName("首次同步：远端为空时本地数据完整保留")
        fun firstSyncKeepsLocal() = runTest {
            val local = CrdtDocument().put("k", "mine", CrdtStamp(1_000L, 0, "nodeA"))
            val merged = codec.decode(merger.merge(snapshotOf(local), SyncSnapshot.EMPTY))
            assertEquals("mine", merged.value("k"))
        }

        @Test
        @DisplayName("远端的删除（墓碑）能压过本地旧值，且不会复活")
        fun tombstoneWins() = runTest {
            val local = CrdtDocument().put("k", "alive", CrdtStamp(1_000L, 0, "nodeA"))
            val remote = CrdtDocument().remove("k", CrdtStamp(2_000L, 0, "nodeB"))
            val merged = codec.decode(merger.merge(snapshotOf(local), snapshotOf(remote)))
            assertNull(merged.value("k"))
        }

        @Test
        @DisplayName("**安全属性**：远端文档损坏时合并失败，绝不产出「空文档」去覆盖远端")
        fun corruptRemoteDoesNotWipe() = runTest {
            val local = CrdtDocument().put("k", "mine", CrdtStamp(1_000L, 0, "nodeA"))
            val corruptRemote = SyncSnapshot(mapOf("sync.json" to "{\"v\":1,\"entries\":"))

            // assertThrows 的 lambda 不是挂起上下文，故用 runCatching 包 suspend 调用
            val thrown = runCatching { merger.merge(snapshotOf(local), corruptRemote) }.exceptionOrNull()
            assertTrue(thrown is JsonParseException, "期望 JsonParseException，实际 $thrown")
            // 关键：没有返回任何「可推送的快照」⇒ 协调器不会写本地、不会推远端，两侧数据都安全
        }

        @Test
        @DisplayName("本地文档损坏时同样失败（不猜、不覆盖）")
        fun corruptLocalAlsoFails() = runTest {
            val corruptLocal = SyncSnapshot(mapOf("sync.json" to "garbage"))
            val thrown = runCatching { merger.merge(corruptLocal, SyncSnapshot.EMPTY) }.exceptionOrNull()
            assertTrue(thrown is JsonParseException, "期望 JsonParseException，实际 $thrown")
        }
    }
}
