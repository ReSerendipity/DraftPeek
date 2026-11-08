# 2026-09 Agent 工作记录归档（DraftPeek）

> 固化说明：本文件是 `.workbuddy/`（agent 工具目录，git-ignored）中 2026-09 记忆与报告的入库归档。原始文件在清理时删除，下文按主题合并、逐条保留可执行事实；寒暄、重复叙述与纯过程流水已删。事实以 `（09-15）` 形式标注来源日期（= 2026-09-15）。
> 与现状冲突的结论不静默丢弃：保留原结论并标「已过时（YYYY-MM-DD）」，其后写明 2026-11-04 复核到的现状。

## 来源

| 来源路径 | 日期 | 原始行数 | 本文保留了什么 |
|---|---|---|---|
| `.workbuddy/memory/2026-09-15.md` | 2026-09-15 | 120 | 资源合并冲突根因与 8 条排除集、全局开关缺陷、分支保护/合并判据、spotless 尾逗号自伤链、沙箱 git 与 gh 坑点 |
| `.workbuddy/memory/2026-09-16.md` | 2026-09-16 | 19 | aapt2 字符串转义三规则与验证命令；沙箱 ref 写入绕法（已过时） |
| `.workbuddy/memory/automations/99d763fc-5277-4b19-9cf7-4354d091e2d0/memory.md` | 2026-09-15 | 20 | 自动化核查的可复用判据（并入 §4、§5，与报告重叠部分去重） |
| `.workbuddy/reports/pr-ci-verdict-20260915.md` | 2026-09-15 | 255 | PR #61/#62 终态结论、门禁基线、失败日志签名、证据索引口径 |

## 1. 构建阻塞：JUnit 5 许可文件撞车（`DuplicateRelativeFileException`）

- 失败任务 `:feature:browser:mergeDebugAndroidTestJavaResource`；异常 `com.android.builder.merge.DuplicateRelativeFileException: 6 files found with path 'META-INF/LICENSE.md'`（09-15）。
- 来源：JUnit 5 六个 artifact（`junit-jupiter{,-api,-engine,-params}`、`junit-platform-{commons,engine}`）各自携带同名 `META-INF/LICENSE.md`，版本 **5.8.2**（传递依赖引入，非 `gradle/libs.versions.toml` 声明的 5.11.3）（09-15）。
- 为何漏网：`app` 自带 packaging 块但**不含** `LICENSE.md`；`core/*` 曾单独修过；`feature/*` 完全无配置（09-15）。
- 为何改约定插件无效：`build-logic/convention/.../AndroidLibraryConventionPlugin.kt` 虽存在，但**没有任何模块使用**——各模块仍写 `id("com.android.library")`（09-15；2026-11-04 复核 `core/sync/build.gradle.kts` 仍为 `id("com.android.library")`）。
- 收敛结论：在根 `build.gradle.kts` 用 `subprojects { plugins.withType<com.android.build.gradle.LibraryPlugin> { ... packaging { resources { excludes += setOf(...) } } } }` 统一注入，所有 library 模块自动继承，避免逐模块复制回归（09-15）。
- 权威排除集 **8 条**（09-15）：`META-INF/LICENSE.md`、`META-INF/LICENSE-notice.md`、`META-INF/{AL2.0,LGPL2.1}`、`META-INF/versions/9/OSGI-INF/MANIFEST.MF`、`META-INF/OSGI-INF/MANIFEST.MF`、`META-INF/*.SF`、`META-INF/*.DSA`、`META-INF/*.RSA`。
- 必须一次补齐的理由：`MergeJavaRes` 是 **fail-fast** 的，只补 `LICENSE.md` 会在下一轮才暴露 OSGI 清单冲突，而 `feature/browser` 恰好 `implementation(libs.jsch)`（09-15）。
- 写法约束：`setOf(...)` **末元素不得带尾逗号**（spotless/ktlint 会判违规），参照 `core/testing/build.gradle.kts:41`（09-15）。
- 现状（2026-11-04）：根 `build.gradle.kts:204-223` 已含 8 条 + 成因注释（jsch / 5.8.2 / fail-fast 均写在注释里）。此前的逐模块副本仍在 `core/testing/build.gradle.kts:33-42`、`app/build.gradle.kts:227-238`。

## 2. 运行时缺陷：`trackRecomposition` 未受全局开关约束

