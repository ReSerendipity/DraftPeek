# DraftPeek 发布版本管理评估报告

> 评估对象：`C:/Users/Doro/DraftPeek`（Android 应用，GitHub Actions → GitHub Release draft）
> 评估日期：2026-09-04 ｜ 项目版本：v1.0.30 (10030)
> 方法：先逐条核实评估提示词「事实锚点表」，再据真实代码/配置给出判定；不确定项标「未核实」并附核实命令（严守证据绑定铁律，未虚构任何门禁或渠道）。

---

## 0. 事实锚点校正（重要：评估提示词部分假设与仓库实际不符）

评估提示词 §0 的「事实锚点表」多数准确，但以下 4 项经核实**不成立或需修正**。这些修正本身即是本次评估的关键发现。

| # | 提示词假设 | 仓库实际（已核实） | 结论 |
|---|---|---|---|
| A | `SECURITY.md` 支持版本表写「1.0.x」 | **不存在 `SECURITY.md`**；仅有 `SECURITY_AUDIT_DraftPeek.md`（审计报告，非支持策略） | 反模式 #6 前提错误；真实缺口是「无安全支持策略文档」 |
| B | `THIRD_PARTY_NOTICES.md` 声明 proot 为 GPL-2.0 **且随包分发** → 每次发版须复核分发义务 | 该文件第 47–53 行明确：**proot 二进制与 rootfs 当前未随 APK 分发**（此前"已随包分发"表述已更正为与仓库实际不符） | 分发义务当前为**空操作**；合规风险取决于"未来是否捆绑" |
| C | 依赖版本统一走 `libs.versions.toml`，铁律禁止 `build.gradle` 硬编码 | `benchmark/build.gradle.kts:66-76`、`feature/editor/build.gradle.kts:75-76` 存在**硬编码版本**（如 `rosemoe:editor:0.24.6`、`androidx.benchmark:benchmark-common:1.2.0`、`hilt-android-testing:2.58`） | 铁律存在实际违反，版本集中化未彻底 |
| D | Tag 规则（`v*` 触发 `release.yml`）已落地运行 | **本地 `git tag -l` = 0；`git ls-remote --tags`（默认远程） = 0** → `release.yml` 从未被 `v*` tag 触发 | 自动化发版链路处于**休眠/未验证**状态，1.0.30 的真实发布路径未经验证 |

另：提示词 §2.6 / §3-3 引用的「`AGENTS.md §9` 版本号同步清单（铁律）」**在 `AGENTS.md` 中检索不到**（grep `版本号同步`/`§9`/`bumpVersion`/`Tag` 均无匹配）。当前 `AGENTS.md` 只有"自进化自检清单"，无独立的发版前版本同步强制清单。

---

## 1. 版本一致性核对表（提示词 §4-①）

| 检查项 | 实际值 | 状态 |
|---|---|---|
| `gradle.properties` `draftpeek.version.*` | major=1 / minor=0 / patch=30 → versionName `1.0.30` | ✅ |
| `app/build.gradle.kts:36` versionCode | `vMajor*10000+vMinor*100+vPatch` = **10030** | ✅ 与版本号一致 |
| `CHANGELOG.md` 最新发布条目 | `## [1.0.30] - 2026-08-10`（第 23 行；其上为 `[Unreleased]` 占位） | ✅ 存在 |
| Git Tag | **无 tag**（本地/远程均 0） | ❌ **tag 这一"腿"完全缺失** |
| `README.md` 版本声明 | 写 AGP 8.8.0 / Gradle 8.13 | ❌ **漂移**：实际 `libs.versions.toml` agp=`8.10.1`、`gradle-wrapper` = `8.14.3` |
| 版本变更工具 | `./gradlew bumpVersion -Pbump=...`（root `build.gradle.kts:118`）+ `./gradlew generateChangelog -Pversion=...` | ✅ 工具齐备 |

**判定**：三处版本中"gradle.properties ↔ CHANGELOG"一致，但 **tag 从未创建**，且 README 文档版本已漂移。按提示词口径"三处不一致即不达标"——此处属于"tag 缺失 + 文档漂移"，**版本管理未达完整闭环**。

---

## 2. 发布流水线逐工序强度审计（`.github/workflows/release.yml`）

> 重要前提：`release.yml` 仅在推送 `v*` tag 时触发，而**仓库无任何 tag**，故以下工序**全部为"设计态"，未经真实执行验证**（休眠）。

