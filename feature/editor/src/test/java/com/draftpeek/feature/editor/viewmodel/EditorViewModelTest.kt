package com.draftpeek.feature.editor.viewmodel

import app.cash.turbine.test
import com.draftpeek.feature.editor.model.EditorUiState
import com.draftpeek.feature.editor.model.MarkdownTheme
import com.draftpeek.feature.editor.model.MarkdownViewMode
import com.draftpeek.feature.editor.tabs.TabManager
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [EditorViewModel] via its testable delegate [EditorStateManager].
 *
 * Since [EditorViewModel] requires Android Context, SavedStateHandle, and Hilt injection,
 * we test the core state logic through [EditorStateManager] which contains all the
 * view-mode switching, content loading, modification tracking, and undo/redo state logic.
 */
class EditorViewModelTest {

    private lateinit var tabManager: TabManager
    private lateinit var stateManager: EditorStateManager

    @BeforeEach
    fun setUp() {
        tabManager = TabManager()
        stateManager = EditorStateManager(tabManager)
        // P0-3: Disable debounce in tests so flushDirtyState commits immediately
        stateManager.dirtyDebounceMs = 0
    }

    @AfterEach
    fun tearDown() {
        // No cleanup needed for pure JVM tests
    }

    // ------------------------------------------------------------------
    // 打开文件后内容加载正确
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("打开文件后内容加载正确")
    inner class LoadContentTests {

        @Test
        @DisplayName("loadContent 应将 uiState 设为 Success")
        fun loadContent_uiStateBecomesSuccess() = runTest {
            stateManager.uiState.test {
                assertEquals(EditorUiState.Loading, awaitItem())

                stateManager.loadContent(
                    content = "hello world",
                    language = "kotlin",
                    fileName = "Test.kt"
                )

                val state = awaitItem()
                assertTrue(state is EditorUiState.Success)
                assertEquals("hello world", (state as EditorUiState.Success).content)
                assertEquals("kotlin", state.language)
                assertEquals("Test.kt", state.fileName)
            }
        }

        @Test
        @DisplayName("loadContent 后 isModified 应为 false")
        fun loadContent_isModifiedFalse() = runTest {
            stateManager.isModified.test {
                stateManager.loadContent("content", "kotlin", "Test.kt")
                assertFalse(awaitItem())
            }
        }

        @Test
        @DisplayName("loadContent 后 getCurrentContent 返回正确内容")
        fun loadContent_getCurrentContentReturnsContent() {
            stateManager.loadContent("some code", "python", "main.py")
            assertEquals("some code", stateManager.getCurrentContent())
        }

        @Test
        @DisplayName("loadContent Markdown 文件后 markdownViewMode 为 WYSIWYG")
        fun loadContent_markdownFile_setsWysiwygMode() = runTest {
            stateManager.markdownViewMode.test {
                // 默认即为 WYSIWYG
                assertEquals(MarkdownViewMode.WYSIWYG, awaitItem())

                stateManager.loadContent("# Title", "markdown", "README.md")
                // Markdown 文件仍为 WYSIWYG，无新发射
                expectNoEvents()
            }
        }

        @Test
        @DisplayName("loadContent 非 Markdown 文件后 markdownViewMode 切换为 EDIT")
        fun loadContent_nonMarkdownFile_switchesToEditMode() = runTest {
            stateManager.markdownViewMode.test {
                assertEquals(MarkdownViewMode.WYSIWYG, awaitItem())

                stateManager.loadContent("val x = 1", "kotlin", "Main.kt")
                assertEquals(MarkdownViewMode.EDIT, awaitItem())
            }
        }

        @Test
        @DisplayName("setLoading 应将 uiState 设为 Loading")
        fun setLoading_uiStateBecomesLoading() = runTest {
            // First load content to get to Success state
            stateManager.loadContent("content", "kotlin", "Test.kt")

            stateManager.uiState.test {
                assertTrue(awaitItem() is EditorUiState.Success)
                stateManager.setLoading()
                assertTrue(awaitItem() is EditorUiState.Loading)
            }
        }

        @Test
        @DisplayName("setError 应将 uiState 设为 Error")
        fun setError_uiStateBecomesError() = runTest {
            stateManager.uiState.test {
                assertEquals(EditorUiState.Loading, awaitItem())
                stateManager.setError("Something went wrong")
                val state = awaitItem()
                assertTrue(state is EditorUiState.Error)
                assertEquals("Something went wrong", (state as EditorUiState.Error).message)
            }
        }
    }

