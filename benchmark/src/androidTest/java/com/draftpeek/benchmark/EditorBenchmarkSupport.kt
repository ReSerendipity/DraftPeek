/**
 * 编辑器 Macrobenchmark 公共脚手架。
 *
 * 提供 [EditorInputBenchmark] 与 [EditorScrollBenchmark] 共用的定位与投喂能力。
 *
 * ## 为什么按「类名」而不是按「文案」定位编辑器
 * DraftPeek 的编辑器是 sora-editor 的 [io.github.rosemoe.sora.widget.CodeEditor]，
 * 它是一个**真实的 Android View**（经 `AndroidView` 宿主接入 Compose），
 * 因此 UiAutomator 可以直接按类名命中，不受系统语言、字号、主题影响。
 *
 * 反之，纯 Compose 节点只能借助无障碍语义树（文案 / contentDescription）定位，
 * 而这些字符串会随系统语言漂移（本项目支持 zh / en / ja / ko / zh-rTW）。
 * 因此本文件里唯一按文案定位的地方用的是**文件名** [SEED_FILE_NAME] —— 它由 CI 自己写入，
 * 不是本地化文案，天然不随语言漂移。
 *
 * ## 运行前提（CI 与本地一致）
 * 1. 安装 production release 变体的应用（CI 由 `android.yml` 的 Macrobenchmark job
 *    `./gradlew :app:assembleRelease` + 单行 `adb install` 完成；`:app` 没有名为
 *    `benchmark` 的构建类型，旧文档里的 `:app:assembleBenchmark` 是不存在的任务）；
 * 2. 设备已解锁、屏幕常亮、已禁用动画（Macrobenchmark 会自行处理大部分）；
 * 3. 首启两道门由 CI 在跑用例前预置成已过状态（`benchmark/ci/preseed-onboarding-gate.sh`：
 *    DataStore `onboarding/completed` + SharedPreferences `agreement/accepted_v1`），
 *    因此用例走真实用户的 LAUNCHER 路径，不直启 Activity、也不点 UI 走引导；
 * 4. 同一脚本还往 `files/user_files/` 播一个 [SEED_FILE_NAME]，用例点它的列表行进编辑器：
 *    性能腿不背「新建文件」对话框（其主按钮 `enabled = filename.isNotBlank()`）与无头档
 *    IME 注入的不确定性。新建链路的功能断言在
 *    `feature/browser/src/androidTest/.../CreateFileDialogFlowTest.kt`，两边不可互相冒充。
 */
package com.draftpeek.benchmark

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/** 被测应用包名，与 `app/build.gradle.kts` 的 applicationId 保持一致。 */
internal const val TARGET_PACKAGE = "com.draftpeek"

/** sora-editor 视图类全名。 */
internal const val SORA_EDITOR_CLASS = "io.github.rosemoe.sora.widget.CodeEditor"

/** 查找 UI 元素的超时时间（毫秒）。 */
internal const val FIND_OBJECT_TIMEOUT_MS = 15_000L

/** 单次测量中连续输入的字符数，模拟一次中等长度的连续键入。 */
internal const val INPUT_BURST = "fun benchmarkFrameTimingUnderTyping(): Int = 42\n"

/**
 * CI 预置进 `files/user_files/` 的种子文件名。
 *
 * 写入方与回读自证在 `benchmark/ci/preseed-onboarding-gate.sh` 步骤②d/②e；两侧必须同名，
 * 改名要同时改两处。生产依据：`AppFileManager.kt:23,33-37`（`filesDir/user_files`）、
 * 该目录即浏览器默认根（`FileBrowserScreen.kt:1560` 走 `internalFiles`）⇒ 只需 1 次点击。
 * `aaa` 前缀是为了在默认排序 NAME_ASC 下落在首屏（`FileSortOption.kt:44-46`）。
 */
internal const val SEED_FILE_NAME = "aaa_bench_seed.kt"

/** 取证行的双通道标签（println + Log.i，某一条不落进 CI 产物时还有另一条）。 */
private const val TAG = "DraftPeekBench"

/** 主 FAB 折叠 / 展开态的图标语义（core/ui BrandFAB.kt:183 硬编码英文），仅用于诊断定位停在哪个页面。 */
private const val FAB_COLLAPSED_DESC = "Add"
private const val FAB_EXPANDED_DESC = "Close menu"

/**
 * 等「被测应用自己拿出可交互窗口」的上限。
 *
 * run 36104204506 的教训：`topResumedActivity` 已经是 `MainActivity`，但窗口焦点整轮
 * 都停在 launcher 上（`Window{884a749…nexuslauncher}` 与预置前逐字相同）。那种状态下
 * 按文案找元素必然空、按坐标点也可能落空 —— 所以先等窗口入树再动手，等不到就硬失败。
 */
