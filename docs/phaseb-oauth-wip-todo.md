# DraftPeek 阶段 B · OAuth 注册临时降级 TODO

> 状态：**临时占位（开发中）**。本文件记录「跳过 OAuth 注册、界面点击提示开发中」的临时降级决策与恢复清单。

## 决策背景
- 来源：用户于 2026-10-11 明确要求「注册先暂时跳过，相应界面点击功能提示正在开发中」。
- 对应任务书：`draftpeek-phaseb-taskbook-v1.md` 的 T1–T5（OAuth 注册与同步闭环）。
- 影响：阶段 B 的「账号 / 同步」能力进入**临时占位**状态，不实现真实 OAuth 与同步传输；真实引擎到位后按「待办」逐项恢复。

## 当前已落地的临时降级
- 登录按钮（`ProfileScreen` 身份区 `SignedOut` 态）：点击弹出 `account_sign_in_coming_soon` →
  「账号功能开发中，敬请期待」（既有占位，本次未改动）。
- 同步状态行按钮（`SyncStatusRow` 的 **立即同步 / 重试 / 取消**）：点击弹出 `account_sync_coming_soon`
  → 「同步功能开发中，敬请期待」（本次新增）。
  - 不再调用 `AccountViewModel.retrySync()/cancelSync()`，避免进入「无真实引擎、Syncing 永不动」的误导态。
- 新增 6 语词条 `account_sync_coming_soon`（values / values-zh / values-zh-rTW / values-en / values-ja / values-ko）。

## 待办（恢复真实能力时逐项清除占位）
- [ ] **T1 注册 OAuth Client（GitHub / Gitee）**：**阻塞项**——需产品/运维交付 Client ID & Secret（任务书 §1）。
- [ ] **T2 认证底座**：将 `SyncGraph.NotReadySyncTransport` 守卫替换为真实 OAuth transport；落地 Token 安全存储。
- [ ] **T3 授权链 UI**：用真实登录 / 授权流程替换「开发中」占位（登录按钮、`account_sign_in_coming_soon` → 真实登录屏；如涉授权 consent 屏一并实现）。
- [ ] **T4 首装同步闭环**：`SyncCoordinator` 接线到 UI——恢复 `retrySync()/cancelSync()` 的真实调用，并接入 `AccountViewModel.syncMachineState`（`SyncCoordinator.state`）。
- [ ] **T5 清理**：删除 debug 模拟入口（`simulateNextState`）、deprecated 词条，以及本临时占位 `account_sign_in_coming_soon` / `account_sync_coming_soon` 与 `NotReadySyncTransport` 守卫。
- [ ] **恢复后验证**：六语文案、Paparazzi 截图、单元 / UI 测试全绿。

## 本次改动文件
- `feature/stats/src/main/java/com/draftpeek/feature/stats/ui/ProfileScreen.kt`：同步按钮回调改为弹出「开发中」Toast。
- `feature/stats/src/main/res/values*/strings.xml`：新增 `account_sync_coming_soon` ×6 语。

## 参考
- 任务书：`draftpeek-phaseb-taskbook-v1.md` §6②–⑤、§7
- T0 回执：`draftpeek-phaseb-t0-receipt.md`（协调器装配骨架，commit `d2b407c`，CI 绿）
