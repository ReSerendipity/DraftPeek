# DraftPeek — 文档与项目速览

> Android 原生代码/文本编辑器（Jetpack Compose + Kotlin 多模块）。内置终端、Markdown 预览、多语言语法高亮；`server/` 为可选的实验性 Python 协作同步服务（非默认启用、非生产部署）。
> 入口：Gradle 多模块（应用壳 `app/`）；构建与脚本见根 `README.md`。版本与发布规范见 `docs/VERSIONING.md`。

## 快速了解本项目

- **做什么**：Android 原生代码/文本编辑器（40+ 语言高亮、Markdown 编辑/预览、集成终端、FTP/SFTP 文件浏览、Git 集成、实验性 LSP）。
- **技术栈**：Kotlin · Jetpack Compose · Room(SQLCipher) · Hilt · sora-editor(TextMate + Tree-sitter)；`server/` 为 Python CRDT 同步（实验性）。
- **如何构建**：`./gradlew assembleDebug`；发布流程见 `docs/VERSIONING.md` 与 `docs/RELEASE_CHECKLIST.md`。

## 目录结构速览

| 目录 | 内容 |
|---|---|
| `app/` | 应用壳模块（MainActivity、导航、安全模块、AppWidget） |
| `core/` | 基础层（common / crdt / data / domain / designsystem / sync / testing / ui） |
| `feature/` | 功能模块（browser / editor / knowledge / settings / stats / terminal） |
| `benchmark/` | Macrobenchmark 性能基准 |
| `build-logic/` | Gradle 约定插件 |
| `gradle/` | wrapper 与版本目录（libs.versions.toml） |
| `migrations/` | 数据库迁移脚本（SQL） |
| `server/` | 可选实验性 Python 协作同步服务（crdt / integrity / sync + pytest） |
| `docs/` | 项目文档（见下方索引） |
| `scripts/` | 构建与辅助脚本 |

## docs/ 索引（本目录）

| 文件 / 子目录 | 存什么 |
|---|---|
| `VERSIONING.md` | 版本号规范、versionCode 公式、发布/灰度/回滚、hotfix 与 SLA |
| `RELEASE_CHECKLIST.md` | 发布前人工检查清单（与 CI 门禁分工） |
| `DATABASE_MIGRATION_STRATEGY.md` | Room / SQLCipher 迁移策略 |
| `release-version-management-evaluation.md` | 发布版本管理评估（版本集中化改造依据） |
| `SECURITY_AUDIT_DraftPeek.md` | 安全审计 |
| `adr/` | 架构决策记录（含 CRDT vs OT 选型） |
| `deploy-download-page/` | 下载页部署说明 |
| `icon-design/` | 应用图标设计与导出 |
| `reports/` | 测试体系 / 前端工程评估报告；架构评审台账、覆盖率爬坡计划（2026-10-15 起） |
| `screenshots/` | 界面截图 |

## 想找内容？

- 改编辑器 → `feature/editor/`；统计看板 → `feature/stats/`；设计系统 → `core/designsystem/`；数据层 → `core/data/`
- 发版 → `docs/VERSIONING.md` + `docs/RELEASE_CHECKLIST.md`

## 仓外规格本体处置声明（T4 · 2026-10-11）

> **决策**：阶段 B 规格本体（`draftpeek-phaseb-design-spec-v1.md`、`draftpeek-phaseb-taskbook-v1.md`、`draftpeek-phaseb-t0-receipt.md`）**保持仓库外、不纳入本仓库快照**，本仓库显式 skip。

**理由**（显式记录，防未来误判为「缺失」）：

1. **外部引用方**：上述文档正被另一执行 agent 的阶段 B 工作流实时引用（规格 v1.1 / 阶段 B 批次），纳入快照会产生双源漂移（仓库内副本 vs 外部工作流主档），以哪份为准不可判定。
2. **无实体可复制**：方案 (a)「复制纳入版本库」需要外部文件实体的可读访问；当前会话可访问位置（本仓库 + 本机已知目录）中无该批文件实体，复制无源。
3. **禁止移动/删除**：指令明确禁止移动/删除外部文件；本仓库仅以引用锚点方式衔接——仓库内 `docs/phaseb-oauth-wip-todo.md`（已入库）记录了任务书 §6②–⑤、§7、§9、§2.5、§1 的引用与阶段 B 范围裁定，版本锚点即该文件所述提交（T0 骨架 `d2b407c`、降级 `c51ddca`/`b6a9010`）。
4. **可逆**：未来如需纳入快照（外部工作流冻结后），可从外部主档一次性复制并声明版本锚点，本声明区块同步改写。