private const val APP_WINDOW_TIMEOUT_MS = 10_000L

/**
 * 等待编辑器出现并返回它。
 *
 * 成功时也打一行取证（`DRAFTPEEK_BENCH editor-ok`）。run 36120757720 暴露的盲区：取证只挂在
 * 失败分支上，于是"红点前移到 requireEditor"那一轮反而**没有**任何焦点/可见性读数可看。
 * 性能腿必须能自证"真的进了 CodeEditor"，不能只在摔倒时才有仪表。
 */
internal fun UiDevice.requireEditor(): UiObject2 {
    wait(Until.hasObject(By.clazz(SORA_EDITOR_CLASS)), FIND_OBJECT_TIMEOUT_MS)
    val editor = findObject(By.clazz(SORA_EDITOR_CLASS))
    if (editor != null) {
        val line = "editor-ok | seed=$SEED_FILE_NAME | visibleBounds=${editor.visibleBounds} | " + evidenceLine()
        println("DRAFTPEEK_BENCH $line")
        Log.i(TAG, line)
        return editor
    }
    error(
        "在 ${FIND_OBJECT_TIMEOUT_MS}ms 内未找到 $SORA_EDITOR_CLASS | " + evidenceLine() + "\n" +
            "点了列表项仍没有 CodeEditor 时按这行判读：seed 与 preseed 步骤②d 不一致 ⇒ 找错了行；" +
            "resumed 不是 .MainActivity ⇒ 点击把界面带去了别处；win 里 isVisible=false ⇒ 编辑器页" +
            "没上屏；win 里可见但 hasFocus=false ⇒ 点击没落到 app 窗口；crash 非空 ⇒ 编辑器初始化" +
            "崩了（wrapperError 分支会渲染 EditorInitErrorScreen，那属生产缺陷，该修 app 不是修测试）。"
    )
}

/**
 * 确保界面已进入编辑器：等应用窗口可交互 → 点 CI 预置的种子文件行 → 等 CodeEditor。
 *
 * 之所以兼容「启动后已在编辑器」：Macrobenchmark 以 WARM 模式重启 Activity，
 * 系统可能恢复到上次离开时的界面。
 *
 * ## 为什么点列表项，而不走「新建文件」
 * 新建链路是 FAB → CreateFileDialog 输文件名 → 「创建并打开」→ 才导航到编辑器
 * （FileBrowserScreen.kt:3060 只把 showCreateFileDialog 置 true；CreateFileDialog.kt:198
 * 的主按钮 enabled = filename.isNotBlank()）。run 36120757720 实测正卡在这：点了菜单项
 * 之后 15s 内永远等不到 CodeEditor，因为对话框还在等输入。让性能基线背这条 UI 链与无头档
 * IME 注入的不确定性只会造出假红，所以这里改为消费 CI 预置的文件；
 * **新建链路自身的功能覆盖在 feature/browser/src/androidTest/.../CreateFileDialogFlowTest.kt**，
 * 两者不可互相冒充。
 *
 * ## 前置（三项都由 benchmark/ci/preseed-onboarding-gate.sh 完成并回读自证）
 * 1. 引导页 DataStore `onboarding/completed`（读点 SplashActivity.kt:141-143）；
 * 2. 协议弹窗 SharedPreferences `agreement/accepted_v1`（读点 MainActivity.kt:173-175）；
 *    以上两项 run 36104204506 已用设备侧回读证实生效（resumed 变 .MainActivity、od -c 17 字节）；
 * 3. 种子文件 [SEED_FILE_NAME] 落在 `files/user_files/`，该目录即浏览器默认根，1 次点击可达。
 * 预置后应用走真实用户的 LAUNCHER 路径、由 Splash 自己导航；本文件不直启 Activity。
 *
 * ## 幂等
 * 编辑器只在显式保存动作时落盘（saveFile 的调用点只有保存按钮 / 另存 / 编码对话框：
 * EditorScreen.kt:361,879,1091 与 EditorViewModel.kt:1134），没有 onPause 自动保存。
 * 键入只改内存文档，WARM 重启后从盘重读 ⇒ 每轮迭代初始内容一致，无需逐轮复位。
 *
 * ## 历史（四条被实测处理掉的假设）
 * - 「停在引导页」：预置后不复现（run 36017565614 是它，36104204506 已穿过去）。
 * - 「`am start --activity-clear-task` 直启」：run 36029759144 否掉（被接受但栈内无
 *   MainActivity record、焦点在桌面）。
 * - 「展开 FAB 就到新建文件」：展开本身确实生效（run 36120757720 里入口被找到并点了），
 *   但点了之后还有对话框 —— 这条路对性能腿太长，按裁决放弃。
 * - 「改 headed 模拟器绕焦点」：1d36408 试过，按 2026-09-25 裁决复原（runner 上没有 X）。
 */
