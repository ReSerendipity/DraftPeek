# 真机走查记录 — 2026-11-07

设备：RMX5010 / Android 16 / SDK 36 / 1264×2780（真机，非 AVD；本机无 AVD）
安装包：`com.draftpeek` 1.0.32（正式包，`debuggable=false`）+ `com.draftpeek.dev` 1.0.33-dev（本机构建，`debuggable=true`，`run-as` 可用）
走查方式：`adb shell input` 点击/长按 + `uiautomator dump` 取控件 bounds + `exec-out screencap` 逐屏取证
取证目录：`docs/qa/2026-11-07-walkthrough/`（本文引用的 PNG 均在该目录）

## 一、覆盖情况

| 屏 | 内容 | 结果 | 截图 |
|---|---|---|---|
| 文件页（文件夹内） | 搜索框、筛选胶囊（全部/最近/收藏/书签/代码/文档…）、`文件 28`、文件行（类型徽标 + 名称 + EXT 标记 + 大小 + 保存按钮） | 正常 | `05-files-list.png` |
| 文件页（根，空态） | 授权引导卡「暂无文件 / 授权访问手机上的文件夹…」、`选择文件夹`、`从 GitHub 导入`、`新建文件` | 正常 | —（现场观察，未单独留档） |
| 富文本模式 | 同一份 `ab_h1_matrix.md`：5 个 ATX 标题按 24sp/20sp/18sp 加粗渲染，正文两段正常 | 正常 | `05-files-list.png` 之前的编辑器首屏（见 `01` 现场）、`18-editor-overflow-menu.png` 背景 |
| 编辑模式 | 源码行 + 行号、语法着色 | 正常 | `16-split-preview-headings-invisible.png` 左栏 |
| 预览模式 | **标题行全部不可见**（见缺陷 1） | **缺陷** | `15-preview-headings-invisible.png` |
| 分屏模式 | 同上：左栏源码有 5 个 `#` 标题，右栏预览只剩两段正文 | **缺陷** | `16-split-preview-headings-invisible.png` |
| 编辑器 ⋮ 溢出菜单 | 三组齐全：编辑（撤销/重做/搜索「打开 查找/替换/跳转到行 面板」）、工具（终端「打开内置终端」/保存为代码片段/反向链接/插入代码片段）、文件（选择编码重新打开） | 正常 | `18-editor-overflow-menu.png` |
| 编辑器格式工具栏 | 粗体/斜体/删除线/代码/无序列表正常，**末项「有序列表」被裁切并逐字竖排**（见缺陷 2） | **缺陷** | `18-editor-overflow-menu.png` 背景上半部 |
| 我的 | 头像、未登录提示、`登录/注册`、版本胶囊（1.0.32）、设置四项（编辑器设置/实验室/无障碍/关于与安全）、统计区间胶囊（汇总/本年/本月/本周/今天/昨天） | 正常 | —（现场观察） |
| 关于与安全 | 安全状态（签名校验 ✅ 已验证官方、DEX 完整性 ✅ 通过、运行时环境 安全）、重新校验、导出安全事件报告、清空安全事件日志；关于（验证应用、版本 1.0.32、开源许可） | 正常（但状态语义存疑，见缺陷 7） | —（现场观察） |
| 首启协议弹层（屏 05） | onboarding 4 页 → 「开始使用」→ 底部协议弹层（未勾选禁用 / 勾选启用 / 展开完整条款） | 正常（可复现） | §六.1 |
| 文件页 ⋮ 菜单（屏 06–08） | **本构建上不存在** ⋮ 入口；顶栏只有 手动排序 / 历史记录 / 代码片段，`+` FAB 才承载新建/导入 | **与旧记载冲突** | §六.2 |
| 编辑器（屏 15–18） | 顶栏 4 个模式页签 + 保存 / 快捷设置 / 更多操作；格式栏 粗体/斜体/删除线… | 正常（细节见 §六.3） | §六.3 |

## 二、缺陷（本次走查新发现，均未修复，仅取证）

### 缺陷 1：预览 / 分屏的预览侧不显示任何 Markdown 标题（ATX heading）

