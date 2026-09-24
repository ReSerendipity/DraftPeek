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
 * 3. 无需人工预备引导页 —— 首屏被引导页挡住时由 [ensureEditorOpen] 显式直启
 *    MainActivity 绕开（见其中注释）。直启失败仍以明确错误失败并附 `am start`
 *    原文与当前焦点组件，**不会静默通过**。
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

/** 等展开确认的上限：200ms 旋转 + 每项 40ms 错峰 + 150ms 淡入，留足一倍余量。 */
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
 * 确保界面已进入编辑器：直启主界面 → 必要时展开 speed-dial FAB → 点「新建文件」。
 *
 * 之所以要兼容「启动后已在编辑器」的情况：Macrobenchmark 以 WARM 模式重启 Activity，
 * 系统可能恢复到上次离开时的界面。
 */
internal fun UiDevice.ensureEditorOpen() {
    if (wait(Until.hasObject(By.clazz(SORA_EDITOR_CLASS)), 3_000L)) return

    // 全新安装的首屏是引导页：路由只在 SplashActivity 上做拦截
    // （SplashActivity.kt:142-143 读 DataStore onboarding/completed 决定去
    // OnboardingActivity 还是 MainActivity，:87-89 执行跳转），MainActivity 自身不拦。
    // 显式直启 MainActivity 绕开引导，不点 UI、也不在设备外伪造 protobuf。
    val launchOutput = launchMainActivityDirectly()

    // 「新建文件」是 speed-dial 的菜单项，折叠态被 AnimatedVisibility 摘出无障碍树
    // （core/ui BrandFAB.kt:97-98），所以必须先展开主 FAB 才可能找到它。
    var newFile: UiObject2? = findNewFileEntry()
    if (newFile == null && expandSpeedDial()) newFile = findNewFileEntry()
    if (newFile == null) error(diagnoseInaccessible(launchOutput))

    newFile.click()
    waitForIdle()
    requireEditor()
}

/** 「新建文件」菜单项：展开后 label 落在文案上、mini 图标落在 content-desc 上。 */
private fun UiDevice.findNewFileEntry(): UiObject2? =
    NEW_FILE_LABELS.firstNotNullOfOrNull { label -> findByTextOrDesc(label) }

/**
 * 展开主 FAB，返回是否展开成功。
 *
 * 把手取主 FAB 自己的图标语义：`core/ui/.../BrandFAB.kt:183` 的
 * `contentDescription = if (expanded) "Close menu" else "Add"`。这两串是硬编码英文，
 * 不随 zh / en / ja / ko 漂移，因此比按坐标或按 `FABSize` 折算屏幕位置稳。
 * 展开态本身用 "Close menu" 或菜单项出现来确认，不靠 sleep 猜动画时长。
 */
private fun UiDevice.expandSpeedDial(): Boolean {
    val collapsedHandle = findObject(By.desc(FAB_COLLAPSED_DESC))
    if (collapsedHandle == null) {
        // 主 FAB 不在折叠态：可能已展开，也可能当前界面根本没有 speed-dial。
        return speedDialExpanded()
    }
    collapsedHandle.click()
    wait(Until.hasObject(By.desc(FAB_EXPANDED_DESC)), SPEED_DIAL_EXPAND_TIMEOUT_MS)
    waitForIdle()
    return speedDialExpanded()
}

/** speed-dial 是否处于展开态（图标变 Close menu，或菜单项已进无障碍树）。 */
private fun UiDevice.speedDialExpanded(): Boolean =
    findObject(By.desc(FAB_EXPANDED_DESC)) != null || findNewFileEntry() != null

/** 主 FAB 当前可观测状态，供失败诊断分辨"停在别的页面"与"展开失败"。 */
private fun UiDevice.fabState(): String = when {
    findObject(By.desc(FAB_EXPANDED_DESC)) != null -> FAB_EXPANDED_DESC
    findObject(By.desc(FAB_COLLAPSED_DESC)) != null -> FAB_COLLAPSED_DESC
    else -> "absent"
}

/**
 * 显式启动 `com.draftpeek/com.draftpeek.MainActivity`，返回 `am start` 的原始输出。
 *
 * 依赖 CI 镜像是 `google_apis`（userdebug，shell 持 `START_ANY_ACTIVITY`）才能启动
 * 非 exported 的 Activity；上一轮 run 35970870476 的实测原文是
 * `Starting: Intent { cmp=com.draftpeek/.MainActivity }`，无 Permission Denial，前提成立。
 */
private fun UiDevice.launchMainActivityDirectly(): String = try {
    executeShellCommand("am start -n $TARGET_PACKAGE/.MainActivity")
} catch (t: Throwable) {
    "executeShellCommand 异常: ${t.message}"
}

/**
 * 失败诊断。第一行就把全部证据排完 —— AGP 的文本报告只打印异常message 的前两行，
 * 证据放在第 3 行等于没有（上一轮的「窗口焦点」正是这样丢的）。
 */
private fun UiDevice.diagnoseInaccessible(launchOutput: String): String =
    "既未发现编辑器也未找到「新建文件」入口 | resumed=" + resumedActivity() +
        " | focus=" + focusedWindow() +
        " | fab=" + fabState() +
        " | launch=" + flat(launchOutput, 140) + "\n" +
        "判读: launch 含 Permission Denial ⇒ 镜像 shell 无权启动非 exported 组件；" +
        "fab=" + FAB_COLLAPSED_DESC + " ⇒ 已在带 speed-dial 的界面但展开失败；" +
        "fab=" + FAB_EXPANDED_DESC + " ⇒ 展开成功而菜单文案不匹配；" +
        "fab=absent ⇒ 当前界面没有 FAB（多半仍不在 MainActivity）。" +
        "resumed/focus 若显示为「无匹配行」，看括号里的字节数与 head 判断是截断还是没有该字段。"

/**
 * 取一条 shell 输出里的关键行；取不到时**必须**报告输出规模与开头若干字节，
 * 否则"探针空"与"环境真的没这行"分不清 —— 上一轮就栽在这里。
 */
private fun UiDevice.shellProbe(command: String, vararg tokens: String): String = try {
    val raw = executeShellCommand(command)
    val hits = raw.lines().map { it.trim() }.filter { line -> tokens.any { line.contains(it) } }
    if (hits.isNotEmpty()) flat(hits.joinToString(";"), 190)
    else "(无匹配行;${raw.length}B head=${flat(raw, 60)})"
} catch (t: Throwable) {
    "(异常 ${t.javaClass.simpleName}:${t.message})"
}

private fun UiDevice.resumedActivity(): String =
    shellProbe("dumpsys activity activities", "ResumedActivity", "topResumedActivity", "mResumed")

private fun UiDevice.focusedWindow(): String =
    shellProbe("dumpsys window", "mCurrentFocus", "mFocusedApp")

/** 压成一行并截断，保证诊断不会把关键证据挤到报告的第二行之后。 */
private fun flat(value: String, limit: Int): String =
    value.replace('\n', ' ').replace("\r", "").trim().take(limit)

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
