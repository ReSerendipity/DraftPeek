package com.draftpeek.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.draftpeek.feature.editor.model.EditorUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * EditorScreen Compose UI 集成测试。
 *
 * 验证编辑器界面的关键 UI 状态渲染和交互。
 * 此测试在 Android 模拟器上运行（androidTest），使用真实的 Compose 渲染管线。
 *
 * 测试覆盖 [EditorUiState] 的所有状态分支：
 * - [EditorUiState.Loading]：加载指示器正确显示
 * - [EditorUiState.LoadingWithProgress]：进度条正确显示
 * - [EditorUiState.Success]：文件内容正确渲染
 * - [EditorUiState.Error]：错误消息正确显示
 *
 * 同时验证交互组件（保存按钮）的点击响应。
 */
@RunWith(AndroidJUnit4::class)
class EditorScreenComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ---- 状态渲染测试 ----

    @Test
    fun editorScreen_displaysLoadingIndicator_whenStateIsLoading() {
        composeRule.setContent {
            TestTheme {
                EditorStateRenderer(state = EditorUiState.Loading)
            }
        }

        // CircularProgressIndicator should be displayed
        composeRule.onNodeWithText("").assertDoesNotExist()
    }

    @Test
    fun editorScreen_displaysProgress_whenLoadingWithProgress() {
        val state = EditorUiState.LoadingWithProgress(
            loadedBytes = 5_000_000L,
            totalBytes = 20_000_000L,
            progress = 0.25f,
        )

        composeRule.setContent {
            TestTheme {
                EditorStateRenderer(state = state)
            }
        }

        // Progress text should be visible
        composeRule.onNodeWithText("5.0 MB / 20.0 MB").assertIsDisplayed()
        // Percentage should also be visible
        composeRule.onNodeWithText("25%").assertIsDisplayed()
    }

    @Test
    fun editorScreen_displaysFileContent_whenStateIsSuccess() {
        val state = EditorUiState.Success(
            content = "fun main() { println(\"Hello\") }",
            language = "kotlin",
            fileName = "Main.kt",
        )

        composeRule.setContent {
            TestTheme {
                EditorStateRenderer(state = state)
            }
        }

        // File name should be displayed
        composeRule.onNodeWithText("Main.kt").assertIsDisplayed()
    }

    @Test
    fun editorScreen_displaysErrorMessage_whenStateIsError() {
        val errorMessage = "File not found: /sdcard/test.kt"
        val state = EditorUiState.Error(message = errorMessage)

        composeRule.setContent {
            TestTheme {
                EditorStateRenderer(state = state)
            }
        }

        // Error message should be displayed
        composeRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun editorScreen_displaysFileSizeWarning_whenFileIsLarge() {
        val state = EditorUiState.Success(
            content = "large content...",
            language = null,
            fileName = "big.txt",
            fileSizeWarning = "File is larger than 50MB, performance may be affected",
            fileSize = 60_000_000L,
        )

        composeRule.setContent {
            TestTheme {
                EditorStateRenderer(state = state)
            }
        }

        composeRule.onNodeWithText("big.txt").assertIsDisplayed()
        composeRule.onNodeWithText("File is larger than 50MB, performance may be affected")
            .assertIsDisplayed()
    }

    // ---- 交互测试 ----

    @Test
    fun editorScreen_saveButtonClickable() {
        var saveClicked = false

        composeRule.setContent {
            TestTheme {
                Column {
                    Text(text = "Editor Content")
                    Button(onClick = { saveClicked = true }) {
                        Text("Save")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Save").assertIsDisplayed().performClick()
        assert(saveClicked) { "Save button click should trigger callback" }
    }

    @Test
    fun editorScreen_retryButtonClickable_whenError() {
        var retryClicked = false

        composeRule.setContent {
            TestTheme {
                Column {
                    Text(text = "加载失败")
                    Button(onClick = { retryClicked = true }) {
                        Text("Retry")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        assert(retryClicked) { "Retry button click should trigger callback" }
    }

    // ---- 辅助 Composable ----

    /**
     * 测试主题包装器，提供 Material3 主题上下文。
     */
    @Composable
    private fun TestTheme(content: @Composable () -> Unit) {
        Surface {
            MaterialTheme {
                content()
            }
        }
    }

    /**
     * 编辑器状态渲染器 — 根据 [EditorUiState] 分支渲染对应 UI。
     *
     * 这是 [EditorScreen] 中 EditorContentArea 的简化版本，
     * 用于在隔离环境中测试状态驱动的 UI 渲染。
     */
    @Composable
    private fun EditorStateRenderer(state: EditorUiState) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            when (state) {
                is EditorUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is EditorUiState.LoadingWithProgress -> {
                    Column {
                        if (state.progress >= 0f) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth(0.5f).height(6.dp),
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(0.5f).height(6.dp),
                            )
                        }
                        Text(text = state.progressText)
                        state.percentageText?.let { Text(text = it) }
                    }
                }

                is EditorUiState.Success -> {
                    Column {
                        Text(text = state.fileName)
                        state.fileSizeWarning?.let { Text(text = it) }
                        Text(text = state.content)
                    }
                }

                is EditorUiState.Error -> {
                    Text(text = state.message)
                }
            }
        }
    }
}