- 复现文件：`DraftPeekAudit/ab_h1_matrix.md`（137 B），内容依次为
  `# H1 第一块 ZZZ1`、`段落甲 AAA1。`、`## H2 中部 ZZZ2`、`### H3 中部 ZZZ3`、`# H1 中部 ZZZ4`、`# ASCII ONLY H1 ZZZ5`、`段落乙 BBB1。`
- 预览模式：屏上只剩「段落甲 AAA1。」「段落乙 BBB1。」两段，5 个标题行全部缺失。
- 分屏模式：左侧源码 11 行可见 5 个 `#` 标题（第 1/5/7/9/11 行），右侧预览与预览模式完全一致，只剩两段正文。
- 富文本模式：同一文件的 5 个标题全部正常（24sp / 20sp / 18sp 加粗）。
- 判读：这是**渲染路径差异**，不是解析差异——同一份文本在富文本路径能出标题。预览侧首行之上仍留有空白（占位存在），倾向「块被排版但文字没画出来」而不是「块被整块丢弃」。
- **根因已定位（2026-11-07 当场修）**：预览走 `MarkdownWebViewPreview` → `assets/markdown/markdown-preview.html`，该页自带 `marked v12.0.0`（`marked.min.js` 头部即 `marked v12.0.0`）。而页面里的自定义 renderer 是按 **v13+ 的 token 入参**写的：
  - `renderer.heading = function(obj){ var text = obj.text || ''; var level = obj.depth; … }`
  - v12 实际传的是**字符串**：`heading(text, level, raw)`、`listitem(text, task, checked)`。于是 `obj.text` → `undefined`、`obj.depth` → `undefined`，产出 `<hundefined></hundefined>`——既没有标题样式也没有文字，屏上就是「标题行整片消失」。列表项同理被渲染成空 `<li>`。
  - 设备侧取证：从已装包 `pm path com.draftpeek` 拉出 `base.apk`，其中 `assets/markdown/markdown-preview.html` 确为上述写法（`var text = obj.text` / `var level = obj.depth` 均命中）。
  - 修复后对照（用仓库内置的同一份 `marked.min.js` 在 node 里跑）：旧写法 → 5 个 `<hundefined>` 空标题 + 4 个空列表项；新写法 → 5 个标题全部带文字（`<h1>H1 第一块 ZZZ1</h1>` …`<h1>ASCII ONLY H1 ZZZ5</h1>`）+ 2 个列表项 + 2 个任务复选框且勾选态保留。脚本与输出留在 agent 工作区 `scan/dp_marked_v12_check.js`。
- 影响：Markdown 主要用途（看结构）在预览下失效；分屏（写作时最常用）同样失效。

### 缺陷 2：格式工具栏末项「有序列表」被裁切 + 逐字竖排

- 六项依次为 粗体 / 斜体 / 删除线 / 代码 / 无序列表 / 有序列表，第 6 项在 1264 宽屏上右溢出，且「有/序/列/表」四字各占一行（宽度被压到单字宽）。
- 判读：工具栏缺横向滚动（或行内未换行容器），末项被父容器按剩余宽度压缩。

### 观察项 3：文件页与文件行都没有 ⋮ 入口（与既有文档记载不一致）

- 文件页顶栏只有三个图标：☰（点击后变红，疑为排序/多选态，屏面无可见变化）、时钟（历史）、`{}`（代码视图）。
- 文件行只有右侧「保存」按钮；短按行不进入详情，长按进入编辑器。
- 既有文档记载「文件页 ⋮ 菜单（重命名/收藏/删除）」在本构建上找不到入口 → 需业主确认是 IA 改版还是入口回归。

## 三、修复与验证（缺陷 1）

- 改动：`feature/editor/src/main/assets/markdown/markdown-preview.html` 的 `renderer.heading` / `renderer.listitem` 改为两种入参形状都兼容（v12 字符串 / v13+ token 对象），并在任务列表项里避免重复插入复选框。
- 验证（无 AVD、无签名链可装新包，故走「同一份 marked + node」的等价验证）：
  1. 旧写法（设备包内 asset 原文）在 v12 上产出 5 个 `<hundefined></hundefined>`、4 个空 `<li>` —— 复现真机现象；
  2. 新写法产出 5 个带文字的 `<h1>/<h2>/<h3>`、2 个列表项、2 个复选框（`checked` 态保留）；
  3. `:feature:editor:assembleDebug` 与 `:app:assembleDebug` 均 BUILD SUCCESSFUL；从产物 `app-production-debug.apk` 解包确认 `assets/markdown/markdown-preview.html` 已含修复。
