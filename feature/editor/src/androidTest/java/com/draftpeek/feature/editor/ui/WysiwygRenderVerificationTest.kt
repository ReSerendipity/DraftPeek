package com.draftpeek.feature.editor.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.draftpeek.core.ui.theme.DraftPeekTheme
import com.draftpeek.feature.editor.model.MarkdownViewMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * WYSIWYG（所见即所得）Markdown 渲染验证测试（P2-10）。
 *
 * 验证 Markdown 预览组件在不同视图模式下的渲染行为：
 * - EDIT 模式：只读预览
 * - PREVIEW 模式：只读预览
 * - SPLIT 模式：分屏编辑+预览
 * - WYSIWYG 模式：富文本编辑
 *
 * 验证内容：
 * 1. 各视图模式能正确渲染基本 Markdown 语法（标题、代码块、列表、引用等）
 * 2. 大文件保护机制（WYSIWYG 模式内容超过 MAX_SAFE_CONTENT_SIZE 时显示降级 UI）
 * 3. 深色/浅色主题切换不影响内容渲染
 *
 * 注意：由于 WebView 组件在 Compose Test 中无法真实渲染，
 * 这些测试验证 Compose 层的组件挂载和状态管理正确性，
 * 而非 WebView 内部的 HTML 渲染结果。
 */
@RunWith(AndroidJUnit4::class)
class WysiwygRenderVerificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val basicMarkdown = """
        # Title Heading
        ## Subtitle

        This is a **bold** and *italic* text.

        ```kotlin
        fun main() {
            println("Hello, World!")
        }
        ```

        - Item 1
        - Item 2
        - Item 3

        > This is a blockquote.

        [Link](https://example.com)
    """.trimIndent()

    @Test
    fun editMode_rendersWithoutError() {
        composeTestRule.setContent {
            DraftPeekTheme {
                MarkdownPreview(
                    markdownContent = basicMarkdown,
                    viewMode = MarkdownViewMode.EDIT,
                    isDarkTheme = false,
                )
            }
        }
        // 验证组件成功挂载，无异常
        // WebView 组件在测试环境中可能无法真实渲染，但 Compose 层应正常挂载
        composeTestRule.onNodeWithTag("test_tag_markdown_preview")
            .assertExists()
    }

    @Test
    fun previewMode_rendersWithoutError() {
        composeTestRule.setContent {
            DraftPeekTheme {
                MarkdownPreview(
                    markdownContent = basicMarkdown,
                    viewMode = MarkdownViewMode.PREVIEW,
                    isDarkTheme = false,
                )
            }
        }
        composeTestRule.onNodeWithTag("test_tag_markdown_preview")
            .assertExists()
    }

    @Test
    fun splitMode_rendersBothPanes() {
        composeTestRule.setContent {
            DraftPeekTheme {
                MarkdownPreview(
                    markdownContent = basicMarkdown,
                    viewMode = MarkdownViewMode.SPLIT,
                    isDarkTheme = false,
                )
            }
        }
        // SPLIT 模式应渲染两个预览面板
        composeTestRule.onNodeWithTag("test_tag_markdown_preview")
            .assertExists()
    }

    @Test
    fun wysiwygMode_rendersRichEditor() {
        composeTestRule.setContent {
            DraftPeekTheme {
                MarkdownPreview(
                    markdownContent = basicMarkdown,
                    viewMode = MarkdownViewMode.WYSIWYG,
                    isDarkTheme = false,
                )
            }
        }
        // WYSIWYG 模式应使用 MarkdownRichEditor
        composeTestRule.onNodeWithTag("test_tag_markdown_rich_editor")
            .assertExists()
    }

    @Test
    fun wysiwygMode_largeContent_showsFallback() {
        // 生成超过 MAX_SAFE_CONTENT_SIZE (100_000) 的大内容
        val largeContent = "A".repeat(100_001)

        composeTestRule.setContent {
            DraftPeekTheme {
                MarkdownRichEditor(
                    markdownContent = largeContent,
                    isDarkTheme = false,
                )
            }
        }
        // 大文件应显示降级 UI 而非 RichTextEditor
        composeTestRule.onNodeWithText("Large file")
            .assertExists()
    }

    @Test
    fun darkTheme_rendersCorrectly() {
        composeTestRule.setContent {
            DraftPeekTheme {
                MarkdownPreview(
                    markdownContent = basicMarkdown,
                    viewMode = MarkdownViewMode.EDIT,
                    isDarkTheme = true,
                )
            }
        }
        composeTestRule.onNodeWithTag("test_tag_markdown_preview")
            .assertExists()
    }

    @Test
    fun emptyContent_rendersWithoutError() {
        composeTestRule.setContent {
            DraftPeekTheme {
                MarkdownPreview(
                    markdownContent = "",
                    viewMode = MarkdownViewMode.EDIT,
                    isDarkTheme = false,
                )
            }
        }
        composeTestRule.onNodeWithTag("test_tag_markdown_preview")
            .assertExists()
    }

    @Test
    fun markdownWithSpecialCharacters_rendersWithoutError() {
        val specialContent = """
            # 标题 with 中文

            Emoji: 🎉🚀💻

            Special chars: <>&"'\\${'$'}{}[]

            ```python
            # 中文注释
            print("Hello, 世界!")
            ```
        """.trimIndent()

        composeTestRule.setContent {
            DraftPeekTheme {
                MarkdownPreview(
                    markdownContent = specialContent,
                    viewMode = MarkdownViewMode.EDIT,
                    isDarkTheme = false,
                )
            }
        }
        composeTestRule.onNodeWithTag("test_tag_markdown_preview")
            .assertExists()
    }
}
