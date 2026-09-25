package com.draftpeek.feature.browser.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.draftpeek.feature.browser.util.FileTemplateProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 「新建文件」真实链路的设备端功能测试。
 *
 * ## 为什么单独有这条
 * Macrobenchmark 的性能腿已改为消费 CI 预置的种子文件直接进编辑器
 * （`benchmark/src/androidTest/.../EditorBenchmarkSupport.kt` 的注释里记了原因），
 * 它**不再经过**新建文件的 UI 链。新建链路的回归必须由本文件守住，
 * 两边不可互相冒充 —— 用 preseed 冒充"新建已被覆盖"是假的覆盖。
 *
 * ## 守的正是把性能腿拖红的那一段
 * 主按钮 `enabled = filename.isNotBlank()`（`CreateFileDialog.kt:198`），且 onClick 内部
 * 还有一次 `if (filename.isNotBlank())` 二次判空：没名字时点它不会创建任何东西。
 * run 36120757720 里"点了菜单项后 15s 等不到 CodeEditor"就是因为链路停在这里。
 *
 * ## 为什么这里的输入在无头档可靠（与 macro 走 IME 的区别）
 * `performTextInput` 触发的是 Compose 语义树里的 `SetTextAction`，**在应用进程内直接改
 * 文本状态**，不经过软键盘窗口与 IME 绑定；所以本测试不受 `-no-window` 下没有 IME 的影响。
 * 而 macro 若要在真 UI 上打字，需要的是另一回事（输入法可见性、焦点窗口），风险不同。
 *
 * ## 覆盖边界（不含糊过去）
 * 本文件覆盖 `CreateFileDialog` 的输入 → 创建回调这一段。它**不**覆盖
 * "MainActivity 上点 FAB → 菜单项把 showCreateFileDialog 置 true" 与
 * "onCreate → SnippetViewModel → 导航到 CodeEditor" 两头：前者在 `FileBrowserScreen`
 * 内（默认参数是 `hiltViewModel()`，本模块无 hilt-android-testing），后者要 `:app` 的
 * instrumented 变体 —— 而 CI 跑的是 `connectedDebugAndroidTest`，`:app` 有
 * dev/staging/production flavor，聚合任务匹配不到它、会被静默跳过
 * （需 `connectedProductionDebugAndroidTest`）。那两头的缺口是既有结构性问题，不属本 PR。
 */
class CreateFileDialogFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** 「创建并打开」在五种语言下的文案；CI 的 en 档取 "Create and open"。 */
    private val createLabels =
        listOf("创建并打开", "Create and open", "作成して開く", "만들고 열기", "建立並開啟")

    private fun createButton() = createLabels.asSequence()
        .map { label -> composeTestRule.onAllNodesWithText(label, useUnmergedTree = true) }
        .firstOrNull { it.fetchSemanticsNodes().isNotEmpty() }
        ?.get(0)
        ?: error("对话框里找不到「创建并打开」按钮（已尝试 ${createLabels.joinToString()}）")

    private fun nameField() = composeTestRule.onNode(hasSetTextAction())

    @Test
    fun createButtonIsDisabledUntilFilenameEntered() {
        var created = 0
        composeTestRule.setContent {
            CreateFileDialog(onDismiss = {}, onCreate = { _, _, _ -> created++ })
        }

        createButton().assertIsDisplayed()
        createButton().assertIsNotEnabled()
        assertEquals("未输文件名前不应发生创建", 0, created)
    }

    @Test
    fun typedFilenameDrivesCreateCallbackWithKotlinEmptyTemplate() {
        var captured: Triple<String, String, String>? = null
        composeTestRule.setContent {
            CreateFileDialog(onDismiss = {}, onCreate = { name, language, content ->
                captured = Triple(name, language, content)
            })
        }

        val typed = "  bench_flow  "
        nameField().performTextInput(typed)
        createButton().assertIsEnabled()
        createButton().performClick()

        val result = requireNotNull(captured) { "点了「创建并打开」却没回调 onCreate" }
        // 对话框自己只交修剪过的裸名，扩展名由上层按语言补（CreateFileDialog.kt:235-239）
        assertEquals("bench_flow", result.first)
        assertEquals("默认语言应是 Kotlin", "Kotlin", result.second)
        assertEquals(
            "默认模板应是空模板",
            FileTemplateProvider.getEmptyTemplate(result.second, "kt"),
            result.third
        )
        assertTrue("空模板内容不该凭空长出代码", result.third.isBlank() || result.third.length < 40)
    }
}
