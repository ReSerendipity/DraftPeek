package com.draftpeek.feature.editor.viewmodel

import com.draftpeek.feature.editor.model.EditorUiState
import com.draftpeek.feature.editor.model.MarkdownTheme
import com.draftpeek.feature.editor.model.MarkdownViewMode
import com.draftpeek.feature.editor.tabs.TabManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("EditorStateManager")
class EditorStateManagerTest {

    private lateinit var tabManager: TabManager
    private lateinit var stateManager: EditorStateManager

    @BeforeEach
    fun setUp() {
        tabManager = TabManager()
        stateManager = EditorStateManager(tabManager)
        // P0-3: Disable debounce in tests so flushDirtyState commits immediately
        stateManager.dirtyDebounceMs = 0
    }

    @Nested
    @DisplayName("initial state")
    inner class InitialStateTest {

        @Test
        @DisplayName("uiState is Loading")
        fun initialStateIsLoading() {
            assertTrue(stateManager.uiState.value is EditorUiState.Loading)
        }

        @Test
        @DisplayName("isModified is false")
        fun initiallyNotModified() {
            assertFalse(stateManager.isModified.value)
        }

        @Test
        @DisplayName("cursorPosition is 1:1")
        fun initialCursorPosition() {
            assertEquals(1, stateManager.cursorPosition.value.line)
            assertEquals(1, stateManager.cursorPosition.value.column)
        }

        @Test
        @DisplayName("scrollPosition is 0:0")
        fun initialScrollPosition() {
            assertEquals(0, stateManager.scrollPosition.value.first)
            assertEquals(0, stateManager.scrollPosition.value.second)
        }

        @Test
        @DisplayName("outlineItems is empty")
        fun initialOutlineEmpty() {
            assertTrue(stateManager.outlineItems.value.isEmpty())
        }

        @Test
        @DisplayName("markdownViewMode is WYSIWYG")
        fun initialViewModeIsWysiwyg() {
            assertEquals(MarkdownViewMode.WYSIWYG, stateManager.markdownViewMode.value)
        }

        @Test
        @DisplayName("focusMode is false")
        fun focusModeOff() {
            assertFalse(stateManager.isFocusMode.value)
        }

        @Test
        @DisplayName("typewriterMode is false")
        fun typewriterModeOff() {
            assertFalse(stateManager.isTypewriterMode.value)
        }

        @Test
        @DisplayName("markdownTheme is DEFAULT")
        fun defaultTheme() {
            assertEquals(MarkdownTheme.DEFAULT, stateManager.markdownTheme.value)
        }
    }

    @Nested
    @DisplayName("loadContent()")
    inner class LoadContentTest {

        @Test
        @DisplayName("sets Success state with content")
        fun setsSuccessState() {
            stateManager.loadContent(
                content = "hello world",
                language = "kotlin",
                fileName = "test.kt"
            )
            val state = stateManager.uiState.value
            assertTrue(state is EditorUiState.Success)
            assertEquals("hello world", (state as EditorUiState.Success).content)
            assertEquals("kotlin", state.language)
            assertEquals("test.kt", state.fileName)
        }

        @Test
        @DisplayName("clears modified flag")
        fun clearsModified() {
            stateManager.loadContent("content", "kotlin", "test.kt")
            assertFalse(stateManager.isModified.value)
        }

        @Test
        @DisplayName("markdown file defaults to WYSIWYG mode")
        fun markdownDefaultsToWysiwyg() {
            stateManager.loadContent("# Hello", "markdown", "test.md")
            assertEquals(MarkdownViewMode.WYSIWYG, stateManager.markdownViewMode.value)
        }

        @Test
        @DisplayName("non-markdown file stays in EDIT mode")
        fun nonMarkdownStaysEdit() {
            stateManager.loadContent("val x = 1", "kotlin", "test.kt")
            assertEquals(MarkdownViewMode.EDIT, stateManager.markdownViewMode.value)
        }

        @Test
        @DisplayName("restores cursor position")
        fun restoresCursor() {
            stateManager.loadContent("content", "kotlin", "test.kt", restoreCursorLine = 5)
            assertEquals(5, stateManager.cursorPosition.value.line)
        }

        @Test
        @DisplayName("getCurrentContent returns loaded content")
        fun getCurrentContent() {
            stateManager.loadContent("test content", "kotlin", "test.kt")
            assertEquals("test content", stateManager.getCurrentContent())
        }

        @Test
        @DisplayName("getFileName returns loaded filename")
        fun getFileName() {
            stateManager.loadContent("content", "kotlin", "myfile.kt")
            assertEquals("myfile.kt", stateManager.getFileName())
        }

        @Test
        @DisplayName("getLanguage returns loaded language")
        fun getLanguage() {
            stateManager.loadContent("content", "python", "test.py")
            assertEquals("python", stateManager.getLanguage())
        }
    }

