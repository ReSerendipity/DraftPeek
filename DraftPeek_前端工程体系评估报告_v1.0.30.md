# DraftPeek 前端工程体系评估报告

> 评估对象：`C:/Users/Doro/DraftPeek`（Android 原生 UI 层：Jetpack Compose + Material 3 + 自研 DesignSystem）
> 评估版本：v1.0.30（10030）· 评估日期：2026-09-04
> 方法论：所有数字均来自本机实际执行（§4 验收命令 + 扩展核查），**无任何虚构指标**。

---

## ① 事实核对表

| 项 | 评估提示词声称 | 实测（证据来源） | 结论 |
|---|---|---|---|
| Compose BOM / Material 3 | 2025.04.01 / M3 | `libs.versions.toml`：`compose-bom=2025.04.01`、`material3=1.3.0` | ✅ 一致 |
| sora-editor / tree-sitter | 0.24.6 / 4.3.2 | `sora-editor=0.24.6`、`tree-sitter-binding=4.3.2` | ✅ 一致 |
| richeditor-compose / commonmark | 1.0.0-rc13 / 0.24.0 | 同上两行 | ✅ 一致 |
| Coil | 2.7.0 | `coil=2.7.0` | ✅ 一致 |
| Navigation | 2.8.4 | `navigation-compose=2.8.4` | ✅ 一致 |
| Hilt / DataStore / Room | 2.58 / 1.1.1 / 2.7.1 | `hilt=2.58`、`datastore=1.1.1`、`room=2.7.1` | ✅ 一致 |
| Glance | 1.1.1 | `glance-appwidget=1.1.1` | ✅ 一致 |
| Paparazzi | 2.0.0-alpha02 | `paparazzi=2.0.0-alpha02` | ✅ 一致 |
| 本地化 | zh/en/ja/ko 四语言 | 实际 **5 个变体**：values-zh、values-zh-rTW（繁中）、values-en、values-ja、values-ko | ⚠️ 比声称多一种 |
| baseline-prof.txt | "存在性待核实，可能缺失" | **存在**：`app/src/main/baseline-prof.txt`（7567 字节，2026-09-03） | ✅ 存在（提示词担忧不成立） |
| 编辑器/终端 View 互操作 | "两者都涉及复杂 View/Compose 混合" | editor 用 `AndroidView`（sora-editor）；**terminal 是纯 Compose**（`LazyColumn` 文本，无 AndroidView） | ❌ 前提不成立 |
| AGENTS.md 记录 Room 版本 | 2.6.1（项目指南文档） | 实际 `room=2.7.1` | ⚠️ 文档滞后，建议同步 AGENTS.md |

**核心结论**：提示词的事实锚点基本准确；两处需修正——① 终端并非 View/Compose 混合（纯 Compose）；② `baseline-prof.txt` 实际存在。

---

## ② 七维度评分（0–5，附证据）

### 2.1 Design System 落地度 — **3.5 / 5**
**加分项**
- Token 集中声明完善：`core/designsystem/theme/` 下 `Color.kt / Shape.kt / Spacing.kt / Elevation.kt / Type.kt / FontOptions.kt`，且含 `ColorBlindMode.kt / HighContrastScheme.kt / AccessibilityState.kt` —— 无障碍主题能力超出一般项目。
- `Brand*` 组件层真实存在（`core/ui/component/`）：`BrandButton / BrandChip / BrandDialog / BrandFAB / BrandFileCard / BrandIconButton / BrandOutlinedTextField / BrandPill / BrandSearchBar / BrandSettingRow / BrandSwitch / BrandToast / BrandTopBar` 共 13 个，覆盖主要交互控件。

**扣分项**
- **无 `BrandText` / `BrandCard` / `BrandScaffold` 包装** → 提示词命令把 `Text/Card` 计入违规是"虚高"的（这些本就无等价物）。
- **真实违规仅约 10 处**（用了对标有 Brand 等价物的 M3 组件）：`IconButton×7`、`Button×1`、`FloatingActionButton×1`、`TopAppBar×1`（命令原文给出的"49 文件/76 行"大部分是 Text/Card 这类无包装组件，以及 `androidTest` 测试文件）。
- **无任何自动化强制**：全仓无自定义 lint/detekt 规则引用 `Brand`，规则仅靠代码评审约定（铁律未按提示词所述"强制"）。
- **动态取色未启用**：`grep dynamicColor|DynamicTheme` 在 `core/designsystem/src` 命中 0 行 → Material You 未接入；且 `Theme.kt` 注释明示"bypass MaterialTheme.colorScheme 以精确匹配原型"，即品牌色自管、不随壁纸变化。