    // ------------------------------------------------------------------
    // 保存文件触发 repository 调用 (通过 onContentSaved 验证)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("保存文件触发状态更新")
    inner class SaveContentTests {

        @Test
        @DisplayName("onContentSaved 后 isModified 应变为 false")
        fun onContentSaved_isModifiedBecomesFalse() = runTest {
            stateManager.loadContent("original", "kotlin", "Test.kt")
            stateManager.onContentChanged("modified")
            stateManager.flushDirtyState()

            stateManager.isModified.test {
                assertTrue(awaitItem())
                stateManager.onContentSaved("saved content")
                assertFalse(awaitItem())
            }
        }

        @Test
        @DisplayName("onContentSaved 后 getCurrentContent 返回保存的内容")
        fun onContentSaved_getCurrentContentReturnsSavedContent() {
            stateManager.loadContent("original", "kotlin", "Test.kt")
            stateManager.onContentChanged("modified")
            stateManager.onContentSaved("saved content")
            assertEquals("saved content", stateManager.getCurrentContent())
        }

        @Test
        @DisplayName("markSaved 后 isModified 变为 false")
        fun markSaved_isModifiedBecomesFalse() = runTest {
            stateManager.loadContent("original", "kotlin", "Test.kt")
            stateManager.onContentChanged("modified")
            stateManager.flushDirtyState()

            stateManager.isModified.test {
                assertTrue(awaitItem())
                stateManager.markSaved()
                assertFalse(awaitItem())
            }
        }
    }

    // ------------------------------------------------------------------
    // 切换视图模式（CODE/SPLIT/PREVIEW）状态正确
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("切换视图模式状态正确")
    inner class ViewModeTests {

        @Test
        @DisplayName("Markdown 文件 togglePreviewMode 循环 WYSIWYG → EDIT → PREVIEW → SPLIT → WYSIWYG")
        fun togglePreviewMode_markdownFile_cyclesThroughModes() = runTest {
            // Load a markdown file so isPreviewable is true
            stateManager.loadContent("# Title", "markdown", "README.md")

            stateManager.markdownViewMode.test {
                // After loading markdown, mode is WYSIWYG
                assertEquals(MarkdownViewMode.WYSIWYG, awaitItem())

                stateManager.togglePreviewMode()
                assertEquals(MarkdownViewMode.EDIT, awaitItem())

                stateManager.togglePreviewMode()
                assertEquals(MarkdownViewMode.PREVIEW, awaitItem())

                stateManager.togglePreviewMode()
                assertEquals(MarkdownViewMode.SPLIT, awaitItem())

                stateManager.togglePreviewMode()
                assertEquals(MarkdownViewMode.WYSIWYG, awaitItem())
            }
        }

        @Test
        @DisplayName("非 Markdown/HTML 文件 togglePreviewMode 不改变模式")
        fun togglePreviewMode_nonMarkdownFile_noChange() = runTest {
            stateManager.loadContent("val x = 1", "kotlin", "Main.kt")
            // Not previewable, so toggle should be no-op
            assertEquals(MarkdownViewMode.EDIT, stateManager.markdownViewMode.value)
            stateManager.togglePreviewMode()
            assertEquals(MarkdownViewMode.EDIT, stateManager.markdownViewMode.value)
        }

        @Test
        @DisplayName("setMarkdownViewMode 在 Markdown 文件上正确设置模式")
        fun setMarkdownViewMode_markdownFile_setsMode() = runTest {
            stateManager.loadContent("# Title", "markdown", "README.md")

            stateManager.markdownViewMode.test {
                assertEquals(MarkdownViewMode.WYSIWYG, awaitItem())

                stateManager.setMarkdownViewMode(MarkdownViewMode.EDIT)
                assertEquals(MarkdownViewMode.EDIT, awaitItem())

                stateManager.setMarkdownViewMode(MarkdownViewMode.SPLIT)
                assertEquals(MarkdownViewMode.SPLIT, awaitItem())
            }
        }

        @Test
        @DisplayName("setMarkdownViewMode 非 Markdown 文件不改变模式")
        fun setMarkdownViewMode_nonMarkdownFile_noChange() = runTest {
            stateManager.loadContent("val x = 1", "kotlin", "Main.kt")
            stateManager.setMarkdownViewMode(MarkdownViewMode.SPLIT)
            assertEquals(MarkdownViewMode.EDIT, stateManager.markdownViewMode.value)
        }

        @Test
        @DisplayName("isPreviewMode 在 EDIT 模式时为 false")
        fun isPreviewMode_editMode_isFalse() {
            stateManager.loadContent("# Title", "markdown", "README.md")
            stateManager.setMarkdownViewMode(MarkdownViewMode.EDIT)
            assertFalse(stateManager.isPreviewMode)
        }

        @Test
        @DisplayName("isPreviewMode 在 PREVIEW 模式时为 true")
        fun isPreviewMode_previewMode_isTrue() {
            stateManager.loadContent("# Title", "markdown", "README.md")
            // After loadContent, markdown files default to PREVIEW
            assertTrue(stateManager.isPreviewMode)
        }

        @Test
        @DisplayName("isPreviewMode 在 SPLIT 模式时为 true")
        fun isPreviewMode_splitMode_isTrue() {
            stateManager.loadContent("# Title", "markdown", "README.md")
            stateManager.setMarkdownViewMode(MarkdownViewMode.SPLIT)
            assertTrue(stateManager.isPreviewMode)
        }
    }

