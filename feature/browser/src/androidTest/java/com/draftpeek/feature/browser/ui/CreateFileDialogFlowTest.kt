package com.draftpeek.feature.browser.ui

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.draftpeek.feature.browser.util.FileTemplateProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 「新建文件」真实链路的设备端功能测试。
 *
 * ## 为什么单独有这条
 * Macrobenchmark 的性能腿已改为消费 CI 预置的种子文件直接进编辑器（原因写在
 * `benchmark/src/androidTest/.../EditorBenchmarkSupport.kt`），它**不再经过**新建文件的 UI 链。
 * 新建链路的回归必须由本文件守住 —— 用 preseed 冒充"新建已被覆盖"是假的覆盖。
 *
 * ## 守的是把性能腿拖红的那一段
 * 主按钮 `enabled = filename.isNotBlank()`（`CreateFileDialog.kt:198`），onClick 内部还有一次
 * `if (filename.isNotBlank())` 二次判空：没名字时点它不会创建任何东西。run 36120757720 里
 * "点了菜单项后 15s 等不到 CodeEditor"就是链路停在这里。
 *
 * ## 三档实测定下来的三条框架事实（都写进过红里，不是推的）
 * 1. 按钮必须取**合并树**节点。`useUnmergedTree = true` 命中的是 Button 里的 Text 叶子
 *    （run 36160995483 转储：Actions 只有 SetTextSubstitution 那几个，既无 ClickAction 也无 Disabled）；
 *    API 34 上点叶子靠坐标转发侥幸命中父级，API 30/26 上静默无效。
 * 2. `performTextInput` 走语义 `SetTextAction`，在应用进程内直接改状态、不经软键盘窗口，
 *    所以"输入"这步在无头档可靠 —— 与"macro 要在真 UI 上打字"不是一回事。
 * 3. **语义节点会先于布局出现**。run `36215197822` 的取证行显示红的那一次
 *    `boundsInRoot=[0,0,0,0]` 而 `screen=1080x2208`、未超出屏幕下沿 ⇒ `ModalBottomSheet`
 *    刚把节点登记进语义树、还没测量；此时 `performClick` 点在零面积矩形上，等于没点。
 *    同一条断言在相邻两次 run 里一红一绿 ⇒ 这是时序抖动，不是按屏幕高度分档的稳定缺陷。
 *
 * ## 因此进门先等"落定"，且重试必须可见
 * [awaitButton] 轮询到按钮节点**有非零面积**为止，最多重试一次；每次重试都打一行
 * `DRAFTPEEK_UI RETRY`（带原因与实测几何）。**不用 assume 跳过** —— 跳过会把抖动藏成"没发生"。
 *
 * ## 覆盖边界（不含糊过去）
 * 只覆盖对话框的输入 → 创建回调这一段。"MainActivity 点 FAB → 置 showCreateFileDialog" 与
 * "onCreate → SnippetViewModel → 导航到 CodeEditor" 两头仍无设备断言：前者在 FileBrowserScreen
 * 内（默认参数 `hiltViewModel()`，本模块没有 hilt-android-testing），后者要 `:app` 的 instrumented
 * 变体，而 CI 跑 `connectedDebugAndroidTest` 时 `:app` 因 flavor 命名被静默跳过。
 */
class CreateFileDialogFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** 「创建并打开」在五种语言下的文案；CI 的 en 档取 "Create and open"。 */
    private val createLabels =
        listOf("创建并打开", "Create and open", "作成して開く", "만들고 열기", "建立並開啟")

    private val screen = Rect().also { rect ->
        val dm = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics
        rect.set(0, 0, dm.widthPixels, dm.heightPixels)
    }

    /** 合并树里的按钮节点（见类注释事实 1）；语义树里还没有它就返回 null。 */
    private fun buttonNodeOrNull() = createLabels.asSequence()
        .map { label -> composeTestRule.onAllNodesWithText(label) }
        .firstOrNull { it.fetchSemanticsNodes().isNotEmpty() }
        ?.get(0)

    /**
     * 等按钮节点拿到非零面积再动手。
     *
     * 最多两次尝试（首次 + 一次重试），每次失败都打一行 `DRAFTPEEK_UI RETRY` 并带上原因与实测
     * 几何 —— 跳过与静默重试都会把抖动藏起来，只有留下行才能在 CI 产物里复盘第 3 条事实。
     */
    /**
     * 等到按钮节点出现在语义树里（最多重试一次，重试必须留可见行）。
     *
     * 刻意**不**把"boundsInRoot 非零"当门禁：c703dc4 加了那道门槛后 API 30/26 变成必然红
     * （`等「创建并打开」完成布局失败 … boundsInRoot=[0,0,0,0]`），而同一节点在 `96b1fe0` 上
     * 直接 `performClick` 是能打通回调的（那次 API 30 绿）。⇒ 零面积是这个时刻的**观测产物**，
     * 不是"没落定"的可靠判据。几何值继续只在取证行里报告，不参与门禁判断。
     */
    private fun awaitButton(): androidx.compose.ui.test.SemanticsNodeInteraction {
        repeat(2) { attempt ->
            buttonNodeOrNull()?.let { node ->
                if (attempt > 0) log("RETRY 第 2 次取到节点 ${describe(boundsOf(node))}")
                return node
            }
            log("RETRY 第 ${attempt + 1}/2 次：语义树里还没有该节点（语言候选全数落空）| ${geometry()}")
            composeTestRule.waitForIdle()
        }
        throw AssertionError(
            "重试一次后语义树里仍没有「创建并打开」节点；语言候选=${createLabels.joinToString()} | ${geometry()}"
        )
    }

    private fun boundsOf(node: androidx.compose.ui.test.SemanticsNodeInteraction) =
        runCatching { node.fetchSemanticsNode().boundsInRoot }.getOrNull()

    private fun log(line: String) {
        println("DRAFTPEEK_UI $line")
        Log.i(TAG, line)
    }

    private fun describe(bounds: androidx.compose.ui.geometry.Rect?) =
        bounds?.let { "[l=${it.left},t=${it.top},r=${it.right},b=${it.bottom}]" } ?: "(取不到)"

    private fun nameField() = composeTestRule.onNode(hasSetTextAction())

    /**
     * 取证行：屏幕尺寸 + 节点几何 + IME 状态，用来把三种"点了没反应"分开：
     * 零面积（未测量）、超出屏幕下沿（真被裁切）、`imeAcceptingText=true` 且几何正常（可能被 IME 遮）。
     * `dumpsys input_method` 是试探性的：测试进程没有 `android.permission.DUMP` 时把失败原文一并打出来，
     * 免得把"权限不够"读成"IME 没弹"。
     */
    private fun geometry(): String {
        val node = buttonNodeOrNull()
        val bounds = node?.let { runCatching { it.fetchSemanticsNode().boundsInRoot }.getOrNull() }
        val imm = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val dumpsys = runCatching {
            val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", "dumpsys input_method"))
            proc.inputStream.bufferedReader().use { reader ->
                reader.readLines().filter { line -> line.contains("mInputShown") }
                    .joinToString(";").ifEmpty { "(输出里没有 mInputShown 行)" }
            }
        }.getOrElse { t -> "dumpsys 不可用(${t.javaClass.simpleName})" }
        return "screen=${screen.width()}x${screen.height()} boundsInRoot=" + describe(bounds) +
            " 超出屏幕下沿=" + (bounds?.let { it.bottom > screen.height() }?.toString() ?: "?") +
            " imeAcceptingText=${imm.isAcceptingText} $dumpsys"
    }

    @Test
    fun createButtonIsDisabledUntilFilenameEntered() {
        var created = 0
        composeTestRule.setContent {
            CreateFileDialog(onDismiss = {}, onCreate = { _, _, _ -> created++ })
        }
        val node = awaitButton()
        node.assertIsDisplayed()

        // Disabled 语义挂在哪个节点上不是我能本机断定的框架事实，所以既记 assertIsNotEnabled
        // 的结果，也记 performClick 是否被拒；两个都不成立时把实测值全打出来。
        val reportedNotEnabled = runCatching { node.assertIsNotEnabled() }.isSuccess
        val clickRejected = runCatching { node.performClick() }.isFailure
        assertTrue(
            "未输文件名时不该可能触发创建。实测：assertIsNotEnabled 通过=$reportedNotEnabled、" +
                "performClick 被拒=$clickRejected、onCreate 调用次数=$created | ${geometry()}",
            (reportedNotEnabled || clickRejected) && created == 0
        )
    }

    @Test
    fun typedFilenameDrivesCreateCallbackWithKotlinEmptyTemplate() {
        var captured: Triple<String, String, String>? = null
        var dismissed = 0
        composeTestRule.setContent {
            CreateFileDialog(
                onDismiss = { dismissed++ },
                onCreate = { name, language, content -> captured = Triple(name, language, content) }
            )
        }
        // 这里只等"落定"，不在输名前断言可用：空文件名时主按钮本就该 disabled，
        // 提前 assertIsEnabled 会让本用例在三档上必然红（49dddbe 正是这么红的，
        // run 36220797005 报 `Failed to assert the following: (is enabled)`）。
        awaitButton()

        nameField().performTextInput("  bench_flow  ")
        val button = awaitButton()
        button.assertIsEnabled()
        button.performClick()

        // 第一次没回调就再点一次，并把 RETRY 行打出来：这是"重试"而不是"放宽判据"——
        // 第二次仍不回调就照旧红，且红里带着两次的几何与 dismissed 计数。
        if (captured == null) {
            log("RETRY 首次 performClick 未触发 onCreate（dismissed=$dismissed），重取节点再点一次 | ${geometry()}")
            composeTestRule.waitForIdle()
            awaitButton().performClick()
        }

        // 失败时把 dismissed 一起报出来：若 onDismiss 被调用过，说明这一拍其实打在 scrim 上
        // （表面试关掉了），与"点在零面积矩形上没落地"是两种不同的成因，别混为一谈。
        val result = requireNotNull(captured) {
            "点了「创建并打开」却没回调 onCreate | dismissed=$dismissed | ${geometry()}"
        }
        assertEquals("对话框不该在这次交互里被关掉", 0, dismissed)
        // 对话框只交修剪过的裸名，扩展名由上层按语言补（CreateFileDialog.kt:235-239）
        assertEquals("bench_flow", result.first)
        assertEquals("默认语言应是 Kotlin", "Kotlin", result.second)
        assertEquals(
            "默认模板应是空模板",
            FileTemplateProvider.getEmptyTemplate(result.second, "kt"),
            result.third
        )
        assertTrue("空模板内容不该凭空长出代码", result.third.isBlank() || result.third.length < 40)
    }

    private companion object {
        const val TAG = "DraftPeekUI"
    }
}