### 2.2 状态管理与架构 — **4 / 5**
- 单向流成立：`TerminalScreen` 用 `collectAsState` 消费多个 `StateFlow`；`TerminalViewModel` 用 `stateIn(...WhileSubscribed)` 暴露状态。
- `ViewModel` 跨层引用安全：`TerminalViewModel` 仅持 `@ApplicationContext`（已取 `.applicationContext`），未持 Android `View`；未见 `ViewModel` 直接持有 `Context` 做 UI 操作。
- 编辑器重状态合理：文档文本/光标由 sora-editor（View 体系）持有，Compose 侧仅持 `liveContent` 与状态标志；大文件经 `needsStreamingPreview` → `LazyMarkdownPreview` 分流。
- 小扣：终端用自有 `TerminalTheme`（非品牌 token）渲染颜色，存在轻微不一致。

### 2.3 性能与重组治理 — **3 / 5**
- ✅ `baseline-prof.txt` 存在（见事实表），启动/首屏有编译期优化。
- ✅ `LazyColumn` 普遍设 `key`（`FileBrowserScreen` 用 `itemsIndexed(key=...)`、`TerminalScreen` 用 `itemsIndexed(key={index})`），标识稳定。
- ⚠️ **无重组次数监控**：`RuntimePerformanceMonitor.kt`（343 行）仅提供 `avgFrameTime / jankFrames / jankPercentage`（Choreographer 帧时间），**不统计 recomposition count**。
- ⚠️ **Macrobenchmark 仅 `FileReadBenchmark` + `StartupBenchmark`**，缺编辑器输入延迟 / 滚动帧率（核心体验指标缺失）。
- ⚠️ Coil 仅默认配置：全仓仅 `MediaViewerScreen` 用 `AsyncImage`，无自定义 `ImageLoader`/磁盘缓存/`.size()` 约束 → Markdown 内图片未做尺寸约束（潜在内存风险）。
- ⚠️ 终端输出：每次 `outputBuffer` 变化 `remember(lines())` 整段重建逐行 `Text`，大输出重组偏多。

### 2.4 Compose 与 View 互操作 — **3.5 / 5**
- ✅ **仅 editor 一个互操作点**（sora-editor 经 `AndroidView`）；终端纯 Compose，无第二套实现。
- ✅ **editor 的 `update` 块是轻量的**：实测 `EditorScreen.kt:2500-2506 / 2576-2582` 的 `update` 仅读取 `wrapper.editor.firstVisibleLine > 5` 来置布尔量，**不做重活** → 反驳"互操作重型 update"反模式。
- ⚠️ **同一 sora View 被 3 个 `AndroidView` 槽位复用**（`EditorScreen.kt:2473/2549/2647`，factory 内 `removeView` 再挂载）。split/preview 切换时同一 `View` 在多个宿主间争用，状态同步（文本/选区/主题）依赖 `wrapper` 而非 Compose 状态，存在脆弱性与回归风险。
- ⚠️ 主题同步待验证：暗色主题下 sora 编辑器外观是否随 `LocalDarkTheme` 切换，未见显式桥接。

### 2.5 可访问性与本地化 — **4 / 5**
- ✅✅ **本地化键集合完全对齐**：`zh=72 / en=72 / ja=72 / ko=72` 键，**0 缺失 / 0 多余**；另有 `values-zh-rTW`（繁中）额外覆盖。即便 `MissingTranslation` 被 disable，当前**无漂移**（反驳反模式 #7 的担忧）。
- ✅ `HardcodedText` **未被 disable**（lint.xml 仅 `MissingTranslation`+`ExtraTranslation` 设为 `ignore`），硬编码字符串仍会被 lint 捕获。
- ✅ 含 `AccessibilityTest.kt`（androidTest）、`AccessibilitySemantics.kt`、`AccessibilityState.kt`、designsystem 含色盲/高对比主题。
- ⚠️ **全仓 `@Preview` 仅 1 处**（位于 `core/ui/.../DraftPeekLocals.kt`，非功能预览）→ 功能性 Preview 实际 **0 个**，暗色预览覆盖率 **0%**；`contentDescription` 与 48dp 触摸目标覆盖未经量化；终端输入区 `BasicTextField` 无 `contentDescription`。
- ✅ 等宽字体/行高/连字可配置：终端用 `FontFamily.Monospace + 12.5sp + lineHeight 1.7`，且有 `FontOptions.kt`。

