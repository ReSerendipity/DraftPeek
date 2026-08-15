# DraftPeek 第三方组件与许可声明（Third-Party Notices）

> 本文档列明 DraftPeek 集成的第三方代码、二进制与资源及其许可义务。
> 凡"待确认"项，以对应上游仓库 LICENSE 为准，随版本升级定期复核。
> 最后更新：2026-08-12

## 1. 主要代码依赖

| 组件 | 用途 | 许可 | 备注 |
|---|---|---|---|
| sora-editor（Rosemoe） | 代码编辑器（TextMate + Tree-sitter） | Apache-2.0（以仓库 LICENSE 为准） | 40+ 语言高亮 |
| com.github.mwiede:jsch | SFTP/SSH 客户端 | BSD-3-Clause（0.2.x 为 mwiede fork） | |
| Room / DataStore | 本地数据层 | Apache-2.0 | |
| SQLCipher | 数据库加密 | BSD-3-Clause（Zetetic 开源版，以仓库为准） | |
| Apache POI | Word/Excel/PPT 预览 | Apache-2.0 | |
| java-diff-utils | Diff | Apache-2.0 | |
| Hilt / KSP | 依赖注入 | Apache-2.0 | |
| CommonMark（commonmark-java） | Markdown 解析 | BSD-2-Clause | |
| Jetpack Compose / Material 3 | UI | Apache-2.0 | |

> 上述均为宽松许可（Apache/BSD/MIT 类），与闭源发布兼容；再分发时保留各库的许可文本与版权声明即可。

## 2. 终端功能 GPL 组件（方案 A：随包分发 + 合规声明）

DraftPeek 终端基于 proot 提供无 root 的 Linux 环境。**proot 及其配套组件为 GPL 许可，已按方案 A 随 APK 分发并履行义务：**

| 组件 | 来源 | 许可 | 义务履行 |
|---|---|---|---|
| proot / proot-care | https://github.com/proot-me/proot | GPL-2.0 | ✅ 许可文本随包（`app/src/main/assets/licenses/GPL-2.0.txt`）；✅ 应用内"我的 → 关于 → 开源许可"提供源码链接与文本入口 |
| busybox（rootfs 内） | https://busybox.net | GPL-2.0 | ⚠️ 若打包进 rootfs：在开源许可入口补充 busybox 声明（当前尚未打包 rootfs） |
| rootfs（最小 Linux 文件系统） | 待定（若取自 Termux bootstrap 则整体为 GPL-3.0） | 视来源 | ⚠️ 打包前记录来源与许可，随包声明 |

**规则**：
- proot/busybox 作为独立 GPL 组件捆绑分发（进程调用、非链接），主程序保持闭源合法；
- 禁止：修改 GPL 组件源码后闭源分发、移除其版权声明；
- rootfs 引入时同步更新本表与开源许可入口。

## 3. 调研用源码目录（repos/）

`repos/` 下 30+ 竞品/参考仓库源码仅作本地调研（已 gitignore、不参与编译、不随 APK 分发），不构成再分发，无许可义务；**禁止将其中 GPL 许可项目的代码片段直接复制进主代码库**（如 termux-app 为 GPL-3.0、Xed-Editor 为 GPL-3.0，仅可参考思路）。

## 4. 其他

- 图标与品牌资源：DraftPeek 自有，无第三方许可；
- **新增依赖时**：同步更新本表；发布前建议启用依赖许可检查（Licensee / gradle license-report 插件）。
