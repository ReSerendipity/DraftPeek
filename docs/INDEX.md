# 文档索引（docs/INDEX）

> 最后核对：2026-11-10　｜　维护：文档整理工单 WO-DOC-B01
> 本文件是 `docs/` 全量 tracked Markdown 的机器可读索引（26 篇：顶层 14 + `adr/` 2 + `reports/` 3 + `qa/` 1 + `icon-design/` 4 + `deploy-download-page/` 1 + `README` 1）。
> [`README.md`](README.md) 保留为人类阅读的项目速览（目录结构、治理速查、门禁裁决、T4 处置声明）；两者冲突时以本文件为准。
> 本仓分发边界：根 `AGENTS.md` 家族（`feature/editor/AGENTS.md`、`core/common/AGENTS.md`、`docs/agents/**`、`FIX_LOG.md`、`LOCAL_RULES.md`）**仅本地保留、有意不入库**（`.gitignore` 的 `*.md` 规则，2026-10-14 已核对），本文件不把它们列入 tracked 清单。

## 状态口径

| 状态 | 含义 |
|---|---|
| 现行 | 仍在指导当前工作，或仍是某项规则、清单、规范的有效来源 |
| 历史 | 时点记录（走查、评审、审计、评估），只作追溯 |
| 废弃 | 已被后继文档取代，或文件自身声明作废 |
| WIP | 文件自述未定稿或有未完成事项 |

## 一、仓根入口文档

| 文档 | 用途 | 状态 | 核对日期 |
|---|---|---|---|
| [`README.md`](../README.md) | 项目总入口：能力、截图、构建与发布、贡献入口 | 现行 | 2026-11-10 |
| [`ARCHITECTURE.md`](../ARCHITECTURE.md) | 分层架构与模块关系（Compose 多模块） | 现行 | 2026-11-10 |
| [`CHANGELOG.md`](../CHANGELOG.md) | 版本变更记录 | 现行 | 2026-11-10 |
| [`CONTRIBUTING.md`](../CONTRIBUTING.md) | 贡献指南 | 现行 | 2026-11-10 |
| [`CODE_OF_CONDUCT.md`](../CODE_OF_CONDUCT.md) | 行为准则 | 现行 | 2026-11-10 |
| [`SECURITY_MATRIX.md`](../SECURITY_MATRIX.md) | 安全控制措施对照 | 现行 | 2026-11-10 |
| [`.github/SECURITY.md`](../.github/SECURITY.md) | 安全漏洞披露渠道 | 现行 | 2026-11-10 |
| `AGENTS.md` / `FIX_LOG.md` / `LOCAL_RULES.md` | 编码代理契约与本地台账：**仅本地保留、不入库**（引用规范时以 `docs/CODING_STANDARDS.md` §7 为准） | 本地 | 2026-11-10 |

## 二、docs/ 顶层（版本、发布、合规、安全、规范）