- 现象：`RecompositionCounter` 只检查 CompositionLocal 本地开关，`counter.increment()` 不受 `isGloballyEnabled` 约束 ⇒ 全局关闭时本地计数仍自增，第二次组合起返回非 0（09-15）。
- 判定：测试 `globalSwitch_disabledSuppressesEvenLocallyEnabledTracking` 断言 `[0, 0]` 是**对的**，实现是错的（09-15）。
- 非 flake 证据：API 26 与 34 失败签名完全一致（18/20、同一断言）；另在 PR #62 同一 job 内重试两轮结果相同（09-15）。
- 修法：在 `remember` 之前短路——`if (!enabled || !RecompositionTracker.isGloballyEnabled) return 0`（09-15）。
- 语义铁律：全局开关关闭时，任何路径（含本地开关打开）都**不得**自增、返回值必须为 0（09-15）。
- 现状（2026-11-04）：`core/ui/src/main/java/com/draftpeek/core/ui/performance/RecompositionCounter.kt:199-207` 已是该短路写法。

## 3. Android 字符串资源转义（aapt2 硬失败）

- 失败：`./gradlew assemble release` 在 `:app:mergeDevBetaResources` / `:app:packageDevBetaResources` 报 `values-en/strings.xml:102:50 在实体引用中, 实体名称必须紧跟在 '&' 后面`（09-16）。
- 两处均在 `app/src/main/res/values-en/strings.xml`（main 源集，影响所有 variant）：第 102 行 `Agree & Start` 的裸 `&` → 必须写 `&amp;`；第 100 行 `others' ` 裸撇号被 aapt2 判 `does not contain a valid string resource`，写成 `\'`（单反斜杠 + 撇号）通过（09-16）。
- 规则（09-16）：`&` 一律 `&amp;`；撇号转义为 `\'` 或用双引号包裹整串；**aapt2 不支持 `\\` 作转义**。
- 验证命令：`./gradlew :app:mergeDevBetaResources --rerun-tasks` → BUILD SUCCESSFUL；全量扫描 6 个 `strings.xml` 未见其它未转义 `&` 或非法反斜杠（09-16）。
- 误报提示：同类扫描脚本对 `&amp;` 的「命中」是 ElementTree 解码后的误报，不能当违约（09-16）。
- 现状（2026-11-04）：第 100 行 `others\'`、第 102 行 `Agree &amp; Start` 均在位。

## 4. 门禁基线与「可合并」判据

| 判据 | 结论 | 来源/现状 |
|---|---|---|
| `mergeStateStatus` 语义 | `BLOCKED` = 必需检查红；`UNSTABLE` = 必需项全绿、仅非必需项红（**UNSTABLE 可合并**） | 09-15 |
| 必需检查是否含 Instrumented | **不含**；`Instrumented Tests` 非必需，其红不阻断合并 | 09-15，机制沿用 |
| PR 档 instrumented 档位 | `pull_request` 只输出 API 30 单档（`google_apis` / `x86_64`），API 26/30/34 全矩阵仅 `workflow_dispatch` / `schedule` | 09-15（`android.yml:243-260`）；2026-11-04 复核仍在 `android.yml:269-290` |
| 必需检查清单 | 原为 `["Build & Test", "layout"]`，`enforce_admins=true`，`required_conversation_resolution=true` | **已过时（2026-10-14）**：见下 |
| 必需检查清单（现状） | `docs/ci/branch-protection.json` 登记 **3 项**：`Build & Test`、`layout`、`Lock Integrity (server lock resolvability)`；`required_approving_review_count=1`、`required_conversation_resolution=false`、`enforce_admins=false`；真值以 `gh api repos/ReSerendipity/DraftPeek/branches/main/protection` 为准 | 2026-10-14 `_measured_at`（09-15 的记忆早于 2026-09-28 治理变更） |