### 2.6 模块化与构建体验 — **4 / 5**
- ✅ 13 个 Gradle 模块边界清晰；`grep projects.feature` 在 `feature/` 内**命中 0** → feature 间无互相依赖。
- ✅ `build-logic` 约定插件生效：`core/designsystem/build.gradle.kts` 仅 **39 行**、`core/ui` 64 行、各 feature 74–120 行、`app` 427 行 → 配置显著去重。
- ⚠️ **构建性能未达标（实测）**：`assembleDevDebug --offline -q` 在 **240s 内未完成**（被 SIGTERM 终止）→ 增量构建仍偏慢，需用 `--profile` 量化瓶颈（R8/Compose 编译/StringEnc IR）。
- ⚠️ 配置缓存兼容性待验证：自定义任务（`bumpVersion`/`generateChangelog`/`StringEncIrGenerationExtension`）未见 `@UntrackedTask`/`@CacheableTask` 注解，AGENTS.md 提及但未运行验证。

### 2.7 测试与回归防线 — **1.5 / 5**
- ❌ **`core/ui` 仅 1 个测试文件**（`BrandComponentScreenshotTest.kt`），且**全仓 0 张 Paparazzi 快照基线 PNG** → 该截图测试当前**无法守护回归**（无 golden 可比，首次运行即失败/需 record）。
- ❌ Paparazzi `2.0.0-alpha02` 进关键链路（UI 基座唯一测试），alpha 稳定性无兜底、CI 未强制。
- ❌ 全仓仅 **78 unit + 9 androidTest**；UI 行为测试稀少；editor 无 `RoundTripFidelityTest`（编辑→预览→导出文本保真）。
- ✅ 正向：存在 `AccessibilityTest`、`EditorScreenComposeTest`、`FileBrowserSortTest` 等少量 androidTest 行为测试。

---

## ③ 缺陷清单（P0 / P1 / P2）

### P0（高，阻断性）
1. **UI 基座无有效回归网**：`core/ui/src/test/.../BrandComponentScreenshotTest.kt` 唯一测试为 alpha Paparazzi 且无基线。
   - 命令：`新增 core/ui androidTest 行为测试` → 见 Q1。
2. **编辑器多 AndroidView 复用同一 sora View，状态同步脆弱**（切换 split/preview 可能双挂载/丢状态）。
   - 文件：`feature/editor/.../EditorScreen.kt:2473,2549,2647`
   - 命令：抽 `SoraEditorBridge`（封装 removeView 复用 / update 轻量化 / 主题同步 / IME 焦点）+ 加切换回归测试。

### P1（中，需近期处理）
3. 无重组次数监控；Macrobenchmark 缺编辑器输入/滚动指标。
   - 文件：`benchmark/`；命令：加 `RecompositionCounter` + `EditorInputBenchmark`/`EditorScrollBenchmark`。
4. 动态取色未启用。
   - 文件：`core/designsystem/theme/Theme.kt`；命令：接入 `dynamicColor`。
5. 真实 `Brand*` 违规约 10 处（IconButton×7、Button×1、FAB×1、TopAppBar×1）+ 无自动强制。
   - 文件：`feature/*`、`app`；命令：替换为 `Brand*` 等价物 + 加 detekt/lint 自定义规则。
6. Coil 默认配置，Markdown 图片无尺寸约束。
   - 文件：`MediaViewerScreen.kt` / 全局 `ImageLoader`；命令：配置 `ImageLoader` + `.size()`。
7. 配置缓存下自定义 Gradle 任务兼容性未验证（bumpVersion/generateChangelog/StringEnc IR）。
   - 文件：`build-logic`、`scripts/`；命令：加 `@UntrackedTask` / 实测 `--configuration-cache`。
8. 增量构建 >240s 未达标。
   - 命令：`./gradlew :app:assembleDevDebug --profile` 定位瓶颈。

### P2（低，可排期）
9. 全仓 0 个功能性 `@Preview`（含暗色）；设计迭代与 a11y 审查缺实时预览。命令：补 `@Preview` + `uiMode` 暗色。
10. `contentDescription`/48dp 触摸目标未量化；终端 `BasicTextField` 无 `contentDescription`。命令：a11y 抽查 + 修复。
11. 终端用自有 `TerminalTheme` 绕过品牌 token（轻微不一致）。文件：`TerminalScreen.kt:103-111`。