- 未能上机复核的原因（如实记录）：设备上已装的 `com.draftpeek` 与本机构建签名不一致，`adb install -r` 报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`；卸载重装会连带清掉设备上 `DraftPeekAudit` 的 28 个走查文件，故不做。装同签名包后可直接复看「预览/分屏标题是否恢复」。

> **2026-11-07 晚补记**：上机障碍已解除——本机 `beta` 构建（minify 开 + debug 签名）可覆盖安装到 `com.draftpeek.dev`。但缺陷 1 的上机复核**仍未完成**，被 §六.4 / §六.5 两处新缺陷挡住（内部文件在文件页不可见、预览态取不到截图），详见 §六.5。

> **2026-11-10 真机上机复核（完成，缺陷 1 关闭）**：设备 RMX5010（Android 16），`adb install -r` 覆盖安装 `app-dev-debug.apk`（1.0.33-dev，`com.draftpeek.dev`），经 SAF 授权 `/sdcard/DraftPeekTest` 打开 `ab_h1_matrix.md`。**预览模式 5 个标题全部可见**（H1 第一块 ZZZ1 / H2 中部 ZZZ2 / H3 中部 ZZZ3 / H1 中部 ZZZ4 / ASCII ONLY H1 ZZZ5），两段正文正常；**分屏模式**左栏 5 个 `#` 与右栏 5 个标题逐项对应。证据：`C:/Users/Doro/Documents/Trae-CN/temp_20261109_01/reports/draftpeek-defect1-device-20261110/01-preview-headings.png`、`02-split-headings.png`。顺带：SAF 授权后文件页正确列出目录与文件（同目录 `03-file-list-ui.xml`），§六.4「列表恒空」修复上机可见。

## 四、未覆盖（明确记录，不假装走完）

- 首启协议弹层：**2026-11-07 晚已覆盖**（用 `com.draftpeek.dev` 1.0.33-dev 全新数据复现，见 §六.1）。
- 文件详情层级（三层 IA 的中间层）：root 空态只能从 `选择文件夹` 进入，未取得详情页。
- ~~`DEX_CRC_BASELINE` / `API30 CreateFileDialogFlowTest` 两项与本走查并列，需另行处理，不在本文范围。~~ → **2026-11-07 已分拆**：`DEX_CRC_BASELINE 恒 0` 已于同日完成构建期接线（`DexCrcBaselineTask` 生成 `assets/dex_crc_baseline.txt`，`./gradlew :app:verifyDexCrc` 三变体全绿，SECURITY.md 已同步）；`API30 CreateFileDialogFlowTest` 仍为独立项，与本走查并列。

## 五、附：设备上 `DraftPeekAudit` 目录归属未查清

`adb shell ls /sdcard/DraftPeekAudit` 报 `No such file or directory`，但 `adb push` 到该路径报 `1 file pushed` 成功；`find /sdcard -name ab_h1_matrix.md` 无结果。即文件确实可被应用读到（列表里 28 个文件都在），但路径不在 `/sdcard` 下的常见位置 → 应用侧 VFS 根目录映射待查（可能是应用私有外部目录或内部存储映射）。

---

## 六、第二轮走查（2026-11-07 晚，`com.draftpeek.dev` 1.0.33-dev）

本节是同日第二轮补充走查，目标是补齐 §四 里原先「未覆盖」的屏 05 / 屏 06–08 / 屏 15–18。设备侧执行方式同上；为避免污染 `com.draftpeek` 1.0.32 与设备上 28 个走查文件，本轮全部在 `com.draftpeek.dev`（本机 debug 构建）上做。

### 六.1 屏 05：首启协议弹层（已完整取证）

复现条件：全新数据（`pm clear` 被 ColorOS 拦，故改用首次安装该包后的第一次启动）。

