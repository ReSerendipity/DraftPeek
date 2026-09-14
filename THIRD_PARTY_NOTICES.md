# DraftPeek 第三方组件与许可声明（Third-Party Notices）

> 本文档列明 DraftPeek 集成的第三方代码、二进制与资源及其许可义务。
> 凡"待确认"项，以对应上游仓库 LICENSE 为准，随版本升级定期复核。
> 最后更新：2026-08-16（合规整改：sora-editor 系列许可更正为 LGPL-2.1；proot 状态更正为"未随包分发"；补充随包资源清单与许可文本）

## 1. 主要代码依赖

| 组件 | 版本 | 用途 | 许可 | 备注 |
|---|---|---|---|---|
| sora-editor（Rosemoe） | 0.24.6 | 代码编辑器核心（TextMate + Tree-sitter） | **LGPL-2.1** | 40+ 语言高亮；义务见 docs/COMPLIANCE_CHECKLIST.md（本地文档，未随仓库发布） |
| language-textmate（Rosemoe） | 0.24.6 | TextMate 语法引擎 | **LGPL-2.1** | 同上 |
| language-treesitter（Rosemoe） | 0.24.6 | Tree-sitter 语言支持 | **LGPL-2.1** | 同上 |
| android-tree-sitter（AndroidIDE） | 4.3.2 | Tree-sitter JNI 绑定 | **LGPL-2.1** | 同上 |
| tree-sitter-java（AndroidIDE） | 4.3.2 | Java 语法 grammar | **LGPL-2.1** | 同上 |
| com.github.mwiede:jsch | 0.2.24 | SFTP/SSH 客户端 | BSD-3-Clause | |
| Room / DataStore | — | 本地数据层 | Apache-2.0 | |
| SQLCipher（net.zetetic） | 4.6.0 | 数据库加密 | BSD-3-Clause | |
| Apache POI | 5.3.0 | Word/Excel/PPT 预览 | Apache-2.0 | |
| java-diff-utils | 4.12 | Diff | Apache-2.0 | |
| Hilt / KSP | — | 依赖注入 | Apache-2.0 | |
| CommonMark（commonmark-java） | 0.24.0 | Markdown 解析 | BSD-2-Clause | |
| Jetpack Compose / Material 3 | — | UI | Apache-2.0 | |
| richeditor-compose | — | WYSIWYG Markdown 编辑 | Apache-2.0（以仓库为准） | |
| JGit | 6.10.1 | Git 支持 | EDL-1.0（Eclipse Distribution License，以仓库为准） | |
| LSP4J | — | LSP 协议层 | EPL-2.0（以仓库为准） | |
| tm4e（含 joni/jcodings 传递依赖） | — | TextMate 语法解析（sora-editor 依赖） | EPL-2.0（以仓库为准） | |

> LGPL-2.1 组件以可替换方式链接，主程序无需开源；再分发须随附许可文本、保留版权声明，并保证用户可用修改后的 LGPL 组件替换重新链接（详见 docs/COMPLIANCE_CHECKLIST.md（本地文档，未随仓库发布））。
> Apache/BSD/MIT/EPL 类为宽松许可，与本仓库 Apache-2.0 开源发布兼容；再分发时保留各库的许可文本与版权声明即可。

## 2. 随包分发的前端资源（Markdown 预览）

