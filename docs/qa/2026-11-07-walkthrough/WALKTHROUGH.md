# 真机走查记录 — 2026-11-07

设备：RMX5010 / Android 16 / SDK 36 / 1264×2780（真机，非 AVD；本机无 AVD）
安装包：`com.draftpeek` 1.0.32（debug，已装于该设备）
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
| 关于与安全 | 安全状态（签名校验 ✅ 已验证官方、DEX 完整性 ✅ 通过、运行时环境 安全）、重新校验、导出安全事件报告、清空安全事件日志；关于（验证应用、版本 1.0.32、开源许可） | 正常 | —（现场观察） |

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

## 四、未覆盖（明确记录，不假装走完）

- 首启协议弹层：需要清除应用数据回到首次启动才能触发；该设备上存有走查用测试文件（`DraftPeekAudit` 下 28 个），清数据会一并抹掉，未做。
- 文件详情层级（三层 IA 的中间层）：root 空态只能从 `选择文件夹` 进入，未取得详情页。
- ~~`DEX_CRC_BASELINE` / `API30 CreateFileDialogFlowTest` 两项与本走查并列，需另行处理，不在本文范围。~~ → **2026-11-07 已分拆**：`DEX_CRC_BASELINE 恒 0` 已于同日完成构建期接线（`DexCrcBaselineTask` 生成 `assets/dex_crc_baseline.txt`，`./gradlew :app:verifyDexCrc` 三变体全绿，SECURITY.md 已同步）；`API30 CreateFileDialogFlowTest` 仍为独立项，与本走查并列。

## 五、附：设备上 `DraftPeekAudit` 目录归属未查清

`adb shell ls /sdcard/DraftPeekAudit` 报 `No such file or directory`，但 `adb push` 到该路径报 `1 file pushed` 成功；`find /sdcard -name ab_h1_matrix.md` 无结果。即文件确实可被应用读到（列表里 28 个文件都在），但路径不在 `/sdcard` 下的常见位置 → 应用侧 VFS 根目录映射待查（可能是应用私有外部目录或内部存储映射）。