- onboarding 共 4 页：第 1 页为徽标卡「DraftPeek / Your code, elegantly」，其后 3 页为引导页；右上「跳过」，底部 4 个指示点（末页高亮第 4 点）；末页文案「一切就绪」+ 主按钮「开始使用」。
- 点「开始使用」进入 `MainActivity` 后，立即弹出**「开始之前」底部弹层**：
  - 正文「继续前请阅读并同意以下条款与政策。」；
  - 链接行「《用户协议》·《隐私政策》」；
  - 「展开完整条款」；
  - 复选框「我已阅读并同意《用户协议》《隐私政策》及上述使用要求」；
  - 主按钮「同意并开始」——**未勾选时禁用（灰色），勾选后变主题红可点**。
- 展开后的完整条款正文（逐字）：
  > 继续使用即表示你已阅读并同意《用户协议》与《隐私政策》：不将本工具用于侵权或违法用途；不修复、传播侵犯他人合法权益（含肖像、声音）的内容；不去除内容来源标识或 AI 标识；输出内容对外传播时，遵守所在司法辖区对 AI 生成合成内容的标识要求。
  展开后链接文字变为「收起完整条款」，弹层整体上移。
- **交互陷阱（实测两次）**：「展开完整条款」的 TextView bounds `[70,2223][332,2332]` 与上方「《用户协议》·《隐私政策》」`[70,2202][660,2253]` 在垂直方向重叠，第一次按视觉位置点击会命中复选框行（现象是「复选框被勾上、正文没展开」），第二次点才展开。展开后该行 bounds 变为 `[70,1948][332,2057]`。属可复现的点击靶区缺陷（未登记为新缺陷，先记录现象）。
- 可用坐标（展开态，1264×2780）：复选框行 `[238,2363][1194,2470]`（center 716,2416）｜「同意并开始」`[508,2597][756,2656]`（center 632,2626）。

### 六.2 屏 06–08：文件页 ⋮ 菜单（结论：本构建上不存在）

- 授权前文件页顶栏**只有 3 个图标**，没有任何 ⋮ / 更多入口：
  - `手动排序` `[690,147][858,315]`（center 774,231）；
  - `历史记录` `[872,147][1040,315]`（center 928,231）；
  - `代码片段` `[1054,147][1222,315]`（center 1110,231）。
- 筛选胶囊 7 项：全部 / 最近 / 收藏 / 书签 / 代码 / 文档 / 媒体（可横向滑动，1264 宽下第 7 项「媒体」被右边缘裁切）。
- 底部 tab 仅「文件」「我的」两个。
- 文件行的操作入口是**行内右侧按钮**（1.0.32 上是「保存」，dev 构建上目录行无按钮），**短按不进入详情，长按进编辑器**。
- 授权后（SAF 已授到某目录）顶栏左端多出返回箭头，标题左侧出现面包屑「📁 > Download」「📁 > DraftPeekAudit」。
- `+` FAB 承载新建三件套，菜单项：`导入…`（center 931,1866）、`新建文件夹`（881,2034）、`新建文件`（904,2202，右侧图标 1138,2202）。
  - **可复现缺陷**：`新建文件` 行本身点击常无响应（实测点 904,2202 与 1000,2202 两次无效），必须点右侧图标 1138,2202 才打开对话框。
- 新建文件对话框：文件名输入框（占位「未命名」，右侧固定显示当前扩展名 `.kt`）、副标题「扩展名将根据所选语言自动添加」；语言网格 8 项（Kotlin/Python/JavaScript/Markdown/HTML/CSS/TypeScript/Java）+「全部语言 ▾」；模板 3 项（空文件/Hello World/类模板）；底部「取消」与「创建并打开」（未填名时禁用）。
- **与既有记载的冲突**：`FIX_LOG.md:117` 记「真机走查未覆盖『文件页 ⋮ 菜单（屏 06–08）』」，暗示该 ⋮ 菜单存在待走查；本构建实测**不存在**该入口（与观察项 3 一致）。需业主判定是 IA 改版（⋮ 收进 FAB/行内按钮）还是入口回归。

### 六.3 屏 15–18：编辑器（进入成功，控件结构已取证）