    @Nested
    @DisplayName("onContentChanged()")
    inner class ContentChangedTest {

        @Test
        @DisplayName("sets isModified when content differs from original")
        fun setsModified() {
            stateManager.loadContent("original", "kotlin", "test.kt")
            stateManager.onContentChanged("modified")
            stateManager.flushDirtyState()
            assertTrue(stateManager.isModified.value)
        }

        @Test
        @DisplayName("clears isModified when content matches original")
        fun clearsModified() {
            stateManager.loadContent("original", "kotlin", "test.kt")
            stateManager.onContentChanged("changed")
            stateManager.flushDirtyState()
            assertTrue(stateManager.isModified.value)

            stateManager.onContentChanged("original")
            stateManager.flushDirtyState()
            assertFalse(stateManager.isModified.value)
        }

        @Test
        @DisplayName("updates currentContent")
        fun updatesCurrentContent() {
            stateManager.loadContent("original", "kotlin", "test.kt")
            stateManager.onContentChanged("new content")
            assertEquals("new content", stateManager.getCurrentContent())
        }

        @Test
        @DisplayName("updates tab modified state")
        fun updatesTabModified() {
            val tabId = tabManager.openTab("file://test.kt", "test.kt", "kotlin")
            stateManager.loadContent("original", "kotlin", "test.kt")
            stateManager.onContentChanged("modified")
            stateManager.flushDirtyState()
            assertTrue(tabManager.tabs.value.find { it.id == tabId }?.isModified == true)
        }
    }

    @Nested
    @DisplayName("onContentSaved()")
    inner class ContentSavedTest {

        @Test
        @DisplayName("sets new baseline content")
        fun setsNewBaseline() {
            stateManager.loadContent("original", "kotlin", "test.kt")
            stateManager.onContentChanged("modified")
            stateManager.flushDirtyState()
            stateManager.onContentSaved("modified")

            // Now "modified" is the new baseline, changing back to "original" should set modified
            stateManager.onContentChanged("original")
            stateManager.flushDirtyState()
            assertTrue(stateManager.isModified.value)
        }

        @Test
        @DisplayName("clears isModified")
        fun clearsModified() {
            stateManager.loadContent("original", "kotlin", "test.kt")
            stateManager.onContentChanged("modified")
            stateManager.flushDirtyState()
            stateManager.onContentSaved("modified")
            assertFalse(stateManager.isModified.value)
        }
    }

    @Nested
    @DisplayName("cursor & scroll")
    inner class CursorScrollTest {

        @Test
        @DisplayName("onCursorChanged updates position")
        fun updatesCursor() {
            stateManager.onCursorChanged(10, 5)
            assertEquals(10, stateManager.cursorPosition.value.line)
            assertEquals(5, stateManager.cursorPosition.value.column)
        }

        @Test
        @DisplayName("onScrollChanged updates position")
        fun updatesScroll() {
            stateManager.onScrollChanged(100, 200)
            assertEquals(100, stateManager.scrollPosition.value.first)
            assertEquals(200, stateManager.scrollPosition.value.second)
        }

        @Test
        @DisplayName("CursorPosition.toDisplayString formats correctly")
        fun cursorDisplayString() {
            val pos = CursorPosition(5, 10)
            assertEquals("5:10", pos.toDisplayString())
        }
    }

    @Nested
    @DisplayName("outline parsing")
    inner class OutlineTest {

        @Test
        @DisplayName("parses single outline item from JSON")
        fun parsesSingleItem() {
            val json = """[{"name":"main","line":1,"type":"function"}]"""
            stateManager.onOutlineItems(json)
            assertEquals(1, stateManager.outlineItems.value.size)
            assertEquals("main", stateManager.outlineItems.value[0].name)
            assertEquals(1, stateManager.outlineItems.value[0].line)
            assertEquals("function", stateManager.outlineItems.value[0].type)
        }

        @Test
        @DisplayName("parses multiple outline items")
        fun parsesMultipleItems() {
            val json = """[{"name":"foo","line":1,"type":"function"},{"name":"bar","line":10,"type":"class"}]"""
            stateManager.onOutlineItems(json)
            assertEquals(2, stateManager.outlineItems.value.size)
        }

        @Test
        @DisplayName("handles invalid JSON gracefully (empty list)")
        fun handlesInvalidJson() {
            stateManager.onOutlineItems("invalid json")
            assertTrue(stateManager.outlineItems.value.isEmpty())
        }
    }