    // ------------------------------------------------------------------
    // 撤销/重做操作状态正确（通过 isModified 跟踪）
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("撤销/重做操作状态正确")
    inner class UndoRedoStateTests {

        @Test
        @DisplayName("初始加载后 isModified 为 false")
        fun initialLoad_isModifiedFalse() = runTest {
            stateManager.loadContent("original", "kotlin", "Test.kt")
            assertFalse(stateManager.isModified.value)
        }

        @Test
        @DisplayName("内容变更后 isModified 为 true")
        fun contentChanged_isModifiedTrue() = runTest {
            stateManager.loadContent("original", "kotlin", "Test.kt")
            stateManager.onContentChanged("modified")
            stateManager.flushDirtyState()

            stateManager.isModified.test {
                assertTrue(awaitItem())
            }
        }

        @Test
        @DisplayName("内容恢复到原始内容后 isModified 为 false（模拟撤销）")
        fun contentRevertedToOriginal_isModifiedFalse() = runTest {
            stateManager.loadContent("original", "kotlin", "Test.kt")
            stateManager.onContentChanged("modified")
            stateManager.flushDirtyState()
            assertTrue(stateManager.isModified.value)

            // Simulate undo: content reverts to original
            stateManager.onContentChanged("original")
            stateManager.flushDirtyState()
            assertFalse(stateManager.isModified.value)
        }

        @Test
        @DisplayName("再次修改后 isModified 重新变为 true（模拟重做）")
        fun contentModifiedAgain_isModifiedTrue() = runTest {
            stateManager.loadContent("original", "kotlin", "Test.kt")
            stateManager.onContentChanged("modified")
            stateManager.flushDirtyState()
            stateManager.onContentChanged("original") // undo
            stateManager.flushDirtyState()
            assertFalse(stateManager.isModified.value)

            // redo
            stateManager.onContentChanged("modified")
            stateManager.flushDirtyState()
            assertTrue(stateManager.isModified.value)
        }

        @Test
        @DisplayName("保存后再次修改 isModified 变为 true")
        fun saveThenModify_isModifiedTrue() = runTest {
            stateManager.loadContent("original", "kotlin", "Test.kt")
            stateManager.onContentChanged("modified")
            stateManager.flushDirtyState()
            stateManager.onContentSaved("modified") // save

            stateManager.onContentChanged("modified again")
            stateManager.flushDirtyState()
            assertTrue(stateManager.isModified.value)
        }
    }

    // ------------------------------------------------------------------
    // 专注模式 / 打字机模式 / Markdown 主题
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("专注模式和打字机模式切换")
    inner class FocusAndTypewriterTests {

        @Test
        @DisplayName("toggleFocusMode 切换专注模式")
        fun toggleFocusMode_togglesState() = runTest {
            stateManager.isFocusMode.test {
                assertFalse(awaitItem())
                stateManager.toggleFocusMode()
                assertTrue(awaitItem())
                stateManager.toggleFocusMode()
                assertFalse(awaitItem())
            }
        }

        @Test
        @DisplayName("toggleTypewriterMode 切换打字机模式")
        fun toggleTypewriterMode_togglesState() = runTest {
            stateManager.isTypewriterMode.test {
                assertFalse(awaitItem())
                stateManager.toggleTypewriterMode()
                assertTrue(awaitItem())
                stateManager.toggleTypewriterMode()
                assertFalse(awaitItem())
            }
        }

        @Test
        @DisplayName("setMarkdownTheme 正确更新主题")
        fun setMarkdownTheme_updatesTheme() = runTest {
            stateManager.markdownTheme.test {
                assertEquals(MarkdownTheme.DEFAULT, awaitItem())

                stateManager.setMarkdownTheme(MarkdownTheme.GITHUB)
                assertEquals(MarkdownTheme.GITHUB, awaitItem())

                stateManager.setMarkdownTheme(MarkdownTheme.NIGHT)
                assertEquals(MarkdownTheme.NIGHT, awaitItem())
            }
        }
    }

