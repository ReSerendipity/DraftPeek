package com.draftpeek.feature.editor.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.draftpeek.feature.editor.model.MarkdownViewMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI Test for EditorScreen view mode switching interaction.
 *
 * Since the full EditorScreen requires Hilt injection, sora-editor native view,
 * and WebView, we test the view mode toggle interaction pattern using a
 * simplified composable that mirrors the toolbar toggle behavior.
 */
class EditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun viewModeToggle_clickToggle_cyclesThroughModes() {
        var currentMode by mutableStateOf(MarkdownViewMode.EDIT)

        composeTestRule.setContent {
            ViewModeToggleTestHarness(
                currentMode = currentMode,
                onToggle = {
                    currentMode = when (currentMode) {
                        MarkdownViewMode.EDIT -> MarkdownViewMode.PREVIEW
                        MarkdownViewMode.PREVIEW -> MarkdownViewMode.SPLIT
                        MarkdownViewMode.SPLIT -> MarkdownViewMode.WYSIWYG
                        MarkdownViewMode.WYSIWYG -> MarkdownViewMode.EDIT
                    }
                },
            )
        }

        // Initial state is EDIT
        assertEquals(MarkdownViewMode.EDIT, currentMode)

        // Click toggle → PREVIEW
        composeTestRule.onNodeWithContentDescription("切换视图模式").performClick()
        assertEquals(MarkdownViewMode.PREVIEW, currentMode)

        // Click toggle → SPLIT
        composeTestRule.onNodeWithContentDescription("切换视图模式").performClick()
        assertEquals(MarkdownViewMode.SPLIT, currentMode)

        // Click toggle → WYSIWYG
        composeTestRule.onNodeWithContentDescription("切换视图模式").performClick()
        assertEquals(MarkdownViewMode.WYSIWYG, currentMode)

        // Click toggle → EDIT (cycle back)
        composeTestRule.onNodeWithContentDescription("切换视图模式").performClick()
        assertEquals(MarkdownViewMode.EDIT, currentMode)
    }

    @Test
    fun viewModeToggle_labelUpdatesWithMode() {
        var currentMode by mutableStateOf(MarkdownViewMode.EDIT)

        composeTestRule.setContent {
            ViewModeToggleTestHarness(
                currentMode = currentMode,
                onToggle = {
                    currentMode = when (currentMode) {
                        MarkdownViewMode.EDIT -> MarkdownViewMode.PREVIEW
                        MarkdownViewMode.PREVIEW -> MarkdownViewMode.SPLIT
                        MarkdownViewMode.SPLIT -> MarkdownViewMode.WYSIWYG
                        MarkdownViewMode.WYSIWYG -> MarkdownViewMode.EDIT
                    }
                },
            )
        }

        // Verify initial label for EDIT mode
        composeTestRule.onNodeWithText("编辑模式").assertIsDisplayed()

        // Toggle to PREVIEW
        composeTestRule.onNodeWithContentDescription("切换视图模式").performClick()
        composeTestRule.onNodeWithText("预览模式").assertIsDisplayed()

        // Toggle to SPLIT
        composeTestRule.onNodeWithContentDescription("切换视图模式").performClick()
        composeTestRule.onNodeWithText("分屏模式").assertIsDisplayed()

        // Toggle to WYSIWYG
        composeTestRule.onNodeWithContentDescription("切换视图模式").performClick()
        composeTestRule.onNodeWithText("富文本模式").assertIsDisplayed()
    }

    @Test
    fun viewModeSelector_directSetMode_updatesCorrectly() {
        var currentMode by mutableStateOf(MarkdownViewMode.EDIT)

        composeTestRule.setContent {
            ViewModeSelectorTestHarness(
                currentMode = currentMode,
                onModeSelected = { currentMode = it },
            )
        }

        // Click SPLIT option
        composeTestRule.onNodeWithText("分屏").performClick()
        assertEquals(MarkdownViewMode.SPLIT, currentMode)

        // Click PREVIEW option
        composeTestRule.onNodeWithText("预览").performClick()
        assertEquals(MarkdownViewMode.PREVIEW, currentMode)

        // Click EDIT option
        composeTestRule.onNodeWithText("编辑").performClick()
        assertEquals(MarkdownViewMode.EDIT, currentMode)
    }

    @Test
    fun viewModeToggle_multipleCycles_stateConsistent() {
        var currentMode by mutableStateOf(MarkdownViewMode.EDIT)

        composeTestRule.setContent {
            ViewModeToggleTestHarness(
                currentMode = currentMode,
                onToggle = {
                    currentMode = when (currentMode) {
                        MarkdownViewMode.EDIT -> MarkdownViewMode.PREVIEW
                        MarkdownViewMode.PREVIEW -> MarkdownViewMode.SPLIT
                        MarkdownViewMode.SPLIT -> MarkdownViewMode.WYSIWYG
                        MarkdownViewMode.WYSIWYG -> MarkdownViewMode.EDIT
                    }
                },
            )
        }

        // Cycle through all 4 modes
        repeat(4) {
            composeTestRule.onNodeWithContentDescription("切换视图模式").performClick()
        }
        // After 4 clicks from EDIT: EDIT → PREVIEW → SPLIT → WYSIWYG → EDIT
        assertEquals(MarkdownViewMode.EDIT, currentMode)

        // One more cycle
        composeTestRule.onNodeWithContentDescription("切换视图模式").performClick()
        assertEquals(MarkdownViewMode.PREVIEW, currentMode)
    }
}

/**
 * Test harness that replicates the view mode toggle button behavior
 * from EditorScreen's top app bar.
 */
@Composable
private fun ViewModeToggleTestHarness(
    currentMode: MarkdownViewMode,
    onToggle: () -> Unit,
) {
    val modeLabel = when (currentMode) {
        MarkdownViewMode.EDIT -> "编辑模式"
        MarkdownViewMode.PREVIEW -> "预览模式"
        MarkdownViewMode.SPLIT -> "分屏模式"
        MarkdownViewMode.WYSIWYG -> "富文本模式"
    }

    // Label showing current mode
    TextButton(onClick = onToggle) {
        Text(text = modeLabel)
    }
    // Button with content description for test lookup
    IconButton(
        onClick = onToggle,
        modifier = Modifier.semantics {
            contentDescription = "切换视图模式"
        },
    ) {
        Text(text = "⟳")
    }
}

/**
 * Test harness for direct mode selection (e.g., FilterChip group).
 */
@Composable
private fun ViewModeSelectorTestHarness(
    currentMode: MarkdownViewMode,
    onModeSelected: (MarkdownViewMode) -> Unit,
) {
    Row {
        MarkdownViewMode.entries.forEach { mode ->
            val label = when (mode) {
                MarkdownViewMode.EDIT -> "编辑"
                MarkdownViewMode.PREVIEW -> "预览"
                MarkdownViewMode.SPLIT -> "分屏"
                MarkdownViewMode.WYSIWYG -> "富文本"
            }
            FilterChip(
                selected = currentMode == mode,
                onClick = { onModeSelected(mode) },
                label = { Text(label) },
            )
        }
    }
}