| 文档 | 用途 | 状态 | 核对日期 |
|---|---|---|---|
| [`README.md`](README.md) | docs 项目速览：目录结构、docs 索引、门禁裁决、T4 处置声明、观察项 | 现行 | 2026-11-10 |
| [`VERSIONING.md`](VERSIONING.md) | 版本管理规范（已收敛为指针 → `release-governance.md`） | 现行 | 2026-11-10 |
| [`release-governance.md`](release-governance.md) | 发布总纲：版本位、灰度、回滚、§5.1 tag 可达性对照 | 现行 | 2026-11-10 |
| [`RELEASE_CHECKLIST.md`](RELEASE_CHECKLIST.md) | 发布前人工检查清单（与 CI 门禁分工） | 现行 | 2026-11-10 |
| [`CODING_STANDARDS.md`](CODING_STANDARDS.md) | 可移植性 / 依赖 / Git 卫生 + §7 交付体规范单一来源 | 现行 | 2026-11-10 |
| [`DATABASE_MIGRATION_STRATEGY.md`](DATABASE_MIGRATION_STRATEGY.md) | Room / SQLCipher 迁移策略 | 现行 | 2026-11-10 |
| [`HANDOVER_GUIDE.md`](HANDOVER_GUIDE.md) | 接管者指引（最小可执行起点） | 现行 | 2026-11-10 |
| [`SRE_RUNBOOK.md`](SRE_RUNBOOK.md) | `server/` 日志落点与勘误指引 | 现行 | 2026-11-10 |
| [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) | 第三方组件与许可声明 | 现行 | 2026-11-10 |
| [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) | 隐私政策 | 现行 | 2026-11-10 |
| [`USER_AGREEMENT.md`](USER_AGREEMENT.md) | 用户协议 | 现行 | 2026-11-10 |
| [`SECURITY_AUDIT_DraftPeek.md`](SECURITY_AUDIT_DraftPeek.md) | 安全审计（2026-09-01，含 2026-10-21 整改复核） | 历史 | 2026-11-10 |
| [`release-version-management-evaluation.md`](release-version-management-evaluation.md) | 发布版本管理评估（2026-09-04，版本集中化依据） | 历史 | 2026-11-10 |
| [`agent-contract.md`](agent-contract.md) | Agent 契约 v2（与组织模板逐字节一致） | 现行 | 2026-11-10 |
| [`phaseb-oauth-wip-todo.md`](phaseb-oauth-wip-todo.md) | 阶段 B OAuth 临时降级 TODO（阶段 B 规格本体的仓内引用锚点） | WIP | 2026-11-10 |

## 三、docs/adr/（架构决策记录）

| 文档 | 用途 | 状态 | 核对日期 |
|---|---|---|---|
| [`adr/README.md`](adr/README.md) | ADR 索引与格式约定 | 现行 | 2026-11-10 |
| [`adr/0002-editor-sync-crdt.md`](adr/0002-editor-sync-crdt.md) | ADR-0002：编辑器同步采用 CRDT（ADR-0001 为本地原件，正文副本即本篇） | 现行 | 2026-11-10 |

## 四、docs/reports/（评审与爬坡计划）

| 文档 | 用途 | 状态 | 核对日期 |
|---|---|---|---|
| [`reports/2026-10-15-架构评审台账.md`](reports/2026-10-15-架构评审台账.md) | 2026-10-15 架构评审台账（问题项与处置状态） | 现行 | 2026-11-10 |
| [`reports/2026-10-15-覆盖率爬坡计划.md`](reports/2026-10-15-覆盖率爬坡计划.md) | 覆盖率爬坡计划（ratchet 目标与批次） | 现行 | 2026-11-10 |
| [`reports/2026-10-15-失败签名机制与排障路径摘要.md`](reports/2026-10-15-失败签名机制与排障路径摘要.md) | 失败签名机制与排障路径摘要 | 现行 | 2026-11-10 |

> 5 篇一次性评估/分析报告（BUILD_PERF、前端工程体系评估、测试体系评估、学习成果落地分析、2026-09 记忆归档）已归档至 [`_archive/执行记录/2026-11/`](../_archive/执行记录/2026-11/)，索引见 [`_archive/INDEX.md`](../_archive/INDEX.md)。

## 五、docs/qa/（真机走查）

| 文档 | 用途 | 状态 | 核对日期 |
|---|---|---|---|
| [`qa/2026-11-07-walkthrough/WALKTHROUGH.md`](qa/2026-11-07-walkthrough/WALKTHROUGH.md) | 2026-11-07 真机走查记录（RMX5010 / Android 16 / SDK 36 / 1264×2780；1.0.32 正式包 + 1.0.33-dev 包；缺陷 1 复核通过） | 历史 | 2026-11-10 |

## 六、docs/icon-design/（图标设计素材档案）

