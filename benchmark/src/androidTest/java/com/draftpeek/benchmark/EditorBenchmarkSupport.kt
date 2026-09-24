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
 * 确保界面已进入编辑器：以新任务直启主界面 → 点「新建文件」→ 等编辑器。
 *
 * 之所以要兼容「启动后已在编辑器」的情况：Macrobenchmark 以 WARM 模式重启 Activity，
 * 系统可能恢复到上次离开时的界面。
 *
 * 曾假设失败点是 speed-dial FAB 折叠，被 run 36017565614 的诊断否掉：`resumed` 与
 * `focus` 两次（t8/t9）都停在 `com.draftpeek/.onboarding.OnboardingActivity`，
 * 界面上压根没有那个 FAB。故展开 FAB 的代码已撤除，问题回到"MainActivity 上不来"。
 */
internal fun UiDevice.ensureEditorOpen() {
    if (wait(Until.hasObject(By.clazz(SORA_EDITOR_CLASS)), 3_000L)) return

    // 路由判定在 SplashActivity（:142-143 读 DataStore onboarding/completed，
    // :87-89 决定去 OnboardingActivity 还是 MainActivity），所以直启主界面绕开它。
    // 带 --activity-clear-task 是本次返工的关键差异：上一次实测 `am start` 被接受
    // （输出只有 `Starting: Intent { cmp=com.draftpeek/.MainActivity }`，无 Error 尾巴、
    // 也非 Permission Denial）但顶层仍是 OnboardingActivity —— 三个 activity 都没声明
    // launchMode（app/src/main/AndroidManifest.xml:56-77），故排除"被压在既有任务顶上"
    // 只能靠新任务 + 下一轮看 crash/栈证据定性。
    val launchOutput = launchMainActivityDirectly()

    val newFile = findNewFileEntry() ?: error(diagnoseInaccessible(launchOutput))

    newFile.click()
    waitForIdle()
    requireEditor()
}

/** 「新建文件」菜单项：展开后 label 落在文案上、mini 图标落在 content-desc 上。 */
private fun UiDevice.findNewFileEntry(): UiObject2? =
    NEW_FILE_LABELS.firstNotNullOfOrNull { label -> findByTextOrDesc(label) }

/** 主 FAB 可观测状态，仅供诊断分辨"当前页面到底有没有 speed-dial"。 */
private fun UiDevice.fabState(): String = when {
    findObject(By.desc(FAB_EXPANDED_DESC)) != null -> FAB_EXPANDED_DESC
    findObject(By.desc(FAB_COLLAPSED_DESC)) != null -> FAB_COLLAPSED_DESC
    else -> "absent"
}

/**
 * 以新任务显式启动 `com.draftpeek/com.draftpeek.MainActivity`，返回 `am start` 原始输出。
 *
 * CI 镜像是 `google_apis`（userdebug，shell 持 `START_ANY_ACTIVITY`）所以能启动
 * 非 exported 的组件 —— run 36017565614 的原文只有 `Starting: Intent {...}`，
 * 既无 Permission Denial 也无 Error 尾巴，前提已被实测确认。
 */
private fun UiDevice.launchMainActivityDirectly(): String {
    val output = try {
        executeShellCommand("am start --activity-clear-task -n $TARGET_PACKAGE/.MainActivity")
    } catch (t: Throwable) {
        "executeShellCommand 异常: ${t.message}"
    }
    waitForIdle()
    return output
}

/**
 * 失败诊断。第一行就把全部证据排完 —— AGP 的文本报告只打印异常message 的前两行，
 * 证据放在第 3 行等于没有（上一轮的「窗口焦点」正是这样丢的）。
 */
private fun UiDevice.diagnoseInaccessible(launchOutput: String): String =
    "既未发现编辑器也未找到「新建文件」入口 | resumed=" + resumedActivity() +
        " | focus=" + focusedWindow() +
        " | fab=" + fabState() +
        " | crash=" + crashLog() +
        " | stack=" + activityStack() +
        " | launch=" + flat(launchOutput, 140) + "\n" +
        "判读: crash 非空 ⇒ MainActivity 起来即崩（属生产缺陷，该修 app 而非测试）；" +
        "stack 里只有 OnboardingActivity ⇒ 直启没落地（看 launch 的 Error/Warning 尾巴）；" +
        "stack 有 MainActivity 而 resumed 是 Onboarding ⇒ MainActivity 被立即 finish；" +
        "launch 含 Permission Denial ⇒ 镜像 shell 无权启动非 exported 组件；" +
        "fab≠absent ⇒ 已在 MainActivity，此时才轮到 speed-dial/文案这一层。" +
        "任一探针显示「无匹配行」时看括号里的字节数与 head，判断是输出被截断还是真没有该字段。"

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
    "(异常 ${t.javaClass.simpleName}:${t.message})"
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
