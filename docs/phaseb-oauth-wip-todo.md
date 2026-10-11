# DraftPeek 阶段 B · OAuth 注册临时降级 TODO

> 状态：**本批止步于占位降级（T0 骨架 + 「开发中」占位）；T2–T5 本批不做，留待后续新对话推进。**
> 本文件记录「跳过 OAuth 注册、界面点击提示开发中」的临时降级决策、范围裁定与恢复清单。

## 决策背景
- 来源：用户于 2026-10-11 明确要求「注册先暂时跳过，相应界面点击功能提示正在开发中」。
- 对应任务书：`draftpeek-phaseb-taskbook-v1.md` 的 T1–T5（OAuth 注册与同步闭环）。
- 影响：阶段 B 的「账号 / 同步」能力进入**临时占位**状态，不实现真实 OAuth 与同步传输。

## 范围裁定（2026-10-11 更正）
- 用户在 2026-10-11 **更正选择：本批不做 T2–T5**（此前曾短暂选「搭 T2–T5 UI 骨架」，已作废）。T2–T5 留待后续新对话推进。
- 裁定依据（均来自任务书硬约束，非主观）：
  - 任务书 §9：`T0 → T1 → T2 → T3 → T4 → T5` **严格串行**，每批 CI 绿是下一批前提。
  - 任务书 §1：OAuth Client ID 到位前，「T1 起全部无法功能化」。
  - 任务书 §2.5：**不做死 UI**——点击目标未实现的入口不给可点态；无后端处不造假数据。
- 推论：在 OAuth（T1）底座缺位下强行做 T2–T5，必然产生死 UI / 不可验证代码，违背任务书约束。故本批只保留「占位降级」，不预支 T2–T5。

## 当前已落地的临时降级（本批唯一交付）
- 登录按钮（`ProfileScreen` 身份区 `SignedOut` 态）：点击弹出 `account_sign_in_coming_soon` →
  「账号功能开发中，敬请期待」（既有占位，本次未改动）。
- 同步状态行按钮（`SyncStatusRow` 的 **立即同步 / 重试 / 取消**）：点击弹出 `account_sync_coming_soon`
  → 「同步功能开发中，敬请期待」（本次新增）。
  - 不再调用 `AccountViewModel.retrySync()/cancelSync()`，避免进入「无真实引擎、Syncing 永不动」的误导态。
- 新增 6 语词条 `account_sync_coming_soon`（values / values-zh / values-zh-rTW / values-en / values-ja / values-ko）。
- 提交：`c51ddca`（代码 + 资源）、`b6a9010`（本 TODO 强制纳入版本控制）；T0 提交 `d2b407c`。
- 全部相关 CI 已绿（T0: `38106413232`；本批: `38108563070` Security Review、`38108563028` CodeQL、`38108563289` 均 success）。

## 待办（后续新对话推进，严格按任务书串行）
- [ ] **T1 注册 OAuth Client（GitHub / Gitee）**：阻塞项——需产品/运维交付 Client ID（任务书 §1c）。
- [ ] **T2 授权链 UI**（B1–B4，规格 §2）：真实登录 / 授权流程替换「开发中」占位（登录按钮、`account_sign_in_coming_soon` → 真实登录屏；B2 范围说明、B3 跳转/回跳、B4 选仓/建仓）。
- [ ] **T3 首装 + 同步闭环**（B5 + 真实数据源）：首装三选一 + 变更触发常态同步。
- [ ] **T4 B6/B7/B8**：`SyncStatusMapping.from(state)` 接真实状态流；B7 冲突对话框（409 默认「保留双方」）、B8 损坏专屏接线。
- [ ] **T5 B9 + 清理批**：撤销授权 / 换仓（重走 B4→B5）；删除 debug 模拟入口（`simulateNextState`）、deprecated 词条与占位 `account_sign_in_coming_soon` / `account_sync_coming_soon` 及 `SyncGraph.NotReadySyncTransport` 守卫。

## 本次改动文件
- `feature/stats/src/main/java/com/draftpeek/feature/stats/ui/ProfileScreen.kt`：同步按钮回调改为弹出「开发中」Toast。
- `feature/stats/src/main/res/values*/strings.xml`：新增 `account_sync_coming_soon` ×6 语。
- `docs/phaseb-oauth-wip-todo.md`：本文件（阶段 B 临时降级决策与范围裁定）。

## 参考
- 任务书：`draftpeek-phaseb-taskbook-v1.md` §6②–⑤、§7、§9；§2.5（不做死 UI）、§1（OAuth 硬前置）。
- T0 回执：`draftpeek-phaseb-t0-receipt.md`（协调器装配骨架，commit `d2b407c`，CI 绿）。
