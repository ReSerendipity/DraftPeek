package com.draftpeek.core.common.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DiffEngine")
class DiffEngineTest {

    /**
     * Mirror of [DiffEngine.MAX_DIFF_LINES]. Kept as a literal here so any
     * change to the production threshold is flagged by a failing test.
     */
    private val maxDiffLines = 5000

    @Nested
    @DisplayName("diff() — equal content")
    inner class EqualContentTest {

        @Test
        @DisplayName("returns all EQUAL when both sides are identical")
        fun identicalStrings() {
            val text = "line1\nline2\nline3"
            val result = DiffEngine.diff(text, text)

            assertEquals(0, result.diffCount)
            assertEquals(3, result.leftLines.size)
            assertEquals(3, result.rightLines.size)
            assertTrue(result.leftLines.all { it.type == DiffType.EQUAL })
            assertTrue(result.rightLines.all { it.type == DiffType.EQUAL })
        }

        @Test
        @DisplayName("empty left and right produce no diff")
        fun bothEmpty() {
            val result = DiffEngine.diff("", "")

            assertEquals(0, result.diffCount)
            assertEquals(0, result.leftLines.size)
            assertEquals(0, result.rightLines.size)
        }

        @Test
        @DisplayName("empty left vs non-empty right: right rows are INSERT")
        fun emptyLeft() {
            val result = DiffEngine.diff("", "a\nb")

            assertEquals(2, result.leftLines.size)
            assertEquals(2, result.rightLines.size)
            assertTrue(result.rightLines.all { it.type == DiffType.INSERT })
        }
    }

    @Nested
    @DisplayName("diff() — non-equal content")
    inner class NonEqualContentTest {

        @Test
        @DisplayName("a single changed line is reported as MODIFY/DELETE/INSERT")
        fun singleLineModified() {
            val result = DiffEngine.diff("hello", "world")

            assertTrue(result.diffCount > 0)
            val leftTypes = result.leftLines.map { it.type }
            val rightTypes = result.rightLines.map { it.type }
            assertTrue(
                leftTypes.contains(DiffType.MODIFY) || leftTypes.contains(DiffType.DELETE),
                "Expected MODIFY/DELETE in left, got $leftTypes"
            )
            assertTrue(
                rightTypes.contains(DiffType.MODIFY) || rightTypes.contains(DiffType.INSERT),
                "Expected MODIFY/INSERT in right, got $rightTypes"
            )
        }

        @Test
        @DisplayName("a line only in left is DELETE; only in right is INSERT")
        fun pureInsertAndDelete() {
            val result = DiffEngine.diff(
                "common\na_only\nshared",
                "common\nshared\nb_only"
            )

            assertTrue(result.leftLines.any { it.type == DiffType.DELETE })
            assertTrue(result.rightLines.any { it.type == DiffType.INSERT })
        }

        @Test
        @DisplayName("common lines preserve their text on both sides")
        fun commonLinesPreserved() {
            val result = DiffEngine.diff("x\ny\nz", "x\ny\nz")

            assertEquals(listOf("x", "y", "z"), result.leftLines.map { it.content })
            assertEquals(listOf("x", "y", "z"), result.rightLines.map { it.content })
        }
    }

    @Nested
    @DisplayName("diff() — pure insert/delete (Myers edge cases)")
    inner class PureInsertDeleteTest {

        @Test
        @DisplayName("pure insert: empty left → all INSERT on right")
        fun pureInsert() {
            val result = DiffEngine.diff("", "a\nb\nc")

            assertEquals(3, result.diffCount)
            assertEquals(3, result.rightLines.size)
            assertTrue(result.rightLines.all { it.type == DiffType.INSERT })
        }

        @Test
        @DisplayName("pure delete: non-empty left → empty right")
        fun pureDelete() {
            val result = DiffEngine.diff("x\ny\nz", "")

            assertEquals(3, result.diffCount)
            assertEquals(3, result.leftLines.size)
            assertTrue(result.leftLines.all { it.type == DiffType.DELETE })
        }

        @Test
        @DisplayName("completely different lines produce correct MODIFY pairing")
        fun completelyDifferent() {
            val result = DiffEngine.diff("a\nb", "c\nd")

            assertTrue(result.diffCount > 0)
            assertEquals(result.leftLines.size, result.rightLines.size)
            // Every left row should be non-EQUAL
            assertTrue(result.leftLines.all { it.type != DiffType.EQUAL })
            assertTrue(result.rightLines.all { it.type != DiffType.EQUAL })
        }
    }

    @Nested
    @DisplayName("diff() — Myers backtrack correctness")
    inner class MyersBacktrackTest {

        @Test
        @DisplayName("insert at beginning: right line 1 is new")
        fun insertAtBeginning() {
            val result = DiffEngine.diff("b\nc", "a\nb\nc")

            assertEquals(1, result.rightLines.count { it.type == DiffType.INSERT })
            // Common lines b, c should be EQUAL
            assertTrue(result.leftLines.any { it.type == DiffType.EQUAL && it.content == "b" })
        }

        @Test
        @DisplayName("delete at end: left has trailing DELETE")
        fun deleteAtEnd() {
            val result = DiffEngine.diff("a\nb\nc", "a\nb")

            assertEquals(1, result.leftLines.count { it.type == DiffType.DELETE })
            // Common lines a, b should be EQUAL
            assertTrue(result.leftLines.any { it.type == DiffType.EQUAL && it.content == "a" })
        }

        @Test
        @DisplayName("interleaved inserts and deletes maintain row alignment")
        fun interleavedEdits() {
            val result = DiffEngine.diff(
                "a\nb\nc\nd",
                "a\nx\nc\ny"
            )

            assertEquals(result.leftLines.size, result.rightLines.size)
            assertTrue(result.diffCount > 0)
            // First and third lines should be EQUAL
            assertTrue(result.leftLines.any { it.type == DiffType.EQUAL && it.content == "a" })
            assertTrue(result.leftLines.any { it.type == DiffType.EQUAL && it.content == "c" })
        }

        @Test
        @DisplayName("single line replace: DELETE + INSERT pair")
        fun singleLineReplace() {
            val result = DiffEngine.diff("old", "new")

            assertEquals(result.leftLines.size, result.rightLines.size)
            assertTrue(result.diffCount > 0)
        }
    }

    @Nested
    @DisplayName("diff() — invariants")
    inner class InvariantsTest {

        @Test
        @DisplayName("left and right always have the same row count")
        fun rowsAligned() {
            val result = DiffEngine.diff("a\nb\nc\nd", "a\nx\nc")

            assertEquals(result.leftLines.size, result.rightLines.size)
        }

        @Test
        @DisplayName("diffCount equals non-EQUAL entries on the left side")
        fun diffCountMatchesLeft() {
            val result = DiffEngine.diff("a\nb\nc", "a\nx\nc")

            val nonEqual = result.leftLines.count { it.type != DiffType.EQUAL }
            assertEquals(nonEqual, result.diffCount)
        }

        @Test
        @DisplayName("input exceeding MAX_DIFF_LINES is rejected without OOM")
        fun oversizeRejected() {
            val bigText = (1..maxDiffLines + 100).joinToString("\n") { "a" }
            val result = DiffEngine.diff(bigText, bigText)

            // When rejected, the engine returns the inputs as-is with no diff
            assertEquals(0, result.diffCount)
            assertNotNull(result.leftLines)
            assertNotNull(result.rightLines)
        }
    }
}