| 组件 | 版本 | 随包位置 | 许可 | 版权/备注 |
|---|---|---|---|---|
| KaTeX | 0.16.9 | feature/editor/src/main/assets/markdown/katex.min.js、katex.min.css | MIT | 版权行以包内文件为准（Khan Academy 等贡献者） |
| KaTeX 字体（woff2 字库） | 随 KaTeX | feature/editor/src/main/assets/markdown/fonts/ | SIL OFL-1.1 | 许可全文 assets/licenses/OFL-1.1.txt |
| Mermaid | 11.13.0（包内版本号） | feature/editor/src/main/assets/markdown/mermaid.min.js | MIT | 版权行以包内文件为准 |
| marked | 12.0.0 | feature/editor/src/main/assets/markdown/marked.min.js | MIT | Copyright (c) 2011-2024, Christopher Jeffrey（见包内文件头） |
| highlight.js | 11.9.0 | feature/editor/src/main/assets/markdown/highlight.min.js | BSD-3-Clause | (c) 2006-2023 contributors（见包内文件头） |
| TextMate 语法文件（40+ 语言） | — | app/src/main/assets/textmate/** | MIT | 源自 VS Code 生态；各语法以来源仓库 LICENSE 为准 |
| Inter 字体（4 字重） | — | core/ui、core/designsystem 的 res/font/ | SIL OFL-1.1 | 许可全文 assets/licenses/OFL-1.1.txt |
| JetBrains Mono 字体（3 字重） | — | core/ui、core/designsystem 的 res/font/ | SIL OFL-1.1 | 同上 |

> KaTeX/Mermaid/marked/highlight.js 为打包的 min 构建，版本号取自包内元数据；升级随包资源时须同步更新本表并保留其许可文本。

## 3. 终端功能 GPL 组件（proot：当前未随包分发）

DraftPeek 终端模块实现了基于 proot 的会话管理框架（ProotSessionManager），但 **proot 二进制与 rootfs 目前均未随 APK 分发**——仓库中不存在 `assets/proot/` 与 `assets/rootfs/`（仅存在按此架构编写的加载代码）。此前文档"已按方案 A 随 APK 分发并履行义务"的表述与仓库实际不符，本版予以更正。

| 组件 | 来源 | 许可 | 当前状态与义务 |
|---|---|---|---|
| proot / proot-care | https://github.com/proot-me/proot | GPL-2.0 | **未随包分发**。若后续捆绑分发：须随包附 GPL-2.0 全文（`assets/licenses/GPL-2.0.txt` 已备）、提供源码获取方式（应用内开源许可页 + 本文档）、保留版权声明 |
| busybox（rootfs 内） | https://busybox.net | GPL-2.0 | 当前未打包 rootfs；打包前补充声明与许可文本 |
| rootfs（最小 Linux 文件系统） | 待定 | 视来源 | 打包前记录来源与许可，随包声明 |

**规则**：
- proot/busybox 若作为独立 GPL 组件捆绑分发（进程调用、非链接），主程序保持闭源合法，但必须满足 GPL-2.0 分发条件（附许可全文、提供源码获取方式）；
- 禁止：修改 GPL 组件源码后闭源分发、移除其版权声明；
- rootfs 引入时同步更新本表与开源许可入口。

## 4. 应用内开源许可入口

- 入口：我的 → 关于 → 开源许可（ProfileScreen），列出全部第三方组件、许可与版本；
- 点击条目查看随包许可全文（读取 assets/licenses/）；proot 条目标注"未随包分发"并提供源码链接；
- 许可文本随包位置：`app/src/main/assets/licenses/`（GPL-2.0.txt、LGPL-2.1.txt、MIT.txt、OFL-1.1.txt、BSD-3-Clause.txt、Apache-2.0.txt）。

## 5. LGPL-2.1 义务（可重链接）

- R8 keep：`app/proguard-rules.pro` 已为 sora-editor（io.github.rosemoe.**）、tree-sitter（com.itsaky.androidide.treesitter.**、io.github.treesitter.**）及 tm4e/joni/jcodings 保留类名与成员（不重命名、不裁剪），满足 LGPL-2.1 第 6 条"用户可用修改后的库版本替换随包版本并重新链接"的要求；
- 修改后的 LGPL 库重新构建路径见 docs/COMPLIANCE_CHECKLIST.md（本地文档，未随仓库发布）。

## 7. 其他

- 图标与品牌资源：DraftPeek 自有，无第三方许可；
- **新增依赖时**：同步更新本表；发布前建议启用依赖许可检查（Licensee / gradle license-report 插件）。