    // ------------------------------------------------------------------
    // 光标和滚动位置
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("光标和滚动位置状态")
    inner class CursorAndScrollTests {

        @Test
        @DisplayName("onCursorChanged 更新光标位置")
        fun onCursorChanged_updatesPosition() = runTest {
            stateManager.cursorPosition.test {
                assertEquals(CursorPosition(1, 1), awaitItem())
                stateManager.onCursorChanged(5, 10)
                assertEquals(CursorPosition(5, 10), awaitItem())
            }
        }

        @Test
        @DisplayName("onScrollChanged 更新滚动位置")
        fun onScrollChanged_updatesPosition() = runTest {
            stateManager.scrollPosition.test {
                assertEquals(Pair(0, 0), awaitItem())
                stateManager.onScrollChanged(100, 200)
                assertEquals(Pair(100, 200), awaitItem())
            }
        }
    }

    // ------------------------------------------------------------------
    // 状态保存和恢复
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("状态保存和恢复")
    inner class SaveRestoreStateTests {

        @Test
        @DisplayName("saveState 在未加载文件时返回 null")
        fun saveState_noFileLoaded_returnsNull() {
            assertNull(stateManager.saveState())
        }

        @Test
        @DisplayName("saveState 在文件加载后返回正确的 EditorSavedState")
        fun saveState_fileLoaded_returnsCorrectState() {
            stateManager.loadContent("content", "kotlin", "Test.kt")
            stateManager.onCursorChanged(3, 5)
            stateManager.onScrollChanged(10, 20)

            val saved = stateManager.saveState()
            assertNotNull(saved)
            assertEquals("content", saved!!.content)
            assertEquals(3, saved.cursorLine)
            assertEquals(5, saved.cursorColumn)
            assertEquals(10, saved.scrollX)
            assertEquals(20, saved.scrollY)
            assertEquals("kotlin", saved.language)
            assertEquals("Test.kt", saved.fileName)
        }

        @Test
        @DisplayName("restoreState 正确恢复编辑器状态")
        fun restoreState_restoresState() = runTest {
            val savedState = EditorSavedState(
                content = "restored content",
                cursorLine = 7,
                cursorColumn = 3,
                scrollX = 50,
                scrollY = 100,
                language = "python",
                fileName = "main.py"
            )

            stateManager.uiState.test {
                assertEquals(EditorUiState.Loading, awaitItem())

                stateManager.restoreState(savedState)

                val state = awaitItem()
                assertTrue(state is EditorUiState.Success)
                assertEquals("restored content", (state as EditorUiState.Success).content)
                assertEquals("python", state.language)
                assertEquals("main.py", state.fileName)
            }

            assertEquals(CursorPosition(7, 3), stateManager.cursorPosition.value)
            assertEquals(Pair(50, 100), stateManager.scrollPosition.value)
            assertFalse(stateManager.isModified.value)
        }
    }

    // ------------------------------------------------------------------
    // 大纲解析
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("大纲项解析")
    inner class OutlineTests {

        @Test
        @DisplayName("onOutlineItems 解析 JSON 并更新 outlineItems")
        fun onOutlineItems_parsesJson() = runTest {
            stateManager.outlineItems.test {
                assertEquals(emptyList<OutlineItem>(), awaitItem())

                val json = """[{"name":"main","line":1,"type":"function"},{"name":"MyClass","line":10,"type":"class"}]"""
                stateManager.onOutlineItems(json)

                val items = awaitItem()
                assertEquals(2, items.size)
                assertEquals("main", items[0].name)
                assertEquals(1, items[0].line)
                assertEquals("function", items[0].type)
                assertEquals("MyClass", items[1].name)
                assertEquals(10, items[1].line)
                assertEquals("class", items[1].type)
            }
        }

        @Test
        @DisplayName("onOutlineItems 无效 JSON 静默忽略")
        fun onOutlineItems_invalidJson_silentlyIgnored() = runTest {
            stateManager.outlineItems.test {
                assertEquals(emptyList<OutlineItem>(), awaitItem())
                stateManager.onOutlineItems("not valid json")
                // Should not crash, and no new emission
                expectNoEvents()
            }
        }
    }

    // Helper: use Kotlin's assert functions
    private fun <T> assertNotNull(actual: T?): T {
        assertTrue(actual != null, "Expected non-null value")
        return actual!!
    }

    private fun assertNull(actual: Any?) {
        assertTrue(actual == null, "Expected null but was $actual")
    }
}