**影响面**：fresh clone / CI 不会携带阶段 B 规格本体；任何依赖该批文档的任务须先从外部工作流获取，或读取 `docs/phaseb-oauth-wip-todo.md` 的引用摘要。

> 特别提醒：根目录 `release.jks` 是 App 签名密钥（敏感，勿提交勿外传）；`index.html` 是被源码注释引用的 HTML 设计原型，保留。**分发边界**：根 `AGENTS.md` 及其家族（`feature/editor/AGENTS.md`、`core/common/AGENTS.md`、`docs/agents/**`、`FIX_LOG.md`、`LOCAL_RULES.md`）为**仅本地保留、有意不入库**，不会出现在 fresh clone；仓库对外开发规范以本 `docs/README.md` 与 `docs/CODING_STANDARDS.md` 为准，若环境已配置 `AGENTS.md` 则在其基础上叠加。**机制已核对（2026-10-14）**：`git check-ignore -v AGENTS.md` 命中 `.gitignore` 的 `*.md`，`git ls-files -- '*AGENTS.md'` 为空 —— `.gitignore` 未为该家族开过任何 `!` 豁免。原先只写在 `AGENTS.md` 里的规范（覆盖率门禁 ratchet、单测平台/注解约定、R8 模式、本分发边界）已落到 **`docs/CODING_STANDARDS.md` 第 7 节**，入库文件引用规范时指向第 7 节，不再指向 fresh clone 取不到的 `AGENTS.md`。

## Server 改动验证门禁（2026-10-14 裁决，权威条目）

> 裁决：`Server CI (Python)` **维持非必需检查分级**（不在分支保护 required contexts 之列，与
> `docs/ci/branch-protection.json`、`.github/workflows/main-verify.yml` 口径一致；升为必需需
> 仓库 owner 单独授权）。作为替代，改动 `server/**` 有一条**有主的本地阻断式验证路径**：

- **强制规则**：任何改动 `server/**`（含依赖 pin `server/requirements*.txt`、`server/ruff.toml`）的提交，
  **提交前必须**在 `server/` 目录跑：

  ```bash
  cd server && pytest tests -q   # 须 exit 0 全绿才可提交（2026-10-14 实测：40 passed）
  ```

  与 CI（`.github/workflows/server-ci.yml`）的测试步骤同源；本地建议加跑 `ruff check .`。
- **有主**：规则守门人 = 仓库 owner（ReSerendipity）；执行义务归改动 `server/**` 的提交者本人。
  admin 直推 main 豁免远端检查，本地门禁是对其唯一阻断点；非 admin 走 PR 时 `Server CI (Python)`
  会额外提供远端回归信号（非阻断）。
- **红灯处置**：
  - **升级**：测试红且无法在本次改动内修复 → 禁止提交/合并，开 issue 并在 PR 描述写明失败签名，
    升级至 owner 裁决；owner 可授权暂时以 `--no-verify` 推送（仅限明确授权，事后必须补绿）。
  - **回退**：红灯由已入库提交引起 → `git revert` 回最近绿灯提交（revert 是止损不是失败），
    再按需修复；不得为过本地门禁而删改 `server/tests/**` 断言来制造绿灯。
- **升为必需检查的前置条件**（若将来改变裁决）：先去掉 `server-ci.yml` push 档的 `paths` 过滤或
  保证每个 main 提交都有该 context，否则不触碰 server/ 的 PR 会永久 Expected-BLOCKED；再同步
  `docs/ci/branch-protection.json` 的 contexts 与 `main-verify.yml` 的 CONTEXT_OWNERS（owner 已预登记），
  并由 owner 授权跑 `python scripts/apply_branch_protection.py --apply` 写回远端。

## 治理与勘误速查（2026-09-18）

- **日志（勘误用）**：`server/logs/{sync_server,crdt_server,integrity_server}.log`（RotatingFileHandler 5MB×3，配置在 `server/_logging_setup.py`；CRDT 服务设 `CRDT_AUDIT_LOG=1` 可输出审计级日志；日志文件在服务首次运行后生成）。
- **根目录守卫**：新增根目录文件必须登记 `.github/layout-rules.yaml → root_allowlist`（structure-guard；2026-09-17 已补 `.gitattributes` / `.workbuddy` / `gitleaks.toml`）。
- **提交**：Conventional Commits + DCO（`git commit -s`，commit-msg 钩子校验）。
- **钩子**：`.githooks/pre-commit` 分发器四级回退；pre-push gitleaks 扫描。