---

## ④ 改进路线图

| 阶段 | 目标 | 关键动作 | 验收 |
|---|---|---|---|
| **P0（1–2 周）** | 止血：UI 基座回归网 + 编辑器互操作稳定 | ① `core/ui` 加 Compose UI Test（行为）② `SoraEditorBridge` 抽象 + 切换回归测试 ③ Paparazzi 补一次基线（record） | `core/ui` 行为测试 ≥5；editor 切换测试通过；Paparazzi 有基线 |
| **P1（3–6 周）** | 性能可观测 + 一致性 | ④ `RecompositionCounter` + 编辑器 Macrobenchmark ⑤ 接入 `dynamicColor` ⑥ `IconButton` 等违规替换 + detekt 规则 ⑦ Coil `ImageLoader` 配置 ⑧ `--configuration-cache` 验证 ⑨ `--profile` 优化构建 | 重组指标可看板；0 Brand 违规；构建 <120s；CI 阻断翻译键漂移 |
| **P2（持续）** | 体验打磨 | ⑩ 补 `@Preview`+暗色 ⑪ a11y 量化覆盖 ⑫ 终端接入品牌 token 或明确豁免 | Preview 暗色覆盖 >80%；a11y 抽查无阻断项 |

---

## ⑤ 三个必答题

### Q1：`core/ui` 仅 1 个测试文件，优先补 Paparazzi 截图 还是 Compose UI Test 行为？理由与首个落地模块？
**结论：优先补 Compose UI Test（行为/语义，androidTest），而非 Paparazzi 截图。**

理由：
- Paparazzi 当前是 **alpha 2.0.0-alpha02** 且 **0 张基线**，截图测试本身不可靠、无 golden 可比；行为测试用稳定 API（`createComposeRule` + `assertText`/`assertHasClickAction`），不依赖 alpha 渲染与像素基线，**可 CI 化、跨设备稳定**。
- UI 基座（`Brand*` 组件）最危险的是**行为契约**（点击回调、enabled 状态、语义 role、`contentDescription`、字体缩放下的 min-touch-target），而非逐像素外观；行为测试捕获回归更直接。
- 截图基线维护成本高，对主题/字体/屏密度敏感，alpha 上更脆弱。

**首个落地模块**：`core/ui` 的 `BrandButton` / `BrandSwitch` / `BrandOutlinedTextField`（交互控件、契约最明确）。先写 androidTest 验证 enabled/disabled、onClick 触发、role/contentDescription、字体放大下的 48dp 触摸目标；再让现有 Paparazzi 测试 `record` 一次生成基线，作为 CI **可选**关卡（非阻断）。进阶：editor 的编辑→预览→导出"往返保真"应做成 `feature/editor` 的行为测试（核心体验，当前缺失）。

### Q2：编辑器与终端两个互操作场景，是否需要抽象统一的 View-Compose 桥接层？
**结论：不需要为"统一桥接"抽象通用层。**

理由：
- **实测 terminal 是纯 Compose**（`LazyColumn` 文本输出，**无 AndroidView**），所以"两套互操作实现"的前提不成立——全仓**只有 editor 一个**互操作点。
- sora-editor 是特化的代码编辑器 View；终端输出是文本流，两者生命周期/状态模型完全不同，强行统一桥接只会引入不必要的抽象与耦合。
- 真正该做的是：**把 editor 内部"多 `AndroidView` 槽位复用同一 sora View"的重复逻辑局部封装**成一个小型 `SoraEditorBridge`（处理 factory 的 removeView/复用、`update` 轻量化、主题同步、IME 焦点），让 `EditorScreen` 三处收敛。**仅限 editor 模块内**，不跨到 terminal。

### Q3：在 disable 了翻译 Lint 的前提下，如何保证四语言不缺失？（可执行脚本化校验）
**实测当前四语言键集合已完全对齐（72/72/72/72，0 缺失）**，但机制上 `MissingTranslation` 被 disable，未来可能漂移。给出可立即落地的脚本化校验：