- 判据：check-runs 中出现 **未展开**的 `Instrumented Tests (API ${{ matrix.api-level }})` 名字 = 矩阵从未实例化（job skipped），不是「跑了三档」（09-15）。
- 判据：`Select Instrumented Matrix`、`instrumented-tests`、`release-build`、`macrobenchmark` 均 `needs: build-and-test` ⇒ `Build & Test` 红时下游**必然 skipped**；此时日志里搜不到 `DuplicateRelativeFile` 是「没跑」而非「已修」，不得误判修复生效（09-15）。
- 判据：分批 push 的中间 commit **没有任何 workflow run**（`actions/runs?head_sha=` 返回空），中间态无法补测（09-15）。
- DCO 是独立检查，`auto-merge` 为 success 仅代表该 Action 自身执行成功，**不等于已获准合并**（09-15）。
- 两个 PR 的结论（09-15 22:30）：#61 `354d2eea9` **不可合并**——必需 `Build & Test` failure、instrumented 全 skipped、两处修复**零 CI 证据**；#62 `b76e508d9` **可合并**——必需两项全绿，唯一红灯 API 30 经三重旁证属 main `c51f9bd` 既有缺陷。放行顺序修正为「**先合 #62，再让 #61 修 spotless 后 rebase**」。现状（2026-11-04）：两处修复内容均已在 main；PR 关闭时间与合并顺序未核实。

## 5. CI 机制与排障口径

- `GITHUB_TOKEN`（`github-actions[bot]`）触发的事件**不创建新 workflow run**，例外仅 `workflow_dispatch` / `repository_dispatch`；`pull_request` 的 opened/synchronize/reopened 需审批、`closed` 不创建 ⇒ bot（automerge）合并产生的 main push 静默无 CI，main HEAD 上 0 个 check，UI 无绿勾也无红叉（09-15）。
- `workflow_dispatch` 产生的 check **不能满足分支规则集的必需状态检查**，也不进入 PR 检查区（合格事件仅 push / pull_request / pull_request_review / pull_request_target / deployment / deployment_status）⇒ 补跑只提供「可视化 + 事后预警」，真正门禁仍是 PR 上的 `pull_request` CI（09-15）。
- 补救设计（已在 main）：`automerge.yml` 在轮询到 PR MERGED 后自行 `workflow_dispatch` 补跑；`main-verify.yml` 用 `pull_request.closed` + `*/15 * * * *` cron + 手动，diff main HEAD 已有 check runs 与 `docs/ci/branch-protection.json` 的 required contexts，缺则补跑，以 `ci-verify-<sha7>` 锚定 tag 作 ref；tag 回收阈值 6 小时（09-15；2026-11-04 复核 `main-verify.yml` 仍为该设计）。
- **已过时（2026-09-15 当晚）**：「远程残留 5 个 `ci-verify-*` tag 待人工回收」——实测未过期（阈值 6h，最老 `80a9fea` 为 10:52 UTC 未到阈值），且 GC 已由 `main-verify.yml`（#58 起）实现，无需人工删除。现状：本地现存 8 个 `ci-verify-*` tag，远程数量未核实。
- 日志取法（09-15）：`gh api .../actions/jobs/<id>/logs` **必须加 `--allow-escape-sequences`**，否则报 "the response contains terminal escape sequences" 且输出为空。
- 取数顺序（09-15）：只看 `commits/<sha>/check-runs` 会漏掉 skipped 的成因；须 `actions/runs?head_sha=<sha>` 取 run_id → `actions/runs/<id>/jobs` 看 job 级 skipped/failure 与 `needs` 链。

## 6. 构建/提交自伤链：spotless 尾逗号

- `Build & Test` 唯一失败任务 `:spotlessKotlinGradleCheck`；违规点在根 `build.gradle.kts:190`——`setOf(...)` 的最后一项 `"META-INF/*.RSA",` 多一个尾逗号（前 7 行的逗号是元素分隔符、合法；紧贴 `)` 的这个才是 trailing comma）（09-15）。
- 该缺陷由「一次补齐 8 条」的第 3 个 commit 引入：抄了 `core/testing` 的集合却破坏格式（对照 `core/testing/build.gradle.kts:41` 无尾逗号）（09-15）。
- 连带效应：格式红 → `Build & Test` 红 → instrumented 整段 skipped → 修复自身的验证也被埋掉（09-15）。
- 放行动作（09-15）：删尾逗号或 `./gradlew spotlessApply` → **新增 commit 并 push，不要 amend 已 push 的提交**（避免 force push）→ 等 `Build & Test` 转绿后 instrumented 才首次真执行。
- 关联：本地 `spotlessCheck` 清单 ≠ CI 结论的机制见 GOTCHAS #DP-06，本文不再重复。

## 7. 环境 / 工具坑（含已过时标注）

