package com.draftpeek.core.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("领域模型")
class DomainModelsTest {

    @Nested
    @DisplayName("UserActivity 加权强度与活动等级")
    inner class UserActivityTests {

        @Test
        @DisplayName("默认值全为 0，无活动")
        fun defaults_areZeroAndInactive() {
            val activity = UserActivity(date = "2026-10-19")
            assertEquals("2026-10-19", activity.date)
            assertEquals(0, activity.totalIntensity())
            assertEquals(0, activity.totalOperations())
            assertFalse(activity.hasActivity())
            assertEquals(0, activity.activityLevel())
        }

        @Test
        @DisplayName("totalIntensity 按操作类型加权（编辑×5 打开×3 预览类×2 其他×1）")
        fun totalIntensity_appliesWeights() {
            val activity = UserActivity(
                date = "2026-10-19",
                fileOpenCount = 1,
                textEditCount = 1,
                otherOperationCount = 1,
                sessionCount = 1,
                previewCount = 1,
                searchCount = 1,
                snippetCount = 1,
                diffCount = 1,
                usageDurationMinutes = 1,
                charWriteCount = 1,
                fileCreateCount = 1
            )
            // 3 + 5 + 1 + 1 + 2 + 2 + 2 + 2 + 1 + 1 + 3 = 23
            assertEquals(23, activity.totalIntensity())
            assertTrue(activity.hasActivity())
        }

        @Test
        @DisplayName("activityLevel 边界：1/5/6/15/16/30/31 次 → 1/1/2/2/3/3/4 级")
        fun activityLevel_boundaries() {
            fun levelOf(ops: Int) = UserActivity(date = "d", usageDurationMinutes = ops).activityLevel()
            assertEquals(1, levelOf(1))
            assertEquals(1, levelOf(5))
            assertEquals(2, levelOf(6))
            assertEquals(2, levelOf(15))
            assertEquals(3, levelOf(16))
            assertEquals(3, levelOf(30))
            assertEquals(4, levelOf(31))
        }

        @Test
        @DisplayName("copy 保留未修改字段且 equals/hashCode 一致")
        fun copy_andEquals() {
            val base = UserActivity(date = "2026-10-19", textEditCount = 3)
            val copied = base.copy(textEditCount = 5)
            assertEquals(5, copied.textEditCount)
            assertEquals(base.date, copied.date)
            assertEquals(base, base.copy())
            assertEquals(base.hashCode(), base.copy().hashCode())
            assertNotEquals(base, copied)
        }
    }

    @Nested
    @DisplayName("UserActivitySummary")
    inner class UserActivitySummaryTests {

        @Test
        @DisplayName("默认值为 0，copy/equals 正常")
        fun defaults_andCopy() {
            val summary = UserActivitySummary(date = "2026-10-19")
            assertEquals("2026-10-19", summary.date)
            assertEquals(0, summary.fileOpenCount)
            assertEquals(0L, summary.charWriteCount)
            val copied = summary.copy(textEditCount = 2, charWriteCount = 10L)
            assertEquals(2, copied.textEditCount)
            assertEquals(10L, copied.charWriteCount)
            assertEquals(summary, summary.copy())
            assertNotEquals(summary, copied)
        }
    }

    @Nested
    @DisplayName("SecurityEvent")
    inner class SecurityEventTests {

        @Test
        @DisplayName("extra 默认空串，copy 保留字段")
        fun defaults_andCopy() {
            val event = SecurityEvent(
                eventType = "DETECTION",
                threatLevel = "SUSPICIOUS",
                signalsMask = 0b101,
                responseLevel = "WARNING",
                timestampEpochMs = 1_760_000_000_000,
                anonymizedDeviceId = "uuid-1",
                appVersionCode = 10030
            )
            assertEquals("", event.extra)
            assertEquals(0, event.id)
            val copied = event.copy(id = 7L, extra = "{\"k\":1}")
            assertEquals(7L, copied.id)
            assertEquals("{\"k\":1}", copied.extra)
            assertEquals(event.eventType, copied.eventType)
            assertEquals(event, event.copy())
        }
    }

    @Nested
    @DisplayName("EditorFile")
    inner class EditorFileTests {

        @Test
        @DisplayName("默认 UTF-8 / 可写 / 非二进制")
        fun defaults() {
            val file = EditorFile(uri = "file:///a.md", fileName = "a.md", language = "markdown", fileSize = 100L)
            assertEquals("UTF-8", file.detectedEncoding)
            assertFalse(file.isReadOnly)
            assertFalse(file.isBinary)
        }

        @Test
        @DisplayName("copy 覆盖指定字段")
        fun copy_overrides() {
            val file = EditorFile(uri = "u", fileName = "f", language = null, fileSize = 1L)
            val copied = file.copy(isReadOnly = true, isBinary = true, detectedEncoding = "GBK")
            assertTrue(copied.isReadOnly)
            assertTrue(copied.isBinary)
            assertEquals("GBK", copied.detectedEncoding)
            assertEquals(file.uri, copied.uri)
        }
    }

    @Nested
    @DisplayName("Link 与 ReadingPosition")
    inner class LinkAndPositionTests {

        @Test
        @DisplayName("Link 默认 id=0，copy 正常")
        fun link_defaults_andCopy() {
            val link = Link(sourceUri = "file:///a.md", targetTitle = "笔记B")
            assertEquals(0L, link.id)
            val copied = link.copy(id = 3L)
            assertEquals(3L, copied.id)
            assertEquals("笔记B", copied.targetTitle)
            assertEquals(link, link.copy())
        }

        @Test
        @DisplayName("ReadingPosition equals/hashCode/toString")
        fun readingPosition_equality() {
            val p1 = ReadingPosition(uri = "file:///a.md", line = 3, column = 7, scrollX = 0, scrollY = 120)
            val p2 = p1.copy()
            assertEquals(p1, p2)
            assertEquals(p1.hashCode(), p2.hashCode())
            assertEquals("file:///a.md", p2.uri)
            assertEquals(3, p2.line)
            assertEquals(7, p2.column)
            assertEquals(0, p2.scrollX)
            assertEquals(120, p2.scrollY)
            assertNotEquals(p1, p2.copy(line = 4))
        }
    }
}