    @Nested
    @DisplayName("preview mode")
    inner class PreviewModeTest {

        @Test
        @DisplayName("togglePreviewMode cycles WYSIWYG → EDIT → PREVIEW → SPLIT for previewable files")
        fun cyclesPreviewMode() {
            stateManager.loadContent("# Hello", "markdown", "test.md")
            assertEquals(MarkdownViewMode.WYSIWYG, stateManager.markdownViewMode.value)

            stateManager.togglePreviewMode()
            assertEquals(MarkdownViewMode.EDIT, stateManager.markdownViewMode.value)

            stateManager.togglePreviewMode()
            assertEquals(MarkdownViewMode.PREVIEW, stateManager.markdownViewMode.value)

            stateManager.togglePreviewMode()
            assertEquals(MarkdownViewMode.SPLIT, stateManager.markdownViewMode.value)
        }

        @Test
        @DisplayName("togglePreviewMode does nothing for non-previewable files")
        fun noToggleForNonPreviewable() {
            stateManager.loadContent("val x = 1", "kotlin", "test.kt")
            stateManager.togglePreviewMode()
            assertEquals(MarkdownViewMode.EDIT, stateManager.markdownViewMode.value)
        }

        @Test
        @DisplayName("setMarkdownViewMode sets mode for previewable files")
        fun setViewMode() {
            stateManager.loadContent("# Hello", "markdown", "test.md")
            stateManager.setMarkdownViewMode(MarkdownViewMode.EDIT)
            assertEquals(MarkdownViewMode.EDIT, stateManager.markdownViewMode.value)
        }

        @Test
        @DisplayName("isPreviewMode is true when not in EDIT mode")
        fun isPreviewMode() {
            stateManager.loadContent("# Hello", "markdown", "test.md")
            assertTrue(stateManager.isPreviewMode)

            stateManager.setMarkdownViewMode(MarkdownViewMode.EDIT)
            assertFalse(stateManager.isPreviewMode)
        }
    }

    @Nested
    @DisplayName("focus & typewriter modes")
    inner class FocusTypewriterTest {

        @Test
        @DisplayName("toggleFocusMode toggles state")
        fun togglesFocusMode() {
            assertFalse(stateManager.isFocusMode.value)
            stateManager.toggleFocusMode()
            assertTrue(stateManager.isFocusMode.value)
            stateManager.toggleFocusMode()
            assertFalse(stateManager.isFocusMode.value)
        }

        @Test
        @DisplayName("toggleTypewriterMode toggles state")
        fun togglesTypewriterMode() {
            assertFalse(stateManager.isTypewriterMode.value)
            stateManager.toggleTypewriterMode()
            assertTrue(stateManager.isTypewriterMode.value)
            stateManager.toggleTypewriterMode()
            assertFalse(stateManager.isTypewriterMode.value)
        }
    }

    @Nested
    @DisplayName("saveState() & restoreState()")
    inner class SaveRestoreTest {

        @Test
        @DisplayName("saveState returns null when not loaded")
        fun saveStateNullWhenNotLoaded() {
            assertNull(stateManager.saveState())
        }

        @Test
        @DisplayName("saveState captures current state")
        fun saveStateCapturesState() {
            stateManager.loadContent("content", "kotlin", "test.kt")
            stateManager.onCursorChanged(5, 10)
            stateManager.onScrollChanged(50, 100)

            val saved = stateManager.saveState()
            assertNotNull(saved)
            assertEquals("content", saved?.content)
            assertEquals(5, saved?.cursorLine)
            assertEquals(10, saved?.cursorColumn)
            assertEquals(50, saved?.scrollX)
            assertEquals(100, saved?.scrollY)
        }

        @Test
        @DisplayName("restoreState restores content and cursor")
        fun restoreStateRestores() {
            val saved = EditorSavedState(
                content = "restored content",
                cursorLine = 3,
                cursorColumn = 7,
                scrollX = 10,
                scrollY = 20,
                language = "python",
                fileName = "test.py",
                isReadOnly = false
            )
            stateManager.restoreState(saved)

            val state = stateManager.uiState.value
            assertTrue(state is EditorUiState.Success)
            assertEquals("restored content", (state as EditorUiState.Success).content)
            assertEquals("python", state.language)
            assertEquals(3, stateManager.cursorPosition.value.line)
            assertEquals(7, stateManager.cursorPosition.value.column)
            assertFalse(stateManager.isModified.value)
        }
    }

