# DraftPeek 接管者指引（HANDOVER GUIDE）

> 建立于 2026-10-15（任务 DraftPeek-v29-remediate，落地《质量工程效率 Flash 报告》P1-1：bus factor=1 → 补「接管者指引」一页）。
> 给新协作者/接管者的最小可执行起点。完整规范见 `docs/CODING_STANDARDS.md`；架构决策与评审见 `docs/adr/`、`docs/reports/2026-10-15-架构评审台账.md`；排障见 `docs/reports/2026-10-15-失败签名机制与排障路径摘要.md`（摘要，完整台账在本地 FIX_LOG/GOTCHAS）。

## 1. 项目是什么

- Android 原生编辑器 App（Jetpack Compose，minSdk 26 / targetSdk 36），app + 7 core + 5 feature 分层（`settings.gradle.kts`）；`server/` 为已裁撤路线的 Python 原型（技术参考，勿投入生产化）。
- 依赖锁定 `LockMode.STRICT`（15 模块 lockfile 入库）；版本单一来源 `gradle/libs.versions.toml`。
- 分发边界：`docs/` 下按白名单分发；`FIX_LOG.md`、`docs/agents/GOTCHAS.md`、根/模块级 `AGENTS.md` 仅本地保留、不入库。

## 2. 门禁与验证（改动必读）

- 必需检查三项（`docs/ci/branch-protection.json` 为单一来源）：**Build & Test**、**layout**（根布局 + domain←data 边界 + 模块方向 R1-R7 + spec-refs + 文件行数上限）、**Lock Integrity**。改动涉及任一，必须本地过对应脚本。
- 覆盖率门禁：`coverage-baseline.json` 单一真相，棘轮只升不降；`scripts/check_coverage_gate.py --ratchet` 报告缺失即 FAIL。改模块必更新基线（新增模块还要 codecov.yml）。
- Server 改动（`server/**`）：提交前必须 `cd server && pytest tests -q` 全绿（`docs/README.md`「Server 改动验证门禁」）。
- 本地 Gradle 规避：`--no-daemon --max-workers=1`（Robolectric 锁假红）；只跑 CI 变体 `:app:testDevDebugUnitTest`；库模块任务名 `testDebugUnitTest`；验证单类加 `--tests "*XxxTest*"`（防 Paparazzi 录制 golden）。
- 本地 spotlessCheck 结果 ≠ CI（Windows PLATFORM_NATIVE 行尾），以 CI 为准。

## 3. 提交纪律（铁律）

1. **显式覆盖作者身份**（防环境注入 zwork 之类）：
   `$env:GIT_AUTHOR_NAME='ReSerendipity'; $env:GIT_AUTHOR_EMAIL='259081123+ReSerendipity@users.noreply.github.com'; $env:GIT_COMMITTER_NAME='ReSerendipity'; $env:GIT_COMMITTER_EMAIL='259081123+ReSerendipity@users.noreply.github.com'`
2. **显式路径** `git add -- <文件>`，严禁 `git add -A/.`；提交后 `git show --stat` 核对文件清单。
3. 禁止 push、force、reset --hard、checkout 丢弃改动、stash、删除他人文件；禁止切分支/merge/rebase/pull（本仓库就地工作约定）。
4. 每个内聚批次打 annotated tag（`git tag -a <名> -m "<说明>"`）。
5. 提交后自检：`git log -1 --pretty="author=%an <%ae> committer=%cn <%ce>"`。

## 4. 关键入口速查

| 需要 | 去这里 |
|---|---|
| 架构决策 | `docs/adr/`（0002 已入库；0001 正文待补录） |
| 评审/决议/回流 | `docs/reports/2026-10-15-架构评审台账.md`（LEDGER-001~007） |
| 覆盖率爬坡 | `docs/reports/2026-10-15-覆盖率爬坡计划.md`（feature/editor+browser 90 天） |
| 排障摘要 | `docs/reports/2026-10-15-失败签名机制与排障路径摘要.md` |
| 完整排障台账（本地） | 根 `FIX_LOG.md`（失败签名 sig 检索）、`docs/agents/GOTCHAS.md`（#DP-01~20） |
| 版本/灰度/回滚 | `docs/VERSIONING.md` |
| 发布检查 | `docs/RELEASE_CHECKLIST.md` |
