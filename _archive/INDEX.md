# DraftPeek 归档索引（仓根 `_archive/`）

> 归档区按治理规范 §12.1 使用仓根 `_archive/`（下划线前缀＝非活动区，§10 审计自动豁免）。
> 本仓 `.gitignore` 原将 `_archive/` 整目录排除（该目录同时存放临时文件、日志、克隆仓、设计资产、旧计划等本地暂存物）。本次改为 `_archive/*` 并放行 `_archive/INDEX.md` 与 `_archive/执行记录/`——既让治理归档可入库，也让上述本地暂存物继续被忽略。
> 归档只移动文件、不改写正文；`docs/README.md` 第 53 行的 reports 汇总已改为「3 篇活动 + 5 篇已归档」。
> 本仓另有 `docs/_devarchive/`（被 `.gitignore` 忽略的本地开发归档），按 §13.1 保留原位、不入库、本次不迁移。

| 原路径 | 归档后路径 | 归档日 | 类别 | 一句话结论 | 改链位置 |
|---|---|---|---|---|---|
| `docs/reports/测试体系评估报告_v1.0.30.md` | `_archive/执行记录/2026-11/测试体系评估报告_v1.0.30.md` | 2026-11-08 | 一次性评估（版本快照） | v1.0.30（10030）时点的测试体系评估，2026-09-04 出具，绑定 `文件:行号` 证据 | `docs/README.md:53`；原引用方 `2026-09-agent-memory-archive.md` 同批同目录移动，相对链接仍有效 |
| `docs/reports/DraftPeek_前端工程体系评估报告_v1.0.30.md` | `_archive/执行记录/2026-11/DraftPeek_前端工程体系评估报告_v1.0.30.md` | 2026-11-08 | 一次性评估（版本快照） | v1.0.30 时点前端工程体系评估（Compose + Material 3 + 自研 DesignSystem），2026-09-04 | `docs/README.md:53`（同上） |
| `docs/reports/BUILD_PERF_REPORT.md` | `_archive/执行记录/2026-11/BUILD_PERF_REPORT.md` | 2026-11-08 | 一次性测量 | 2026-09-13 构建性能分析与优化（32 核 / 32GB / JDK21 / Gradle 8.14.3，`assembleDevDebug`） | `docs/README.md:53` |
| `docs/reports/学习成果落地分析报告.md` | `_archive/执行记录/2026-11/学习成果落地分析报告.md` | 2026-11-08 | 一次性分析 | 2026-08-25 对已移除的 `docs/repo-analysis/` 学习报告做的系统性落地分析，分析对象已不在本克隆 | `docs/README.md:53` |
| `docs/reports/2026-09-agent-memory-archive.md` | `_archive/执行记录/2026-11/2026-09-agent-memory-archive.md` | 2026-11-08 | 执行记录（记忆归档） | 把 `.workbuddy/`（git-ignored）中 2026-09 记忆与报告按主题合并入库；过期结论保留并标注复核现状（2026-11-04） | `docs/README.md:53` |

## 未归档（活动文档，判定理由）

| 文档 | 保留理由 |
|---|---|
| `docs/reports/2026-10-15-覆盖率爬坡计划.md` | 90 天爬坡计划（2026-10-15 起，尚未到期），带阈值与实测基线，仍在执行期 |
| `docs/reports/2026-10-15-架构评审台账.md` | 台账本体：架构与重大决策「评审 → 决议 → 回流」登记处，与 `docs/adr/` 分工并存 |
| `docs/reports/2026-10-15-失败签名机制与排障路径摘要.md` | 现行机制说明：把「同一失败签名禁止同方向再试」与 TOP 排障路径从仅本地转成入库口径 |
| `docs/release-version-management-evaluation.md` | 2026-09-04 评估，但被 `docs/CODING_STANDARDS.md:107`、`docs/DATABASE_MIGRATION_STRATEGY.md:110`、`docs/deploy-download-page/README.md:6` 作为「版本集中化依据」引用；`docs/README.md:44` 亦标为「版本集中化依据」——属决策依据而非纯历史 |
| `docs/qa/2026-11-07-walkthrough/`（`WALKTHROUGH.md` + 4 张 png） | 2026-11-07 真机走查（RMX5010 / Android 16）证据，是当前整改依据，属活动证据链 |
| `docs/phaseb-oauth-wip-todo.md` | Phase B OAuth 未完成事项清单 |
| `docs/DATABASE_MIGRATION_STRATEGY.md`、`docs/RELEASE_CHECKLIST.md`、`docs/VERSIONING.md`、`docs/HANDOVER_GUIDE.md`、`docs/SRE_RUNBOOK.md`、`docs/CODING_STANDARDS.md`、`docs/release-governance.md`、`docs/agent-contract.md` | 现行规范 / 清单 / 手册 |
| `docs/agents/GOTCHAS.md`（55KB）、`docs/agents/CODE_STYLE.md` | 坑点台账与代码风格（长期维护） |
| `docs/PRIVACY_POLICY.md`、`docs/USER_AGREEMENT.md`、`docs/THIRD_PARTY_NOTICES.md`、`docs/SECURITY_AUDIT_DraftPeek.md` | 法律文本与安全审计（审计结论支撑现行合规面） |
| `docs/adr/0002-editor-sync-crdt.md`、`docs/project/ADR-001-CRDT-vs-OT-Selection.md` | ADR（§12.5 条款 8：一律不迁移） |
| `docs/icon-design/`（含 `archive/g34r`、`archive/initial-2026-08-15`、60+ 个 `patch*.js`、`icon-export-g34o`） | 图标设计资产与设计阶段脚本：`docs/icon-design/archive/` 已是 docs 内历史区（存量保留原位）；脚本与当前导出产物同目录配套，迁移会断开生成链 |
| `docs/assets/readme-*.png/svg` | README 展示图（治理 §8.1 指定落点） |
| `docs/deploy-download-page/`、`docs/ci/branch-protection.json` | 下载页部署件与分支保护配置 |

## 例外登记

| 位置 | 说明 |
|---|---|
| `docs/_devarchive/` | `.gitignore` 忽略的本地开发归档（不入库），按既有口径保留原位 |
| `docs/icon-design/export-snapshot-index.md`、`docs/screenshots/` | 工作区未跟踪文件（用户本地产物），本轮未纳入任何提交 |
| `CHANGELOG.md`、`FIX_LOG.md` 中对上述 5 篇的提及 | 发布与修复历史，按既定口径不改写正文 |