| 文档 | 用途 | 状态 | 核对日期 |
|---|---|---|---|
| [`icon-design/README.md`](icon-design/README.md) | 图标设计素材档案总览（109 个 tracked 文件，素材为主、非文档实体） | 现行 | 2026-11-10 |
| [`icon-design/archive/README.md`](icon-design/archive/README.md) | 历史归档索引（initial / g34r 两批） | 历史 | 2026-11-10 |
| [`icon-design/archive/g34r/icon-export/README.md`](icon-design/archive/g34r/icon-export/README.md) | G34R 导出包说明（历史评审板 408 KB 超 §18.2 阈值，按 HISTORICAL 保留原样） | 历史 | 2026-11-10 |
| [`icon-design/scripts/README.md`](icon-design/scripts/README.md) | 图标导出脚本说明 | 现行 | 2026-11-10 |

## 七、docs/deploy-download-page/（自托管下载页基座）

| 文档 | 用途 | 状态 | 核对日期 |
|---|---|---|---|
| [`deploy-download-page/README.md`](deploy-download-page/README.md) | 自托管下载页工程基座说明（README + `index.html` + `latest.json.example`） | 现行 | 2026-11-10 |

## 八、docs/ 内的资产、配置与本地层

| 项 | 用途 | 状态 | 核对日期 |
|---|---|---|---|
| `assets/` | README 展示图（`readme-*`，§8.1 指定位，无 Markdown） | 现行 | 2026-11-10 |
| `ci/branch-protection.json` | 分支保护口径单一来源（非 Markdown，不在文档改动范围） | 现行 | 2026-11-10 |
| `agents/`（2 篇）、`project/`（1 篇） | **仅本地、不入库**：`docs/agents/`（GOTCHAS / CODE_STYLE）与 `docs/project/`（ADR-0001 本地原件） | 本地 | 2026-11-10 |
| `screenshots/` | **未跟踪 WIP 截图暂存**（`mobile-ux-2026-10/` 4 张 + `.gitkeep`，§8.1.3） | WIP | 2026-11-10 |
| `_devarchive/` | 空目录（已 gitignore） | — | 2026-11-10 |
| `icon-design/export-snapshot-index.md` | **未跟踪 WIP**：图标导出快照索引（本轮只登记，不修改） | WIP | 2026-11-10 |

## 九、docs/ 之外的同体系文档（目录级）

| 区域 | 用途 | 状态 | 核对日期 |
|---|---|---|---|
| [`.github/`](../.github/) | CI 工作流与治理配置说明（2 篇；`workflows/*` 不在文档改动范围） | 现行 | 2026-11-10 |
| [`.githooks/README.md`](../.githooks/README.md) | 本地 Git 钩子说明（pre-commit 四级回退、pre-push gitleaks） | 现行 | 2026-11-10 |
| [`_archive/`](../_archive/) | 归档 6 篇（`执行记录/2026-11/` 与 `INDEX.md`） | 历史 | 2026-11-10 |
| [`server/`](../server/) | Python 同步服务说明（1 篇），改动需先跑 `cd server && pytest tests -q` | 现行 | 2026-11-10 |
| [`core/`](../core/)、[`feature/`](../feature/) | 模块级说明各 1 篇 | 现行 | 2026-11-10 |
| `local/`（2 篇）、`.workbuddy-ai/`（6 篇） | 本地维护层与工具资产，**未随仓库分发** | 本地 | 2026-11-10 |

## 十、索引维护规则

1. **新增文档**：归入 `docs/` 对应目录并在本文件相应节补一行；`状态` 按上文口径判定；同时更新 [`README.md`](README.md) 的分组表。
2. **取代 / 归档**：被取代的文档在行内把 `状态` 改为 `废弃` 并写后继路径；需移走的入 `_archive/docs-archive-<YYYYMMDD>/`（保留原相对路径），**不删除**；本仓既有 `_archive/`（含 `INDEX.md`）沿用。
3. **本地层与分发边界**：`AGENTS.md` 家族、`docs/agents/`、`docs/project/`、`local/`、`.workbuddy-ai/` 只做目录级说明，引用时必须标注「仅本地保留、不入库」。
4. **未跟踪 WIP**：`docs/icon-design/export-snapshot-index.md`、`docs/screenshots/**` 当前未入库，`状态` 列标 `WIP`，整理时不得修改其内容；入库后再转为常规条目。
5. **核对日期**：整仓复核时统一刷新。
