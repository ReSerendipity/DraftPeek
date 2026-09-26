package com.draftpeek.feature.browser.ui

import android.content.Context
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
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
 * ## 三档实测定下来的框架事实（都写进过红里，不是推的）
 * 1. 按钮必须取**合并树**节点。`useUnmergedTree = true` 命中的是 Button 里的 Text 叶子
 *    （run 36160995483 转储：Actions 只有 SetTextSubstitution 那几个，既无 ClickAction 也无 Disabled）。
 * 2. `performTextInput` 走语义 `SetTextAction`，在应用进程内直接改状态、不经软键盘窗口。
 * 3. 本 compose 版本的 `SemanticsNode` **没有** `visibleBounds` / `unclippedBoundsInWindow`
 *    （javap 实测），可用的几何读数是 `boundsInRoot / boundsInWindow / touchBoundsInRoot /
 *    positionOnScreen` —— 取证行报这四个。
 *
 * ## 不 assume 跳过，也不拿几何读数当门禁
 * `c703dc4` 曾把"boundsInRoot 非零"当前置，结果 API 30/26 变必然红；而 `96b1fe0` 上同一个
 * 零面积节点直接 `performClick` 是能打通回调的 ⇒ 零面积只是那一刻的观测产物，只报告不否决。
 *
 * ## API 26 点击不回调（#106，本文件的最后一轮定位）
 * 剔除我自身 bug 的两个 sha 上稳定复现：`96b1fe0`、`2a1c43b` 都是 API 26 红、API 30/34 绿。
 * 三路取证：A `performClick()`；B `performTouchInput { click() }`（另一条注入路径）；
 * C 直投语义 `ClickAction` —— **C 触发的回调不算通过**（trigger 记为 probe），它只回答
 * "坐标打不到"还是"控件本身没接上"。判据不变：必须由真手势（A 或 B）触发 onCreate。
 */
class CreateFileDialogFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** 「创建并打开」在五种语言下的文案；CI 的 en 档取 "Create and open"。 */
    private val createLabels =
        listOf("创建并打开", "Create and open", "作成して開く", "만들고 열기", "建立並開啟")

    /** 语义节点出现后，等它连续两轮读数一致再动手（布局稳定）。 */
    private val settleRounds = 3

    private val metrics =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics

    /** 合并树里的按钮节点（见类注释事实 1）；语义树里还没有它就返回 null。 */
    private fun buttonNodeOrNull() = createLabels.asSequence()
        .map { label -> composeTestRule.onAllNodesWithText(label) }
        .firstOrNull { it.fetchSemanticsNodes().isNotEmpty() }
        ?.get(0)

    /** 等到按钮节点进语义树，并等几何读数稳定；重试至多一次，且每次重试留可见行。 */
    private fun awaitButton(): androidx.compose.ui.test.SemanticsNodeInteraction {
        repeat(2) { attempt ->
            val node = buttonNodeOrNull()
            if (node != null) {
                awaitStableGeometry(node)
                if (attempt > 0) log("RETRY 第 2 次才取到节点 | ${geometry(node)}")
                return node
            }
            log("RETRY 第 ${attempt + 1}/2 次：语义树里还没有该节点（语言候选全数落空）")
            composeTestRule.waitForIdle()
        }
        throw AssertionError(
            "重试一次后语义树里仍没有「创建并打开」节点；语言候选=${createLabels.joinToString()}"
        )
    }

    /** 连续 [settleRounds] 轮读数一致就收；仍在变就逐行打出变化，最后再试一次 performScrollTo。 */
    private fun awaitStableGeometry(node: androidx.compose.ui.test.SemanticsNodeInteraction) {
        var previous = geometry(node)
        repeat(settleRounds) {
            composeTestRule.waitForIdle()
            val current = geometry(node)
            if (current == previous) return
            log("布局仍在变：$previous → $current")
            previous = current
        }
        runCatching { node.performScrollTo() }
            .onFailure { t ->
                log("performScrollTo 不可用(${t.javaClass.simpleName}:${t.message})，用当前读数继续")
            }
        composeTestRule.waitForIdle()
    }

    private fun log(line: String) {
        println("DRAFTPEEK_UI $line")
        Log.i(TAG, line)
    }

    private fun rect(r: androidx.compose.ui.geometry.Rect?) =
        r?.let { "[l=${it.left},t=${it.top},r=${it.right},b=${it.bottom}]" } ?: "(取不到)"

    private fun nameField() = composeTestRule.onNode(hasSetTextAction())

    /**
     * 取证行：四个几何读数 + 屏幕与密度 + IME 状态。
     *
     * `dumpsys input_method` 是试探性的 —— 测试进程没有 `android.permission.DUMP` 时把失败原文
     * 一并打出来，免得把"权限不够"读成"IME 没弹"。
     */
    private fun geometry(node: androidx.compose.ui.test.SemanticsNodeInteraction?): String {
        val sn = (node ?: buttonNodeOrNull())
            ?.let { runCatching { it.fetchSemanticsNode() }.getOrNull() }
        val imm = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val dumpsys = runCatching {
            val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", "dumpsys input_method"))
            proc.inputStream.bufferedReader().use { reader ->
                reader.readLines().filter { line -> line.contains("mInputShown") }
                    .joinToString(";").ifEmpty { "(输出里没有 mInputShown 行)" }
            }
        }.getOrElse { t -> "dumpsys 不可用(${t.javaClass.simpleName})" }
        return "screen=${metrics.widthPixels}x${metrics.heightPixels} dpi=${metrics.densityDpi}" +
            " boundsInRoot=${rect(sn?.boundsInRoot)} boundsInWindow=${rect(sn?.boundsInWindow)}" +
            " touchBoundsInRoot=${rect(sn?.touchBoundsInRoot)} posOnScreen=${sn?.positionOnScreen}" +
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
                "performClick 被拒=$clickRejected、onCreate 调用次数=$created | ${geometry(node)}",
            (reportedNotEnabled || clickRejected) && created == 0
        )
    }

    @Test
    fun typedFilenameDrivesCreateCallbackWithKotlinEmptyTemplate() {
        var probing = false
        var captured: Triple<String, String, String>? = null
        var trigger = "无"
        var dismissed = 0
        composeTestRule.setContent {
            CreateFileDialog(
                onDismiss = { dismissed++ },
                onCreate = { name, language, content ->
                    captured = Triple(name, language, content)
                    trigger = if (probing) "probe" else "gesture"
                }
            )
        }
        // 只等落定，不在输名前断言可用：空文件名时主按钮本该 disabled
        // （49dddbe 就是那么红的，run 36220797005 报 `Failed to assert (is enabled)`）。
        awaitButton()

        nameField().performTextInput("  bench_flow  ")
        val button = awaitButton()
        button.assertIsEnabled()

        // A：标准 performClick
        runCatching { button.performClick() }
            .onFailure { t -> log("A 路 performClick 抛异常：${t.javaClass.simpleName}:${t.message}") }
        if (trigger != "gesture") {
            log("RETRY A 路未走出手势回调（trigger=$trigger dismissed=$dismissed）| ${geometry(button)}")

            // B：另一条注入路径
            runCatching { button.performTouchInput { click() } }
                .onFailure { t -> log("B 路 performTouchInput 抛异常：${t.javaClass.simpleName}:${t.message}") }
        }
        if (trigger != "gesture") {
            log("RETRY B 路仍未走出手势回调（trigger=$trigger dismissed=$dismissed）| ${geometry(button)}")

            // C：只作对照，不算通过
            probing = true
            val probe = runCatching { button.performSemanticsAction(SemanticsActions.OnClick) }
            probing = false
            log(
                "对照 C 路（直投 ClickAction，不计入通过）：调用成功=${probe.isSuccess} " +
                    "trigger=$trigger dismissed=$dismissed captured=${captured != null} | ${geometry(button)}"
            )
        }

        // 判据不变：必须是被真手势（A 或 B）点出来的回调；C 路只用来分辨成因。
        val result = requireNotNull(captured?.takeIf { trigger == "gesture" }) {
            "没走手势路径就回调了 onCreate，或根本没回调 | trigger=$trigger " +
                "captured=${captured != null} dismissed=$dismissed | ${geometry(button)}"
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