| 工序 | 实现 | 强度 | 备注 |
|---|---|---|---|
| 解码 keystore（Secrets → `release.jks`） | `base64 -d > release.jks`（**repo 根**） | ⚠️ 潜在缺陷 | 见 §2.3 路径错配 |
| Set up JDK 17（temurin） | `setup-java@v4` | ✅ 硬（环境） | 与本地 JDK17 一致 |
| `./gradlew test` | 单元测试 | ✅ 硬阻断 | 失败即终止 |
| `./gradlew lint` | Lint | ✅ 硬阻断 | 失败即终止 |
| **校验 CHANGELOG 版本 == tag** | `grep -q "## [$VERSION]"` → 否则 `exit 1` | ✅ **硬阻断** | 真实门禁，但休眠未跑 |
| `assembleRelease` | APK | ✅ 硬阻断 | 依赖 keystore 路径正确 |
| `bundleRelease` | AAB | ✅ 硬阻断 | — |
| `apksigner verify --verbose --print-certs` | 签名校验 | ✅ 硬阻断（签名无效则失败） | 无 `--min-sdk` 阈值断言；v1 对 minSdk26 冗余但无害 |
| **Macrobenchmark**（模拟器） | `:benchmark:connectedBenchmarkAndroidTest` | ⚠️ **非性能门禁** | 无阈值断言，仅"测试崩则败"；超限**不阻断** |
| 生成 SHA256 + `.sha256` | `sha256sum` | ✅ 产出 | 已生成并上传 |
| 创建 **draft** Release | `action-gh-release@v2` `draft:true` | — | 人工点发布；**无强制发布前清单** |

**android.yml 补充审计**：
- `build-and-test` job：spotless + lint + assembleDebug + **unit test（546 用例）** + **JaCoCo 覆盖率 60% 硬门禁**（`android.yml:157-185`）。
- `instrumented-tests` job：`if: github.event_name == 'workflow_dispatch'` → **仅在手动 dispatch 时运行**，push/PR/tag 均不跑。→ 发版路径**无真机/UI/DAO 仪器测试**。
- `release-build` job：keytool 生成 `app/ci-release.jks` 测试密钥构建 release APK（仅验证流程，非发布）。

---

## 3. 六维度评分（0–5，附证据）

| 维度 | 评分 | 证据 / 理由 |
|---|---|---|
| ① 版本管理 | **3/5** | 单源+工具齐备；但 tag 缺失、README 漂移、AGENTS.md 无版本同步强制清单 |
| ② 构建可复现 | **3/5** | `dependencyLocking{lockAllConfigurations()}` 已配（root `build.gradle.kts:166`）但**非 STRICT**且 CI 不传 `--lock-mode`；硬编码版本存在；JDK17 一致 |
| ③ 签名与密钥治理 | **4/5** | 凭据不外置硬编码、v1+v2+v3、CI 走 Secrets、测试密钥隔离；扣分于**路径错配潜在缺陷** + 无 Secrets 轮换 SOP + 无 SECURITY.md |
| ④ 发布门禁强度 | **3/5** | unit/lint/CHANGELOG 硬门禁齐备；但**仪器测试缺位**、macrobenchmark 非阈值门禁、SHA256 下载侧仅手工复核，且**全链路休眠未验证** |
| ⑤ 分发与灰度 | **1/5** | 仅 GitHub Release draft；无 Play/蒲公英/静态下载页；无分阶段放量；**无 kill-switch / 热修** → 非开发者用户获取困难，属 P0 商业风险 |
| ⑥ 回滚与热修 | **2/5** | `DATABASE_MIGRATION_STRATEGY.md` 规范完善（禁破坏性升级、降级允许 destructive、迁移测试）；但**无 down-migration 文件**、回滚文档引用"Play Console 暂停"（不适用，无 Play）、无 kill-switch/热修 |

**综合**：工程骨架扎实，但**发布/分发层不成熟且基本未经验证**。

---

## 4. 反模式判定（提示词 §3，逐条）