- 进入路径：`历史记录` → 点文件行（`qa-walkthrough-16.md`，center 460,701）→ 编辑器。
- 顶栏结构（1264×2780，y=231）：`返回`(105)｜`编辑`(246) / `预览`(368) / `分屏`(490) / `富文本`(631) 四个模式页签｜`保存`(826)｜`快捷设置`(1008)｜`更多操作`(1164)。
- 二级格式栏（y=424 起）：`粗体`(129)、`斜体`(339)、`删除线`(549)…（末项溢出竖排问题与缺陷 2 一致）。
- **正文区不是 `EditText`**：`uiautomator dump` 里没有任何 `EditText` 节点，`dumpsys input_method` 显示 `mServedView=androidx.compose.ui.platform.AndroidComposeView`、`mServedInputConnection=…NullableInputConnectionWrapperApi34`，即 sora-editor / Compose 自绘输入。因此 `adb shell input text` 打不进正文（实测：键盘弹出、`mInputShown=true`，但文件内容仍为 0 字节）。
- 绕过办法（已验证可行）：`adb push` 样本到 `/data/local/tmp/sample.md` → `run-as com.draftpeek.dev cp` 覆盖 `files/user_files/qa-walkthrough-16.md`（221 B，含 H1/H2/H3、段落、无序/有序列表、引用、行内代码、kotlin 代码块）。
- 但**仍然取不到「预览态」截图**，被 §六.5 的两条新缺陷挡住。

### 六.4 新缺陷：R8 混淆打断 JNI 类查找，正式包 native 安全层整体失效（已修 + 已上机验）

- 现场证据（`com.draftpeek` 1.0.32 进程 logcat）：
  `Rejecting re-init on previously-failed class java.lang.Class<com.draftpeek.security.NativeSecurityChecker>: ExceptionInInitializerError`，`Caused by: java.lang.ClassNotFoundException: com.draftpeek.security.AiDetector`。
  即 `System.loadLibrary("native_security")` 抛穿，`NativeSecurityChecker` 类初始化失败，整个 native 检测层静默失效。
- 根因链：
  1. `app/src/main/cpp/native_security.c` 用硬编码 JNI 路径做 `FindClass` + `RegisterNatives`：`NSC_CLASS="com/draftpeek/security/NativeSecurityChecker"`、`SSR_CLASS="com/draftpeek/core/common/security/SecureStringResolver"`、`AD_CLASS="com/draftpeek/security/AiDetector"`；
  2. `app/proguard-rules.pro` 原先给的是 `-keep,allowobfuscation class com.draftpeek.security.AiDetector` —— **保留了成员但允许改类名**；
  3. `FindClass` 失败时 `JNI_OnLoad` 直接 `LOGW` 后继续/返回，**没有 `ExceptionClear`**，挂起异常冒到 `System.loadLibrary` 调用方，Kotlin 侧 `try/catch (UnsatisfiedLinkError)` 也接不住（异常类型不是它）。
- 静态佐证（逐 dex 查字符串，脚本见 agent 工作区）：
  | APK | dex 数 | `com/draftpeek/security/AiDetector` | `NativeSecurityChecker` | `SecureStringResolver` |
  |---|---|---|---|---|
  | `dp-release-base.apk`（1.0.32 正式，minify 开） | 5 | **0 命中** | 1 命中 | 1 命中 |
  | `app-dev-debug.apk`（minify 关） | 35 | 2 命中 | 命中 | 命中 |
  → 被 R8 改名的正是 `AiDetector`（`-keep,allowobfuscation` 的类），`NativeSecurityChecker` / `SecureStringResolver` 因 JNI 方法被钉住而保住类名。
- 修复（两处）：
  1. `app/proguard-rules.pro`：`AiDetector` / `AiDetector$*` 由 `-keep,allowobfuscation` 改为 **`-keep`**（类名不可混淆），并加注释说明 JNI 硬编码路径与失败后果；
  2. `app/src/main/cpp/native_security.c`：新增 `static void clear_pending(JNIEnv *env)`，在 `JNI_OnLoad` 每一处 `FindClass` / `RegisterNatives` 失败分支后调用（含 NSC 失败路径、SSR 可选路径、AD 可选路径），避免失败时把异常抛穿到 `System.loadLibrary`。