internal fun UiDevice.ensureEditorOpen() {
    if (wait(Until.hasObject(By.clazz(SORA_EDITOR_CLASS)), 3_000L)) return

    // ① 窗口层：应用没有可交互窗口时，后面每一步都是空转，直接 fail-closed 并报焦点真值。
    if (!wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), APP_WINDOW_TIMEOUT_MS)) {
        error(diagnoseInaccessible("${APP_WINDOW_TIMEOUT_MS}ms 内应用无可交互窗口"))
    }

    // ② 列表层：按种子文件名命中行（preseed 步骤②e 已断言该目录下 .kt 唯一，故无歧义）。
    val row = findSeededFileRow()
        ?: error(diagnoseInaccessible("列表里找不到预置文件 $SEED_FILE_NAME，查 preseed ②d/②e 输出"))

    row.click()
    waitForIdle()
    requireEditor()
}

/**
 * 种子文件的列表行。
 *
 * 行内文件名是可见文案且含扩展名（BrandFileCard.kt:168），整行是 Compose `Card`
 * （无障碍类名 `android.view.View`、可点击）。用 textContains 而不是精确匹配：同一行还有
 * 文件大小与相对时间（BrandFileCard.kt:189），后者随语言与时钟变化。
 */
private fun UiDevice.findSeededFileRow(): UiObject2? {
    val selector = By.pkg(TARGET_PACKAGE).textContains(SEED_FILE_NAME)
    wait(Until.hasObject(selector), FIND_OBJECT_TIMEOUT_MS)
    return findObject(selector)
}

/** 一行现场取证：焦点 / 窗口可见性 / FAB / 崩溃缓冲 / activity 栈。成功与失败两条路共用。 */
private fun UiDevice.evidenceLine(): String = "resumed=" + resumedActivity() +
    " | focus=" + focusedWindow() +
    " | win=" + appWindowState() +
    " | fab=" + fabState() +
    " | crash=" + crashLog() +
    " | stack=" + activityStack()

/** 探针取不到值时的标记前缀：让"设备侧命令失败"与"环境里真没这行"在报告里长得不一样。 */
private const val ShellFailurePrefix = "(异常"

/** 主 FAB 可观测状态，仅供诊断分辨"当前页面到底有没有 speed-dial"。 */
private fun UiDevice.fabState(): String = when {
    findObject(By.desc(FAB_EXPANDED_DESC)) != null -> FAB_EXPANDED_DESC
    findObject(By.desc(FAB_COLLAPSED_DESC)) != null -> FAB_COLLAPSED_DESC
    else -> "absent"
}

/**
 * 失败诊断。第一行就把全部证据排完 —— AGP 的文本报告只打印异常message 的前两行，
 * 证据放在第 3 行等于没有（上一轮的「窗口焦点」正是这样丢的）。
 *
 * `win=` 一列专门用来分开两种"找不到"：窗口压根没上屏（isVisible=false）与
 * 上屏了但焦点仍在 launcher（isVisible=true 而 hasFocus=false ⇒ 点了也不落）。
 */
private fun UiDevice.diagnoseInaccessible(reason: String): String = "前置未成立：$reason | " + evidenceLine() + "\n" +
    "判读（A' 两道门已由 run 36104204506 的设备侧回读证实已过）：resumed 仍是 " +
    ".onboarding.OnboardingActivity ⇒ 预置回退，查 preseed 的 od -c 输出；" +
    "win 里 isVisible=false ⇒ 应用窗口没上屏，属启动时序而非控件契约；" +
    "win 里 isVisible=true 而 hasFocus=false ⇒ 窗口可见但焦点在 launcher，点击会落空，" +
    "这也解释直启那次的「被接受却焦点在桌面」；fab=Add 且展开后仍无入口 ⇒ speed-dial 的" +
    "手势或 contentDescription 变了（BrandFAB.kt:183 / FileBrowserScreen.kt:3056）；" +
    "crash 非空 ⇒ MainActivity 起来即崩，属生产缺陷、停手修 app。" +
    "任一探针显示「无匹配行」时看括号里的字节数与 head，判断是输出被截断还是真没有该字段。"

/**
 * 应用窗口的可见性与焦点原值，用来把"根本没上屏"与"上屏了但没焦点"分开。
 *
 * 刻意不在设备侧用管道收窄（`dumpsys … | grep …`）：本仓里 `executeShellCommand` 从没走过
 * 带管道的命令，一条未验证的通道不该塞在**失败取证**的代码里 —— 它出问题时会把唯一的证据
 * 一起带走。改成取原文后在 JVM 侧切块过滤。
 */