| # | 反模式 | 判定 | 依据 |
|---|---|---|---|
| 1 | 三处人工同步无门禁→漂移 | **部分成立** | CHANGELOG-verify 硬门禁存在但休眠；tag 从未建 → 三处同步实际未完成 |
| 2 | 发版不经仪器测试 | **成立** | `instrumented-tests` 仅 `workflow_dispatch`（`android.yml:204`） |
| 3 | draft 发布缺强制检查清单 | **成立** | `AGENTS.md` 无发版前清单；release 仅 `draft:true` 人工发布 |
| 4 | 无灰度/无 kill-switch/无热修 | **成立** | 仅 dev/staging/production + beta；无 10→50→100 机制；无 feature flag |
| 5 | 临时测试密钥与正式密钥同流水线污染 | **部分成立（设计有隔离，但有 bug）** | `app/ci-release.jks` 与 Secrets `release.jks` 路径分离；但 release.yml 把正式密钥解码到**根** `release.jks`，而 `signing.gradle` 按 **app/** 相对解析 → 正式发版 keystore 解析会失败（见 §2.3） |
| 6 | 支持版本表过期（SECURITY.md 1.0.x） | **前提错误** | 无 `SECURITY.md`；真实问题是**无安全支持策略** |
| 7 | 误推事故后仅靠文档约束 | **部分缓解但未根治** | pre-push hook **已安装**（`.git/hooks/pre-push`，2026-09-03）并跑 `precheck.ps1`；但 hook **不校验远程/分支**，无法拦截 `git push origin main` 红线（仍依赖 GitHub 分支保护【未核实】+ 人自觉） |

---

## 5. 整改清单（P0 / P1 / P2）

### P0（商业/交付阻断）
| 项 | 文件/命令 |
|---|---|
| **建立分发渠道**：当前产物仅落 GitHub Release draft，非开发者用户无法便捷获取。决策渠道（F-Droid / 蒲公英 / 自托管下载页）+ 实现 in-app 更新检查 | 需产品决策；工程侧可加 `docs/deploy-download-page` 与更新检测模块 |
| **验证并修复发版链路**（因 0 tag，整条 `release.yml` 未跑过）：先修 keystore 路径错配，再打一个测试 tag 跑通全流程 | 见 P1-1；测试 tag：`git tag v0.0.0-dryrun && git push origin v0.0.0-dryrun`（仓库已全量公开，2026-09 修订：测试 tag 直接推 origin 即可） |

### P1（应在下个发布前完成）
| 项 | 文件/命令 |
|---|---|
| **1. 修复 keystore 路径错配**：`release.yml` 解码改为 `mkdir -p app && echo "$RELEASE_STORE_FILE_BASE64" \| base64 -d > app/release.jks`（与 `signing.gradle` 的 app/ 相对解析一致，对齐 `android.yml` 的 `app/ci-release.jks` 写法） | `.github/workflows/release.yml:55-61` |
| **2. 补版本一致性 CI 门禁**：在 `docs-consistency.yml` 或新增 job 中校验 `gradle.properties` 版本 == CHANGELOG 最新发布条目（tag 存在时再比 tag）。当前 `docs-consistency.yml` 仅跑 `check_spec_refs.py`，**不覆盖版本号** | `.github/workflows/docs-consistency.yml`；新增 step 跑提示词 §4-① 三处比对 |
| **3. 消除硬编码依赖版本**：将 `benchmark/*`、`feature/editor` 的硬编码坐标迁入 `gradle/libs.versions.toml` | `benchmark/build.gradle.kts:66-76`、`feature/editor/build.gradle.kts:75-76` |
| **4. 依赖锁定严格化**：`dependencyLocking` 设 `lockMode.set(LockMode.STRICT)`，CI 构建传 `--lock-mode=strict`，防止新依赖静默溜入 | root `build.gradle.kts:166`；CI step 加参数 |
| **5. 校正 README 版本声明**：AGP 8.8.0→8.10.1、Gradle 8.13→8.14.3 | `README.md:28-32` |
| **6. 建立安全支持策略文档** `SECURITY.md`（支持版本表 + 接收漏洞渠道），替代"无策略"现状 | 新建 `SECURITY.md` |
| **7. 补发版前强制检查清单**：在 `AGENTS.md` 增 §发版清单（三处版本一致 + CHANGELOG + 签名 + 分发链接），或做成 `precheck.ps1` 的 `--release` 模式 | `AGENTS.md`、`precheck.ps1` |
| **8. pre-push hook 强化红线**：在 hook 中拦截 `git push origin main` / `origin main:public`（当前 hook 仅跑代码预检，不校验远程/分支） | `.git/hooks/pre-push`；并确认公开仓库已设 branch protection【未核实】 |

### P2（工程卫生/可选项）
| 项 | 文件/命令 |
|---|---|
| 关闭 v1 签名（minSdk=26 时 v2+v3 已足够；v1 冗余） | `app/signing.gradle:41` 设 `enableV1Signing=false`（需回归验证） |
| macrobenchmark 增加性能阈值断言（`BaselineProfile`/阈值比对），使回归可阻断 | `benchmark/` 模块 |
| 增加 down-migration 能力或"补偿迁移"规范，应对无 kill-switch 下的坏 schema 修复 | `DATABASE_MIGRATION_STRATEGY.md` §5 改写（去掉 Play Console 依赖，改"撤 GitHub Release + 发 hotfix"） |
| 引入 feature flag / 远程配置作为 kill-switch 轻量替代 | 新增远程配置层 |

---

## 6. 必答三问

### Q1：是否值得引入 release-please 或等价自动化，消灭三处人工同步？迁移代价？
**结论：值得，但分步推进。** 
- 当前已具备一半基础：`generate_changelog.py` 已解析 Conventional Commits，`bumpVersion` 已存在，CHANGELOG-verify 硬门禁已设计。
- release-please 价值：自动从 commit 生成 CHANGELOG + 自动 bump 版本 + 自动建 tag/PR，彻底消除"三处人工同步"的人因漂移。
- **迁移代价（中，约 1–2 天配置 + 团队约定 adoption）**：① 全仓统一 Conventional Commits（已有脚本基础）；② 配 `release-please` action，决策 bump 策略（可取代 `bumpVersion`/`generateChangelog`）。③ ~~双分支定制难点~~（2026-09 修订：仓库已全量公开、仅单一 `main` 分支，无 public/private 双分支，release-please 可直接按单默认分支配置）。
- **建议路线**：短期先（a）修好并跑通现有 tag 发版链路；（b）加"gradle.properties==CHANGELOG==tag"CI 门禁。中期再用 release-please 替换手工 bump+CHANGELOG，人工保留"发布"决策（2026-09 修订：不再有双分支安全模型约束，单 main + tag 流程即可）。

### Q2：若产物仅落 GitHub Release draft，目标用户如何获取？工程还是商业问题？
**结论：本质是商业/产品决策，但执行是工程缺口，且对当前用户群是 P0 风险。**
- GitHub Release 需 GitHub 账号 + 手动下载 APK + 侧载（开"未知来源"），对**非开发者用户**极不友好；且无自动更新提示，升级靠用户自觉。
- 这是"分发渠道缺失"——属**商业/产品**问题（选哪个渠道），但其**落地是工程任务**（上传到静态托管 / F-Droid / 蒲公英 / 自托管下载页 + in-app 更新检查）。
- 建议：至少补「自托管下载页 + 应用内更新检测」；评估 **F-Droid**（与本项目 LGPL/GPL 合规姿态契合）或 **蒲公英**做 beta 分发。渠道未定前，工程侧不应假设"GitHub Release 即分发完成"。

### Q3：无 kill-switch 前提下，`001_create_knowledge_graph.sql` 这类 schema 变更的回滚预案？
**结论：001 本身是纯增量、幂等（CREATE TABLE IF NOT EXISTS + FTS5 + 触发器），前向安全、重跑无害，无需"回滚"；真正风险在未来破坏性迁移。**
- **已升级用户无法强制降级** → 不能靠"回滚 APK"修复。
- **唯一修复路径**：发新版本，内含**补偿迁移**（如 DROP 新增表/列、或重建到兼容态），或提供 N+1→N+2 修复迁移使数据可用。
- **已损坏数据**：设计正确——升级路径禁 `fallbackToDestructiveMigration`（不会静默删数据）；`fallbackToDestructiveMigrationOnDowngrade` 仅在用户**手动**降级时重建（数据丢失），不适用于多数场景。
- **kill-switch 轻量替代（建议补）**：用 feature flag / 远程配置（如 Firebase Remote Config）关闭知识图谱功能入口，无需发版即可止血——比热修更轻，是当前最现实的止损手段。
- **文档修正**：`DATABASE_MIGRATION_STRATEGY.md` §5 写"在 Google Play Console 暂停发布"——本项目**无 Play 商店**，应改为"撤回 GitHub Release / 下架分发链接 + 发 hotfix 分支"。

---

## 7. 硬约束遵守声明

- 本报告**未虚构任何门禁或分发渠道**；所有"门禁/缺失"判定均附文件+行号证据。
- **未核实项**（需进一步确认，已标注）：
  1. 公开仓库 `origin` 是否配置了 branch protection / ruleset 拦截 `push origin main`（本地无法验证，需查 GitHub 设置）。
  2. 1.0.30 的**历史真实发布方式**（因无 tag，推测为手工发布或本流水线从未跑过；需向用户确认）。
  3. `release.yml` 的 keystore 路径错配导致正式发版失败——为**代码静态推演高置信结论**，最终需一次真实 dry-run tag 确认（2026-09 修订：仓库已全量公开，直接推 origin 测试 tag 即可）。
  4. `macrosbenchmark` 是否在某处定义了未在 `release.yml` 体现的阈值（已确认 `release.yml` 内无阈值断言）。

---

*评估人：星枢（Xīngshū）｜ 依据：AGENTS.md 证据绑定铁律 + 真实代码/配置核查，未作任何猜测性断言。*