    @Nested
    @DisplayName("setLoading() & setError()")
    inner class LoadingErrorTest {

        @Test
        @DisplayName("setLoading sets Loading state")
        fun setsLoading() {
            stateManager.loadContent("content", "kotlin", "test.kt")
            stateManager.setLoading()
            assertTrue(stateManager.uiState.value is EditorUiState.Loading)
        }

        @Test
        @DisplayName("setError sets Error state")
        fun setsError() {
            stateManager.setError("Something went wrong")
            val state = stateManager.uiState.value
            assertTrue(state is EditorUiState.Error)
            assertEquals("Something went wrong", (state as EditorUiState.Error).message)
        }
    }

    @Nested
    @DisplayName("isMarkdownFile & isPreviewable")
    inner class FilePropertiesTest {

        @Test
        @DisplayName("isMarkdownFile is true for .md files")
        fun isMarkdownFile() {
            stateManager.loadContent("# Hello", "markdown", "test.md")
            assertTrue(stateManager.isMarkdownFile)
        }

        @Test
        @DisplayName("isMarkdownFile is true for .markdown files")
        fun isMarkdownFileLongExt() {
            stateManager.loadContent("# Hello", "markdown", "test.markdown")
            assertTrue(stateManager.isMarkdownFile)
        }

        @Test
        @DisplayName("isMarkdownFile is false for .kt files")
        fun isNotMarkdownFile() {
            stateManager.loadContent("val x = 1", "kotlin", "test.kt")
            assertFalse(stateManager.isMarkdownFile)
        }

        @Test
        @DisplayName("isPreviewable is true for HTML files")
        fun htmlIsPreviewable() {
            stateManager.loadContent("<p>hello</p>", "html", "test.html")
            assertTrue(stateManager.isPreviewable)
        }
    }

    @Nested
    @DisplayName("setMarkdownTheme()")
    inner class ThemeTest {

        @Test
        @DisplayName("sets markdown theme")
        fun setsTheme() {
            stateManager.setMarkdownTheme(MarkdownTheme.GITHUB)
            assertEquals(MarkdownTheme.GITHUB, stateManager.markdownTheme.value)
        }
    }

    @Nested
    @DisplayName("P0-3 dirty-mark debounce")
    inner class DirtyDebounceTest {

        @Test
        @DisplayName("isModified is not set immediately after content change (debounce active)")
        fun dirtyNotSetImmediately() {
            stateManager.dirtyDebounceMs = 500L // restore real debounce
            stateManager.loadContent("original", "kotlin", "test.kt")
            stateManager.onContentChanged("modified")
            // isModified should NOT be set yet — debounce window hasn't elapsed
            assertFalse(stateManager.isModified.value)
        }

        @Test
        @DisplayName("flushDirtyState commits isModified after debounce elapses")
        fun flushCommitsAfterDebounce() {
            stateManager.dirtyDebounceMs = 0L // immediate commit
            stateManager.loadContent("original", "kotlin", "test.kt")
            stateManager.onContentChanged("modified")
            stateManager.flushDirtyState()
            assertTrue(stateManager.isModified.value)
        }

        @Test
        @DisplayName("pendingDirty is cancelled on save")
        fun pendingDirtyCancelledOnSave() {
            stateManager.dirtyDebounceMs = 500L
            stateManager.loadContent("original", "kotlin", "test.kt")
            stateManager.onContentChanged("modified")
            // pendingDirty is set but not flushed yet
            stateManager.onContentSaved("modified")
            // Even if we flush, there should be no pending update
            stateManager.flushDirtyState()
            assertFalse(stateManager.isModified.value)
        }

        @Test
        @DisplayName("pendingDirty is cancelled on loadContent")
        fun pendingDirtyCancelledOnLoad() {
            stateManager.dirtyDebounceMs = 500L
            stateManager.loadContent("original", "kotlin", "test.kt")
            stateManager.onContentChanged("modified")
            // Load new content — should cancel pending dirty
            stateManager.loadContent("new content", "python", "test.py")
            stateManager.flushDirtyState()
            assertFalse(stateManager.isModified.value)
        }

        @Test
        @DisplayName("content and liveContent update immediately despite debounce")
        fun contentUpdatesImmediately() {
            stateManager.dirtyDebounceMs = 500L
            stateManager.loadContent("original", "kotlin", "test.kt")
            stateManager.onContentChanged("modified")
            // Content should be updated immediately (not debounced)
            assertEquals("modified", stateManager.getCurrentContent())
            assertEquals("modified", stateManager.liveContent.value)
        }
    }
}