- 验证（真机端到端）：
  1. `:app:assembleDevBeta`（`beta` = initWith release：minify 开 + debug 签名）BUILD SUCCESSFUL，`[dex-baseline] devBeta: 5 个 dex，sum=13498282265`；
  2. 产物 `app-dev-beta.apk` 逐 dex 查字符串：`com/draftpeek/security/AiDetector` **True**（修复前同一 minify 配置下为 0）；
  3. `adb install -r` 到 `com.draftpeek.dev`（Success，`beta` 用 debug 签名故可直接覆盖）→ `monkey` 启动 → logcat 无 `ClassNotFoundException`，且 `libnative_security.so` 加载 ok：
     `D nativeloader: Load …/libnative_security.so … : ok`
     `E m.draftpeek.dev: ----- class 'Lcom/draftpeek/security/AiDetector;' cl=0x252e7c0 -----`（即 `FindClass` 已拿到该类）。
- 影响面（修复前）：所有 minify 构建（`release` / `beta`）的 native 安全层失效，`AiDetector.nativeQuickDexChecksum` / `NativeSecurityChecker` 的 4 个 native 能力全不可用，且因异常被 `catch` 覆盖，界面与日志都不明显。

### 六.5 新缺陷：SAF 授权成功但文件列表恒空；新建文件后不跳编辑器

- SAF 授权链路可走通：`选择文件夹` → `com.coloros.filemanager/com.oplus.filemanager.picker.PickerActivity`「选择文件夹」→「全部文件」→ 存储根（`共 227 项`）→ `Download` → `DraftPeekAudit` → 底栏「使用此文件夹」（center 632,2525）。
- 授权**两级都试过**：只授到 `Download`、以及授到 `Download/DraftPeekAudit`。两种情况下应用内面包屑都正确显示对应目录名（说明 URI 已生效），但**列表恒为「暂无文件 / 点击 + 创建第一个文件」或「未找到匹配的文件」**，切「文档」筛选同样为空。
- 排除了：重启应用（`force-stop` 后回到 `Download` 面包屑且依旧为空）、切换筛选、`shared_prefs` 里无 SAF 根 URI 持久化键（只有 `agreement.xml`、`draftpeek_crash_metrics.xml`）。
- 另一种触发路径同样为空：`选择文件夹` 授权后点面包屑文字（如 `Download`，center 334,736）**不是进目录，而是直接退出当前目录授权**（回到未授权态），说明面包屑不是导航控件 —— 这本身也值得作为 IA 缺陷记录。
- 新建文件路径的问题：`+` → `新建文件` → 选 Markdown → 输入 `qa-walkthrough-16` → 「创建并打开」→ 应用**掉回文件页**（`topResumedActivity` 仍是 `MainActivity`），文件已真实落地 `files/user_files/qa-walkthrough-16.md`（`run-as` 可见），但文件页列表**不刷新也不显示该文件**，随后无法再从 UI 打开它（`历史记录` 在 `force-stop` 后清空为「暂无历史」）。
- 结论：`LocalFileSystemProvider.listFiles` 链路（`supportsUri` → `resolveDocumentFile(uri)` → `documentFile.listFiles().filter { it.exists() }` → 排序）**未定位到具体失败点**：既无异常日志，也拿不到非空列表。下一个怀疑点是 `.filter { it.exists() }` 在 ColorOS SAF provider 上对子项恒 false（`DocumentFile.exists()` 依赖 `name != null`）——但这是**假设，未验证**，需 instrumentation 或在 `listFiles` 里加临时日志才能定论。
- **复核（2026-11-08 S13 报告）**：上述 `LocalFileSystemProvider` 怀疑点**不在主链** —— DI 把 `FileRepository` 绑到 `FileRepositoryImpl`（`BrowserModule`），`LocalFileSystemProvider` 只注册进 `FileSystemRegistry`，生产代码里没有调用点；`FileBrowserViewModel.loadFiles` → `FileRepositoryImpl.listFiles`（`DocumentFile` 树查询）才是文件页唯一数据来源。根因改判为「授权态与列表数据的发布时序」。

#### 六.5-a 处置状态（WO-B22，2026-11-09）