- **方案 A（键集合比对，必须）**：脚本提取 `values-zh/strings.xml` 的 `name`，与各 `values-{lang}/strings.xml` 比对，缺失/多余即非零退出。下面给出可直接放入 `scripts/check_strings_keys.py` 的实现。
- **方案 B（疑似漏翻检测）**：对每个非 zh 键，若其 `<string>` 文本与 zh 完全相同或为空，标记为疑似漏翻。
- **方案 C（接入 CI/precheck）**：在 `precheck.ps1` 增加一步调用该脚本；或加一个 Gradle `verifyTranslations` 任务（用 `XmlSlurper`）挂到 `check`；或 GitHub Actions / pre-commit 跑。

```python
#!/usr/bin/env python3
# scripts/check_strings_keys.py  —— 跨语言 strings.xml 键集合一致性校验
import sys, re, pathlib

RES = pathlib.Path("app/src/main/res")
BASE = "values-zh"
LANGS = ["values-en", "values-ja", "values-ko", "values-zh-rTW"]

def keys(locale: str) -> set[str]:
    f = RES / locale / "strings.xml"
    if not f.exists():
        return set()
    txt = f.read_text(encoding="utf-8")
    return set(re.findall(r'name="([^"]+)"', txt))

base = keys(BASE)
missing_total = 0
for lang in LANGS:
    lk = keys(lang)
    missing = base - lk
    extra = lk - base
    if missing or extra:
        missing_total += len(missing)
        print(f"[{lang}] 缺失 {len(missing)}: {sorted(missing)}")
        print(f"[{lang}] 多余 {len(extra)}: {sorted(extra)}")
if missing_total:
    print(f"FAIL: {missing_total} 个键缺失"); sys.exit(1)
print("OK: 四语言键集合一致")
```

**建议**：`HardcodedText` 仍保持启用；并加一个对 Compose `Text("字面量")` 的静态 grep 软校验（非阻断）。这样即便翻译 Lint 被 disable，也有脚本化兜底，且已实测当前无漂移。

---

## ⑥ 硬约束声明
- 本报告所有数字（版本、违规计数、键集合、测试数、模块数、构建结果、Preview 数）均来自对 `C:/Users/Doro/DraftPeek` 的**实际命令执行**与**文件读取**，未虚构任何 UI 测试覆盖或性能指标。
- 唯一未给出精确数值的项：`assembleDevDebug` 增量耗时（240s 内未完成，已如实标注，未估算）；重组次数（确认无监控，非"为 0"）。
- 对提示词前提的两处修正已写入事实核对表：① 终端为纯 Compose（非 View/Compose 混合）；② `baseline-prof.txt` 实际存在。

---

## ⑦ 整改实施记录（2026-09-04 / 09-05，证据绑定）

