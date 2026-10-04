package com.draftpeek.feature.browser.ui

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.filters.SdkSuppress
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
 * 3. **红的时候 `boundsInRoot == boundsInWindow == [0,0,0,0]`，真实矩形只在 `touchBoundsInRoot` 里**。
 *    三次取证一致（run `36215197822` / `36237323311` / `36244917596`）：`posOnScreen=(397,1468)`；
 *    直投语义 `OnClick` 能让 `onCreate` 回调（`captured=true`、`dismissed=0`）。
 *    `445582e` 据此在 `typedFilename…` 里按 `touchBoundsInRoot` 中心补一次坐标注入：坐标算对了
 *    （`root(711.0, 1468.0)`，与 `posOnScreen` 一致），注入本身被拒 `AssertionError: Failed to inject touch input.`。
 *    ⚠ **本条当初把它判成"API 26 的手势几何问题、与生产码无关"，那个结论已被 2026-10-04 的实测否证**，
 *    见 [typedFilenameDrivesCreateCallbackWithKotlinEmptyTemplate] 的注释与 #DP-05。
 *
 * ## 进门先等节点出现，且重试必须可见
 * [awaitButton] 轮询到语义树里出现该节点为止（最多重试一次）；每次重试都打一行
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
     * 按 `touchBoundsInRoot` 的中心做一次坐标点击，返回是否完成注入。
     *
     * 为什么不用语义 `OnClick`：直投语义动作能触发回调（#106 取证里 `captured=true`），但那就
     * 不是"点了一下"，用它通过等于把这条 UI 回归测成假的。所以只在手势矩形塌零时，退回用
     * 真实存在的 `touchBoundsInRoot` 坐标注入 —— 仍然是手势。
     */
    private fun tapAtTouchBoundsCenter(node: androidx.compose.ui.test.SemanticsNodeInteraction): Boolean {
        val tb = runCatching { node.fetchSemanticsNode().touchBoundsInRoot }.getOrNull()
            ?: return false.also { log("坐标注入跳过：取不到 touchBoundsInRoot") }
        if (tb.width <= 0f || tb.height <= 0f) {
            log("坐标注入跳过：touchBoundsInRoot 也是零面积 ${describe(tb)}")
            return false
        }
        val cx = (tb.left + tb.right) / 2f
        val cy = (tb.top + tb.bottom) / 2f
        val result = runCatching {
            composeTestRule.onRoot().performTouchInput { click(Offset(cx, cy)) }
        }
        log("坐标注入点击 @ root($cx, $cy) 完成=${result.isSuccess} | ${geometry()}")
        result.onFailure { t -> log("坐标注入异常：${t.javaClass.simpleName}:${t.message}") }
        return result.isSuccess
    }

    /**
     * 取证行：屏幕尺寸 + 节点几何 + IME 状态，用来把三种"点了没反应"分开：
     * 零面积（未测量）、超出屏幕下沿（真被裁切）、`imeAcceptingText=true` 且几何正常（可能被 IME 遮）。
     * `dumpsys input_method` 是试探性的：测试进程没有 `android.permission.DUMP` 时把失败原文一并打出来，
     * 免得把"权限不够"读成"IME 没弹"。
     */
    private fun geometry(): String {
        val node = buttonNodeOrNull()
        val sn = node?.let { runCatching { it.fetchSemanticsNode() }.getOrNull() }
        val bounds = sn?.boundsInRoot
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
            " boundsInWindow=${describe(sn?.boundsInWindow)}" +
            " touchBoundsInRoot=${describe(sn?.touchBoundsInRoot)} posOnScreen=${sn?.positionOnScreen}" +
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

    /**
     * 输入文件名 → 点「创建并打开」→ 交回裸名 + 默认语言 + 空模板。
     *
     * ## `@SdkSuppress(minSdkVersion = 27)` 的原始理由（#106）**已被实测否证**
     * 原注释写的是"红都红在手势那一拍，不红在判据上 ⇒ 与生产码无关，是这一档上 Compose 1.8.0 的
     * 手势注入几何问题；API 27+ 仍走真手势路径"。2026-10-04 在 API 30 x86_64 / swiftshader 的
     * `DraftPeekApi30` AVD 上按用例分别单跑，实测是：
     * - **本条单跑 3/3 恒红**：红在末尾 `requireNotNull(captured)` 抛的"没回调 onCreate"（取证时栈在 `:248`）；
     * - **另一条 `createButtonIsDisabledUntilFilenameEntered` 冷启动后单跑 4/4 绿，再连续跑约 9 次后
     *   3 次里红 2 次** —— 它红在 `node.assertIsDisplayed()`（取证时栈在 `:185`），**那一步没有任何手势**。
     *   ⇒ "红只在手势那一拍" 不成立；"API 27+ 仍走真手势路径" 也不成立，因为 PR 门禁矩阵只有
     *   API 30（`.github/workflows/android.yml`），这条屏蔽保护不到任何在跑的门禁。
     *
     * ## 已用实测排除的解释（不要再照着它们找）
     * - 动画没落定：动画缩放为默认 1.0 时整类 2/2 红，把三个缩放全置 0 后整类连跑 3 次仍每次 2/2 红
     *   ⇒ 两种设置都红过，动画不是那个变量。
     * - 坐标算错：注入点 `root(711.0, 1468.0)` 正是 `touchBoundsInRoot` 的中心。
     * - **sheet 窗口没创建**：错。红的那次 `dumpsys window windows` 里同刻有两个
     *   `.../androidx.activity.ComponentActivity` 窗口（宿主 + `ModalBottomSheet` 自己的窗口）。
     *   （先前"窗口根本没创建"的结论来自跑起来后第 2.5 s 的**单次**快照，而那一刻连宿主窗口都还没
     *   出现（实测 ~0.67 s 才出现、sheet ~1.2 s）；单次快照定不了"从不"，只能定"还没"。）
     * - 跑类时的方法顺序：单跑本条 3/3 红，与顺序无关。
     * - 模拟器/instrumentation 坏了：对照 `FileBrowserSortTest` 同一台机 `OK (4 tests)`。
     *
     * ## 仍未定的是机制本身
     * 为什么 `boundsInRoot`/`boundsInWindow` 塌零而 `touchBoundsInRoot` 是真实矩形、且注入被拒。
     * 待验的候选：`CreateFileDialog` 是 `ModalBottomSheet`（`CreateFileDialog.kt:102`，Material3 把
     * 内容挂在独立窗口里），而本文件用 `createComposeRule()` + `onRoot()` 只绑到宿主窗口的 root，
     * 于是节点归属与手势目标都落在另一个 root 上。**这条还没被证明**，别当结论引用。
     * 另一条同样没量过的候选：失败时 `imeAcceptingText=true`（`performTextInput` 之后键盘已起），
     * 而注入点 `y=1468` 在 1080x2072 的屏上大概落在键盘覆盖区里（`touchBoundsInRoot` 不会因遮挡收缩）。
     * 判别很便宜：同一条用例里先收键盘再点，若转绿则遮挡是主因。本轮没跑这个实验。
     * 同理，"与生产码无关"现在也只是**未证明有关**，不是被排除的结论 —— 同一个 composable 在真机
     * 是宿主在 `MainActivity` 的窗口层级里显示的，测试宿主与生产宿主不同，这个差异没量过。
     *
     * 断言与判据一字未放宽；本条注释只改"为什么这么屏蔽"，不改屏蔽本身。
     */
    @Test
    @SdkSuppress(minSdkVersion = 27)
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

        // 前两次都是 performClick（第二次重新取节点），第三次才用坐标注入 —— 逐级加码，
        // 每次都留可见行；判据一字未放宽：仍然必须真的回调 onCreate 才算过。
        if (captured == null) {
            log("RETRY 首次 performClick 未触发 onCreate（dismissed=$dismissed）| ${geometry()}")
            composeTestRule.waitForIdle()
            button.performClick()
        }
        if (captured == null) {
            // 定向修复依据（#106，三次取证一致）：红的那一刻 `boundsInRoot == boundsInWindow ==
            // [0,0,0,0]`，而 `touchBoundsInRoot=[397,1402,1025,1534]`、`posOnScreen=(397,1468)`；
            // 直投语义 OnClick 能让回调发生 ⇒ 手势没打到真实矩形。故按真实矩形中心注入坐标。
            log("RETRY 二次 performClick 仍未触发，改用 touchBoundsInRoot 中心做坐标注入 | ${geometry()}")
            tapAtTouchBoundsCenter(button)
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
