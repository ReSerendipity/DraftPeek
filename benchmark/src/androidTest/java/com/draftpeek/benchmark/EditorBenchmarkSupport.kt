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
 * 而这些字符串会随系统语言漂移——本项目支持 zh/en/ja/ko 四语言，
 * 所以凡是必须按文案查找的地方，都改成遍历 [NEW_FILE_LABELS] 四语言候选。
 *
 * ## 运行前提（CI 与本地一致）
 * 1. 安装 production release 变体的应用（CI 由 `android.yml` 的 Macrobenchmark job
 *    `./gradlew :app:assembleRelease` + 单行 `adb install` 完成；`:app` 没有名为
 *    `benchmark` 的构建类型，旧文档里的 `:app:assembleBenchmark` 是不存在的任务）；
 * 2. 设备已解锁、屏幕常亮、已禁用动画（Macrobenchmark 会自行处理大部分）；
 * 3. 首启两道门由 CI 在跑用例前预置成已过状态（`benchmark/ci/preseed-onboarding-gate.sh`：
 *    DataStore `onboarding/completed` + SharedPreferences `agreement/accepted_v1`），
 *    因此用例走真实用户的 LAUNCHER 路径，不直启 Activity、也不点 UI 走引导；
 * 4. 预置过后 `MainActivity` 的**初始态是折叠的 speed-dial FAB**，菜单项不在无障碍树里，
 *    所以展开 FAB 属于用例前置的一部分（见 [ensureEditorOpen]），不是失败兜底。
 */
package com.draftpeek.benchmark

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

/** 「新建文件」在四语言下的文案，用于跨语言定位 FAB 菜单项。 */
internal val NEW_FILE_LABELS = listOf("新建文件", "New file", "新規ファイル", "새 파일")

/** 主 FAB 折叠 / 展开态的图标语义（core/ui BrandFAB.kt:183 硬编码英文）。 */
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

/** 等 speed-dial 展开确认的上限：200ms 旋转 + 每项 40ms 错峰 + 150ms 淡入，留一倍余量。 */
private const val SPEED_DIAL_EXPAND_TIMEOUT_MS = 2_000L

/**
 * 等待编辑器出现并返回它。
 *
 * @throws IllegalStateException 超时未找到时抛出，附可操作的排查指引
 */
internal fun UiDevice.requireEditor(): UiObject2 {
    wait(Until.hasObject(By.clazz(SORA_EDITOR_CLASS)), FIND_OBJECT_TIMEOUT_MS)
    return findObject(By.clazz(SORA_EDITOR_CLASS))
        ?: error(
            "在 ${FIND_OBJECT_TIMEOUT_MS}ms 内未找到 $SORA_EDITOR_CLASS。请确认：\n" +
                "1) 安装的是 production release 包（CI 由 android.yml 的 Macrobenchmark job 装）；\n" +
                "2) 设备已解锁且在桌面无锁屏遮挡；\n" +
                "3) 「新建文件」入口能点进编辑器（它是 FileBrowserScreen.kt:3056 起的 " +
                "speed-dial 菜单项，折叠态下按文案找不到）。"
        )
}

/**
 * 确保界面已进入编辑器：等应用窗口可交互 → 展开 speed-dial → 点「新建文件」。
 *
 * 之所以要兼容「启动后已在编辑器」的情况：Macrobenchmark 以 WARM 模式重启 Activity，
 * 系统可能恢复到上次离开时的界面。
 *
 * 前置：两道"首启门"由 CI 在跑用例前预置成已过状态 —— 引导页
 * （DataStore onboarding/completed，读点 SplashActivity.kt:141-143）与协议弹窗
 * （SharedPreferences agreement/accepted_v1，读点 MainActivity.kt:173-175）。
 * 见 benchmark/ci/preseed-onboarding-gate.sh；run 36104204506 已用设备侧回读证实两者生效
 * （`resumed` 变为 `.MainActivity`、`od -c` 17 字节、属主/SELinux 标签正确）。
 * 因此这里**不**直启 MainActivity、也**不**点 UI 走引导：setupBlock 的
 * pressHome + startActivityAndWait 走真实用户的 LAUNCHER 路径，由 Splash 自己导航。
 *
 * 历史（三条被实测处理掉的假设）：
 * - 「停在引导页」：预置后已不复现（run 36017565614 时是它，run 36104204506 已穿过去）。
 * - 「`am start --activity-clear-task` 直启」：run 36029759144 否掉（被接受但栈内无
 *   MainActivity record、焦点在桌面）。
 * - 「折叠 FAB 无需处理」：run 36104204506 反否 —— `fab=Add` 且菜单项不在树里，
 *   展开它是到达「新建文件」的必经前置，故本函数把展开写成正路而非兜底。
 */
internal fun UiDevice.ensureEditorOpen() {
    if (wait(Until.hasObject(By.clazz(SORA_EDITOR_CLASS)), 3_000L)) return

    // ① 窗口层：应用没有可交互窗口时，后面每一步都是空转，直接 fail-closed 并报焦点真值。
    if (!wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), APP_WINDOW_TIMEOUT_MS)) {
        error(diagnoseInaccessible("${APP_WINDOW_TIMEOUT_MS}ms 内应用无可交互窗口"))
    }

    // ② 控件层：折叠态的菜单项被 AnimatedVisibility 摘出无障碍树（BrandFAB.kt:97-98）。
    var newFile = findNewFileEntry()
    if (newFile == null && expandSpeedDial()) newFile = findNewFileEntry()
    if (newFile == null) error(diagnoseInaccessible("展开 speed-dial 后仍无「新建文件」入口"))

    newFile.click()
    waitForIdle()
    requireEditor()
}

/** 「新建文件」菜单项：label 落在文案上、mini 图标落在 content-desc 上。 */
private fun UiDevice.findNewFileEntry(): UiObject2? =
    NEW_FILE_LABELS.firstNotNullOfOrNull { label -> findByTextOrDesc(label) }

/**
 * 展开主 FAB，返回是否展开成功。
 *
 * 把手取主 FAB 自己的图标语义：`core/ui/.../BrandFAB.kt:183` 的
 * `contentDescription = if (expanded) "Close menu" else "Add"`。这两串硬编码英文，
 * 不随 zh / en / ja / ko 漂移，比按坐标或按 `FABSize` 折算屏幕位置稳。
 * 展开态用 "Close menu" 或菜单项入树来确认，不靠 sleep 猜动画时长。
 */
private fun UiDevice.expandSpeedDial(): Boolean {
    val collapsedHandle = findObject(By.desc(FAB_COLLAPSED_DESC))
    if (collapsedHandle == null) return speedDialExpanded()

    collapsedHandle.click()
    wait(Until.hasObject(By.desc(FAB_EXPANDED_DESC)), SPEED_DIAL_EXPAND_TIMEOUT_MS)
    waitForIdle()
    return speedDialExpanded()
}

/** speed-dial 是否处于展开态（图标翻成 Close menu，或菜单项已进无障碍树）。 */
private fun UiDevice.speedDialExpanded(): Boolean =
    findObject(By.desc(FAB_EXPANDED_DESC)) != null || findNewFileEntry() != null

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
private fun UiDevice.diagnoseInaccessible(reason: String): String = "前置未成立：$reason | resumed=" + resumedActivity() +
    " | focus=" + focusedWindow() +
    " | win=" + appWindowState() +
    " | fab=" + fabState() +
    " | crash=" + crashLog() +
    " | stack=" + activityStack() + "\n" +
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