| 缺陷 | 状态 | 落地物（均已编译/测试验证） |
|---|---|---|
| P0-1 UI 基座回归网 | ✅ | `core/ui/src/androidTest/.../BrandComponentsBehaviorTest.kt`（13 用例）。本批修复其**从未编译过**的存量缺陷：DpRect width/height 扩展属性缺 import、`core:ui` 漏配 AndroidJUnitRunner、androidTest 缺 runner 依赖（GOTCHAS #32） |
| P0-2 编辑器互操作脆弱 | ✅ | `SoraEditorBridge.kt` + 单测（三处 AndroidView 槽位收敛） |
| P0-3 Paparazzi 基线 | ✅ | 2 张基线 PNG 入库；并确认 `:core:ui:testDebugUnitTest` 实测执行（此前因 useJUnitPlatform 风险差点被跳过，GOTCHAS #34） |
| P1-3 重组监控 | ✅ | `core/ui/.../performance/RecompositionCounter.kt`（非 State 计数器避免自激重组；双开关默认关、零开销）+ 18 个 JVM 单测 + 7 个 androidTest 运行时行为用例 |
| P1-3 编辑器 Macrobenchmark | ✅ | `benchmark/.../EditorInputBenchmark.kt`（键入帧耗时）+ `EditorScrollBenchmark.kt`（fling 帧耗时）。**前置修复**：benchmark 模块缺 coreLibraryDesugaring 致全部基准自诞生起未编译过；`measureRepeated` 缺顶层函数 import（GOTCHAS #33，已连带修好 FileReadBenchmark 与 CI 幻影任务名） |
| P1-4 dynamicColor | ✅ | `Theme.kt` 默认 `dynamicColor = true`（Android 12+ 系统取色，低版本回落品牌色板） |
| P1-5 Brand* 违规 | ✅ | 实测违规 0 处（grep 复核 IconButton/FAB/TopAppBar 直用仅剩模块内私有包装） |
| P1-6 Coil 配置 | ✅ | DraftPeekApp 全局 ImageLoaderFactory（内存 20% / 磁盘 64MB） |
| P1-8 增量构建 <120s | ✅（实跑验证） | 目标 <120s，**实测 42–48s**（详见下方「P1-8 实测方法与数据」）：新增/删除/修改 app 源码文件后 `:app:assembleDevDebug` 分别 48s / 42s / 45s。方法论要点：Gradle 按**内容哈希**而非 mtime 判定输入，**`touch` 不会触发重编译**（首轮误测因此得到假的 13s），必须用真实内容变更 |
| P1-7 配置缓存兼容 | ✅（实跑验证） | 全仓扫描确认无 `afterEvaluate`/`buildFinished`/`addListener` 等执行期 project 捕获；唯一两处违反在根 `build.gradle.kts` 的 `bumpVersion`/`generateChangelog`（`doLast` 内用 `project.findProperty`/`rootProject.file`/`rootDir`），已改为配置期捕获、行为不变（提交 `b788a72`）。**实测**：`./gradlew :feature:browser:compileDebugKotlin --configuration-cache` → `BUILD SUCCESSFUL in 25s` + 日志出现 `Configuration cache entry stored` |
| P2-9 功能性 @Preview | ✅ | `core/ui/.../BrandComponentPreviews.kt`：6 组件 × 亮暗双主题（PreviewParameter 驱动），新增 ui-tooling-preview 依赖（GOTCHAS #35） |
| P2-10 a11y 量化 | ✅ | 新增 `scripts/check_a11y.py`：图标/图片 contentDescription 覆盖率 **136/136 = 100%**，已接入 `precheck.ps1`（--strict 阻断）；修复 `TerminalScreen` BasicTextField 无 a11y 名称（semantics.contentDescription） |
| P2-11 终端主题 token | ✅（豁免） | `TerminalModels.kt` 增加书面豁免声明：16 色 ANSI 调色板与 Material 语义色正交，接入会破坏命令输出着色；品牌一致性由终端容器（Brand* 组件）承担 |

未做（如实记录）：Macrobenchmark 阈值断言（需真机基线数据积累后才能定，首轮先出数）。

#### P1-8 实测方法与数据（2026-09-05，:app:assembleDevDebug）

| 轮次 | 变更内容 | 耗时 | 任务情况 |
|---|---|---|---|
| 基线 | 本轮首次（配置缓存 cold，需 `stored`） | **125s** | 272 actionable，62 executed |
| A | 新增 1 个 app 源文件 | **48s** | 14 executed / 258 up-to-date |
| B | 删除该文件 | **42s** | 12 executed / 2 from cache |
| C | 修改已有文件 `MainActivity.kt` | **45s** | 18 executed / 254 up-to-date |
| D | 还原该改动 | **13s** | 11 executed / 7 from cache |

- **测量条件**：`--offline --no-daemon`；配置缓存 **开**（`org.gradle.configuration-cache=true`，即项目默认）、构建缓存 **开**（`org.gradle.caching=true`）。
- **与旧结论的差异说明**：本报告早先记录「`assembleDevDebug` 增量 240s 内未完成」，当时使用的是沙箱惯例参数 `--no-configuration-cache --no-build-cache`，两大缓存机制均被关闭，与开发者日常构建路径不同，故数值不可比。本次按项目默认配置实测。
- **方法陷阱（值得记录）**：首轮用 `touch` 改文件测出 13s，是**假数据**——Gradle 以内容哈希判定输入变更，`touch` 只改 mtime 不会使 `compileDevDebugKotlin` 失效，实测退化为一次空跑。已改为真实内容变更后重测。
- **遗留观察（未修）**：`configureCMakeDebug`/`buildCMakeDebug`（4 个 ABI）在部分轮次中每次都执行、无法进入 UP-TO-DATE，是 13s 空跑轮次里的主要开销；伴随 `CXX5304` 警告（本机 SDK XML 版本 4 > AGP 可识别的 3）。建议后续单独立项。

> **2026-09-05 状态纠偏（铁律 #1 事实同步）**：上一版本本行曾将「P1-7 配置缓存兼容专项验证」列为未做，系报告定稿早于验证完成所致（报告 11:24 定稿，P1-7 实跑验证在其后）。P1-7 现已实测通过并补入上表，此处移除该项，避免与证据冲突。