- 采纳方向：**B（授权态上移 ViewModel，与 `uiState` 同源）+ A（授权后显式触发加载）+ C（未授权空 / 已加载空 结构性分离）**。理由与取舍见 `reports/WO-B22-draftpeek-files-fix.md` §3。
- 改动：`FileBrowserViewModel` 新增 `safAccessState`（`NONE/PENDING/GRANTED`），`onTreeUriSelected` 固定为 `PENDING+Loading → 取持久授权 → GRANTED → 首次加载`，首次加载为空再显式补一次；`FileBrowserScreen` 删掉 `rememberSaveable` 的 `hasSafPermission`（组合期置真 + `LaunchedEffect(isInSubdirectory)` 复位），列表分支改按 `currentTreeUri == null` 判定首页；`FileRepositoryImpl.listFiles` 对「树根无法解析 / 不是目录」由静默 `emit(emptyList())` 改为抛错，`FileBrowserViewModel` 归一为 `BrowserUiState.Error`。
- 验证（脚本化，无设备）：`:feature:browser:testDebugUnitTest` **154 例全绿**（新增 `SafDirectoryAccessTests` 6 例，含一条能判红的负控：把授权态发布挪到加载之后，该类恰 1 例变红；实测日志见报告 §5）；`:app:assembleDebug` `rc=0`。
- **真机未验**：本轮没有 AVD 也没有连接设备，走查里那台 ColorOS 上的屏上复核**未做**。设备侧步骤（复制粘贴即可执行）见报告 §7，验收时以该节为准。

## 七、本轮遗留（明确记录，不假装做完）

- 缺陷 1 修复（`5454de2`）**仍未上机复核**：内部文件在文件页不可见（§六.5），文件页里点不进预览，故没有「预览标题恢复」的屏上证据。
  - **2026-11-09 决策**：暂不安排上机复核（无设备），登记为「待所有者安排真机时间」。理由：本轮无 AVD 亦无连接设备，§三 已走「同一份 marked + node」的等价验证（旧写法复现 5 个 `<hundefined>`、新写法产出 5 个带文字标题），代码侧修复已成立；SLO ≥99.5%/月 已接受。触发条件：Doro 安排真机时间后执行 §三 步骤 3 的屏上复看。责任人：Doro。
- 缺陷 2（有序列表竖排）未定位容器代码、未修。
- §六.5 的列表恒空：S13 已把根因改判到「授权态 / 列表发布时序」（`LocalFileSystemProvider` 不在主链），WO-B22 已按该根因改代码 + 补 ViewModel 单测（模块 154 例全绿、`:app:assembleDebug` rc=0）；**真机复核未做**（无设备），步骤见 `reports/WO-B22-draftpeek-files-fix.md` §7 —— 在真机上跑完之前，本条只能算「代码侧已处置」。
- §六.6（安全状态 fail-open，见下）需产品口径确认后决定是否改为三态。

### 六.6 新缺陷：安全状态「✅ 通过」是 fail-open 展示，负向对照未变红

- 负向对照做法：把 `app-dev-beta.apk` 里的 `assets/dex_crc_baseline.txt` 的 `sum=` 改成明显错误值 `12345678901`（实际 `13498282265`），`zipalign -p 4` + 用同一 debug 证书 `apksigner sign` 重签，`adb install -r` 到 `com.draftpeek.dev`，进入「我的 → 关于与安全」。
- 结果：**仍显示「DEX 完整性：✅ 通过」**，未变「❌ 已修改」。即该行的 fail 路径没有被触发。
- 代码链路：`feature/settings/.../AboutSecurityScreen.kt:111-116` 读 `AiProtectionStateHolder.current.triggeredSignals`，只有 `AiDetectionSignal.DEX_TAMPERED in triggeredSignals` 才显示 ❌；而 holder 的默认值是空集。`AiDetector.assess()` 在 `DraftPeekApp.kt:360-381` 的后台协程里跑，**整段被 `try/catch(Exception)` 包住**，一旦前面某个检测抛出，`AiProtectionStateHolder.update(state)` 永不执行，界面就停在默认「通过」。
- 两条候选根因（**未区分**，需进一步取证）：
  1. `assess()` 早期检测抛出 → holder 未更新 → 默认「通过」（fail-open）；
  2. `verifyDexIntegrity` 自身异常/返回空集（`readDexCrcBaseline` 的 `catch` 一律返回 0 = 跳过校验）。
- 影响：安全状态页在「未检测 / 检测失败」两种情况下都显示「通过」，是安全语义上的假绿；这也意味着**构建期 DEX CRC 接线的运行时有效性尚未被证明**（构建期闸门 `:app:verifyDexCrc` 与 17 例单测是有效的，运行时这一侧不成立）。
