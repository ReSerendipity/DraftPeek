package com.draftpeek.core.crdt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CrdtStamp / HybridLogicalClock")
class CrdtStampTest {

    @Test
    @DisplayName("比较顺序：先物理毫秒，再计数，最后节点 id（保证确定性）")
    fun ordering() {
        val base = CrdtStamp(1_000L, 0, "a")
        assertTrue(base < CrdtStamp(1_001L, 0, "a"))
        assertTrue(base < CrdtStamp(1_000L, 1, "a"))
        assertTrue(base < CrdtStamp(1_000L, 0, "b"))
        assertEquals(0, base.compareTo(CrdtStamp(1_000L, 0, "a")))
    }

    @Test
    @DisplayName("同一毫秒内多次写入：戳严格递增（靠计数器）")
    fun monotonicWithinSameMillis() {
        val clock = HybridLogicalClock("node") { 5_000L }
        val first = clock.next()
        val second = clock.next()
        val third = clock.next()
        assertEquals(5_000L, first.wallMillis)
        assertTrue(first < second)
        assertTrue(second < third)
    }

    @Test
    @DisplayName("时钟回拨：戳不回退，靠计数器继续前进")
    fun clockGoesBackwards() {
        var now = 10_000L
        val clock = HybridLogicalClock("node") { now }
        val before = clock.next()
        now = 1_000L // 用户把时钟改早了
        val after = clock.next()
        assertTrue(after > before)
        assertEquals(10_000L, after.wallMillis)
    }

    @Test
    @DisplayName("observe 远端戳后，本地新戳必须大于远端（否则本地写入会一直落败）")
    fun observePushesClockForward() {
        val clock = HybridLogicalClock("node") { 1_000L }
        val remote = CrdtStamp(9_999L, 3, "peer")
        clock.observe(remote)
        assertTrue(clock.next() > remote)
    }

    @Test
    @DisplayName("observeAll 覆盖文档内所有戳")
    fun observeAll() {
        val clock = HybridLogicalClock("node") { 1_000L }
        val far = CrdtStamp(8_888L, 0, "peer")
        val doc = CrdtDocument(mapOf("k" to CrdtEntry("v", far)))
        clock.observeAll(doc)
        assertTrue(clock.next() > far)
    }
}

@DisplayName("CrdtDocument · LWW 合并语义")
class CrdtDocumentTest {

    private val nodeA = "nodeA"
    private val nodeB = "nodeB"

    private fun stamp(wall: Long, counter: Int = 0, node: String = nodeA) = CrdtStamp(wall, counter, node)

    @Nested
    @DisplayName("读写")
    inner class ReadWrite {

        @Test
        @DisplayName("put 后可读；remove 写墓碑，value 返回 null 但键仍在")
        fun putAndRemove() {
            val doc = CrdtDocument()
                .put("k", "v1", stamp(1))
                .remove("k", stamp(2))

            assertNull(doc.value("k"))
            assertFalse(doc.isPresent("k"))
            assertTrue(doc.tombstones().contains("k"))
        }

        @Test
        @DisplayName("visibleValues 只返回未删除的键")
        fun visibleValues() {
            val doc = CrdtDocument()
                .put("keep", "1", stamp(1))
                .put("gone", "2", stamp(1))
                .remove("gone", stamp(2))
            assertEquals(mapOf("keep" to "1"), doc.visibleValues())
        }
    }

    @Nested
    @DisplayName("合并")
    inner class Merge {

        @Test
        @DisplayName("同一键：戳大者胜（后写覆盖先写）")
        fun laterStampWins() {
            val local = CrdtDocument().put("k", "old", stamp(1))
            val remote = CrdtDocument().put("k", "new", stamp(2, node = nodeB))
            assertEquals("new", local.merge(remote).value("k"))
            // 反向合并结果一致（可交换）
            assertEquals("new", remote.merge(local).value("k"))
        }

        @Test
        @DisplayName("墓碑也能胜出：远端删除不会被本地旧值复活")
        fun tombstoneWinsOverOlderPut() {
            val local = CrdtDocument().put("k", "alive", stamp(1))
            val remote = CrdtDocument().remove("k", stamp(2, node = nodeB))
            assertNull(local.merge(remote).value("k"))
        }

        @Test
        @DisplayName("旧墓碑不会覆盖新值")
        fun olderTombstoneLoses() {
            val local = CrdtDocument().put("k", "alive", stamp(5))
            val remote = CrdtDocument().remove("k", stamp(2, node = nodeB))
            assertEquals("alive", local.merge(remote).value("k"))
        }

        @Test
        @DisplayName("不同键互不影响")
        fun disjointKeys() {
            val local = CrdtDocument().put("a", "1", stamp(1))
            val remote = CrdtDocument().put("b", "2", stamp(1, node = nodeB))
            val merged = local.merge(remote)
            assertEquals("1", merged.value("a"))
            assertEquals("2", merged.value("b"))
        }

        @Test
        @DisplayName("幂等：重复合并同一份文档不改变结果")
        fun idempotent() {
            val local = CrdtDocument().put("a", "1", stamp(1))
            val remote = CrdtDocument().put("b", "2", stamp(2, node = nodeB))
            val once = local.merge(remote)
            assertEquals(once, once.merge(remote))
        }

        @Test
        @DisplayName("可结合：三种合并顺序得到同一结果")
        fun associative() {
            val d1 = CrdtDocument().put("k", "v1", stamp(1))
            val d2 = CrdtDocument().put("k", "v2", stamp(2, node = nodeB))
            val d3 = CrdtDocument().put("k", "v3", stamp(3))
            assertEquals(d1.merge(d2).merge(d3), d3.merge(d1.merge(d2)))
            assertEquals(d1.merge(d2).merge(d3), d2.merge(d3).merge(d1))
        }

        @Test
        @DisplayName("合并空文档返回原文档")
        fun mergeEmpty() {
            val local = CrdtDocument().put("a", "1", stamp(1))
            assertEquals(local, local.merge(CrdtDocument()))
        }
    }
}