private fun UiDevice.appWindowState(): String {
    val raw = shellText("dumpsys window windows")
    if (raw.startsWith(ShellFailurePrefix)) return raw

    val blocks = raw.split("Window #").filter { block -> block.contains(TARGET_PACKAGE) }
    if (blocks.isEmpty()) return "(无 $TARGET_PACKAGE 窗口块;${raw.length}B)"
    val tokens = listOf("Window{", "isVisible", "mVisible", "hasFocus", "mHasSurface", "mViewVisibility")
    val hits = blocks.flatMap { block -> block.lines() }
        .map { line -> line.trim() }
        .filter { line -> tokens.any { token -> line.contains(token, ignoreCase = true) } }
    return flat(hits.joinToString(";"), 240)
}

/** 崩溃缓冲：MainActivity 若在 release 构建里起不来，这是唯一的直接证据。 */
private fun UiDevice.crashLog(): String = shellProbe("logcat -d -b crash -t 200", "com.draftpeek", "FATAL")

/** 目标包当前在 activity 栈里的记录，用于判断 MainActivity 到底有没有被创建。 */
private fun UiDevice.activityStack(): String = shellProbe("dumpsys activity activities", "u0 $TARGET_PACKAGE/")

/**
 * 取一条 shell 输出里的关键行；取不到时**必须**报告输出规模与开头若干字节，
 * 否则"探针空"与"环境真的没这行"分不清 —— 上一轮就栽在这里。
 */
private fun UiDevice.shellProbe(command: String, vararg tokens: String): String = try {
    val raw = executeShellCommand(command)
    val hits = raw.lines().map { it.trim() }.filter { line -> tokens.any { line.contains(it) } }
    if (hits.isNotEmpty()) {
        flat(hits.joinToString(";"), 190)
    } else {
        "(无匹配行;${raw.length}B head=${flat(raw, 60)})"
    }
} catch (t: Throwable) {
    "$ShellFailurePrefix ${t.javaClass.simpleName}:${t.message})"
}

/**
 * 取 shell 输出的**原文**（供需要在 JVM 侧再加工的探针使用）。
 *
 * 异常与空白输出各自回一个可分辨的标记 —— 不返回 null、不静默空串，否则"探针坏了"
 * 会被读成"环境里真没这东西"。异常标记以 [ShellFailurePrefix] 开头，调用方据此分流。
 */
private fun UiDevice.shellText(command: String): String = try {
    val raw = executeShellCommand(command)
    if (raw.isBlank()) "(空白输出;${raw.length}B)" else raw
} catch (t: Throwable) {
    "$ShellFailurePrefix ${t.javaClass.simpleName}:${t.message})"
}

private fun UiDevice.resumedActivity(): String =
    shellProbe("dumpsys activity activities", "ResumedActivity", "topResumedActivity", "mResumed")

private fun UiDevice.focusedWindow(): String = shellProbe("dumpsys window", "mCurrentFocus", "mFocusedApp")

/** 压成一行并截断，保证诊断不会把关键证据挤到报告的第二行之后。 */
private fun flat(value: String, limit: Int): String = value.replace('\n', ' ').replace("\r", "").trim().take(limit)

/**
 * 按文案或内容描述查找元素。
 *
 * Compose 节点的可见名称可能落在 `text` 也可能落在 `content-description` 上
 * （取决于是否设置了 `contentDescription`），因此两者都要试。
 * 注意 [BySelector] 没有 `or()` 方法，只能分别查询。
 *
 * @param label 目标文案，等待超时为 2 秒
 * @return 命中的元素，未命中返回 null
 */
internal fun UiDevice.findByTextOrDesc(label: String): UiObject2? {
    wait(Until.hasObject(By.text(label)), 2_000L)
    return findObject(By.text(label)) ?: findObject(By.desc(label))
}

/**
 * 向当前获得焦点的视图发送整段文本。
 *
 * 走 `Instrumentation.sendStringSync` 而不是逐次 `pressKeyCode`：
 * 后者每次按键都是一次跨进程调用，在 40 字符量级上会引入数十毫秒的测试框架噪声，
 * 污染 FrameTimingMetric 的读数。
 */
internal fun sendText(text: String) {
    InstrumentationRegistry.getInstrumentation().sendStringSync(text)
}

/**
 * 生成用于滚动基准的样本文档。
 *
 * @param lineCount 行数，默认 120 行，足以让编辑器产生多屏可滚动内容
 * @return 组装好的 Markdown 文本
 */
internal fun seedDocument(lineCount: Int = 120): String = buildString {
    repeat(lineCount) { index ->
        append("## Section ${index + 1}\n")
        append("DraftPeek scroll benchmark line ${index + 1}: some prose long enough to wrap.\n")
    }
}