- **已过时（2026-09-15 → 09-16 两版结论互相矛盾）**：沙箱内 git ref 写入结论。09-15 实测「`refs/heads/*`、`refs/tags/*` 可写，`refs/remotes/*` 写入被静默丢弃」；09-16 实测「`git update-ref` / `git commit` / `git reset --mixed HEAD` 走 tempfile+rename 被拦，报 exit 0 但 ref 不落盘，而 `git write-tree` / `git commit-tree` / 手写 loose ref 可落盘」。
  - 当时的绕法（仅备查，勿再照用到正常仓库）：`git commit-tree <tree> -p <baseline> -m <msg>` 造提交 → `git read-tree <baseline>` 复位索引 → 只 `git add` 目标文件 → `git write-tree` 核对 → 手写 `.git/refs/heads/<branch>`；过程中禁止 `git reset --mixed HEAD` / `git branch -f` / `git update-ref`。
  - 现状（2026-11-04）：当前仓库 git 正常（`main` 跟踪 `origin/main`、HEAD 为 `5b71e01`，ref 读写无异常）；上述两条结论互相矛盾且同属当时的 WorkBuddy 沙箱环境，其他机器/会话未核实。
- `git fetch origin <branch>` 带显式 refspec 时**只写 `FETCH_HEAD`**，不更新 remote-tracking ref，objects 下载成功但 `origin/main` 仍陈旧（09-15）——核远程事实一律走 `gh api`。
- 单 fixer 协议 `.github/FIXER.lock`：开工把 `session` 改为自身标识并提交，收工置回 `null`；检测到 `session` 非空且非本会话时禁止 commit（09-15；2026-11-04 复核该文件仍为 `session: null`）。

## 8. 自动化任务规则（CI 终态核查）

- 一次执行 = 只读核查 + 给结论，**不合并 PR、不改工作区**；结论按「可合并 / 不可合并 + 证据」两段式输出（09-15）。
- 判定「能否合并」只看必需检查（清单见 §4），非必需项红只写进旁证，不改结论（09-15）。
- 修复是否生效必须以「对应 job 真跑过」为前提；上游红导致的 skipped 一律记为「零验证」，不得写成「已修复」（09-15）。
- 复用事实清单（09-15）：必需检查只有两项（今为三项）、PR 档只跑 API 30、下游均依赖 `build-and-test`、权威排除集 8 条且末元素无尾逗号、该类固定 7 个 `@Test`、中间 commit 无 run。

## 9. 已由 GOTCHAS / AGENTS / 既有报告覆盖，不再重复

- 本地 `spotlessCheck` 清单与 CI 不同、`lineEndings = PLATFORM_NATIVE` 的行尾机制：GOTCHAS #DP-06；`docs/reports/2026-10-15-失败签名机制与排障路径摘要.md`。
- 分支保护 JSON ↔ live 漂移、`apply_branch_protection.py --apply` 双向口径：GOTCHAS #DP-20。
- 新增模块 `--write-locks` 四族 × 全变体、依赖方同步：GOTCHAS #DP-17。
- 多会话并发吞提交 / 工作树被外部删除：GOTCHAS #DP-04、#DP-15。
- PR 档只跑 API 30 的设计与其落地：`docs/reports/测试体系评估报告_v1.0.30.md`（P2-7）+ `android.yml` 内联注释。
- `RecompositionCounter` 的 7 个 androidTest 用例规模与双开关零开销设计：`docs/reports/DraftPeek_前端工程体系评估报告_v1.0.30.md`（P1-3）。
- 15 个 workflow、模块结构、CI 命令与版本工具链：`AGENTS.md` §0/§5、`docs/README.md`。

## 10. 未搬入的内容与未核实项

- 已剔除（纯过程流水或已失效状态）：本地 worktree 落后 20 提交、`backups/` 备份路径与 WIP 文件清单、一次性自动化任务的创建时间、PR 逐项 check 表与日志字节数、逐个 commit SHA 流转。
- 已过时且无替代现状：09-16 的孤儿分支 `compliance/legal-remediation-20260916` / 游离提交 `fc03937` / 终态 `54db438`，属一次性事故现场，未在现行分支留存。
- 未核实：PR #61/#62 的关闭与合并时间；远程 `ci-verify-*` tag 实际数量与回收执行情况；§7 沙箱结论在其他环境是否复现。
