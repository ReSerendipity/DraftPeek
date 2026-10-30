# DraftPeek 发布治理规范（Release Governance）

> **来源与权威**：公共条款见 `ReSerendipity/.github` 的 `REPOSITORY_GUIDELINES.md` §10（发布 / 分发统一规范）。本文件是 DraftPeek **唯一发布总纲**，由原 `docs/VERSIONING.md` 收敛而来；公共条款与 §10 对齐，不自行改写。
>
> 遵循 [Semantic Versioning 2.0.0](https://semver.org/lang/zh-CN/) 与 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

---

## 0. 远端优先（根原则，不可改写）

发布链的构建、打包、扫描、上传、推送**全部在 GitHub Actions 远端完成**；本地只做三件事：改版本位、落 CHANGELOG、`git tag` + `git push`。本地不执行打包 / 镜像构建 / 发布上传。

配套根原则（§10.1）：

- **tag 驱动**：`push: tags: v*` 触发发布链；禁止在本地产出交付物后手工上传充当发布。
- **产物必须可校验**：Release 资产附 `SHA256SUMS.txt` 防传输损坏；不使用 GPG 分离签名（2026-10-18 裁决撤掉）。

## 1. 版本号规范

- 遵循 **SemVer** `MAJOR.MINOR.PATCH`，预发布用 `-alpha.N` / `-beta.N` / `-rc.N`。

| 段 | 含义 | 递增条件 |
|----|------|----------|
| MAJOR | 不兼容的 API / schema 变更 | 数据库 schema 不兼容迁移、核心架构重构 |
| MINOR | 向后兼容的新功能 | 新增 feature 模块、新语言语法支持 |
| PATCH | 向后兼容的 Bug 修复 | 安全补丁、UI 修复、性能优化 |
| PRERELEASE | 预发布标识 | `-alpha.N`、`-beta.N`、`-rc.N` |

- **唯一权威版本位**：`gradle.properties` 的集中版本位 `draftpeek.version.major` / `draftpeek.version.minor` / `draftpeek.version.patch`（当前 `1.0.32`）。
- **必须同步的其它版本位**：`app/build.gradle.kts` 从 `gradle.properties` 读取版本位，**勿手改**；`versionCode` 由 `bumpVersion` task 自动核算，不手写。
- **versionCode 规则（本仓特有，不得丢失）**：
  - 公式：`versionCode = MAJOR * 10000 + MINOR * 100 + PATCH`（示例：`1.0.30` → `10030`）。
  - `versionCode` 必须为整数且**单调递增**；每次发布（含 hotfix）必须递增。
- **自动化方式**：手动 tag（人工）。发版时执行 `./gradlew bumpVersion -Pbump=patch|minor|major`（自动回写 `gradle.properties` 集中版本位并核算 versionCode）。不使用 release-please。
- 预发布标签用途：`-alpha.N` 内部测试（功能不完整）；`-beta.N` 封闭测试（功能完整待验证）；`-rc.N` 发布候选。

## 2. CHANGELOG 管理

遵循 **Keep a Changelog**，段落为 `Added / Changed / Deprecated / Removed / Fixed / Security`。

- 每次 PR 合并至 `main` 时，开发者在 `## [Unreleased]` 下追加变更条目；type 映射：`feat → Added`、`fix → Fixed`、`refactor → Changed`。
- 正式发布时将 `[Unreleased]` 改为 `[VERSION] - YYYY-MM-DD`。

## 3. 发布流程（远端）

```
1. 确认 [Unreleased] 变更条目完整
2. ./gradlew bumpVersion -Pbump=patch|minor|major（回写 gradle.properties 集中版本位；versionCode 自动核算）
3. 将 [Unreleased] 改为 [VERSION] - DATE
4. git commit -m "release(vX.Y.Z): bump version + changelog"
5. 本地打 tag：git tag vX.Y.Z
6. git push origin vX.Y.Z   （触发远端 release.yml 构建）
7. 远端 Actions 构建完成后，在 GitHub Release 页面审核 draft
8. 人工审核确认无误后 published；盯到终态：gh run list 确认绿，红了当场修或 revert 止损
```

### 3.1 灰度发布四阶段（本仓特有）

> **口径提示（2026-10-21）**：下表是**目标态**分级与阈值。当前崩溃观测为「仅本地」、无 ANR/native 采集、`CRASH_REPORT_URL` 默认空串即关闭（见 §3.2）——因此各阶段 crash rate **尚无远端聚合数据可自动判定**，落地前按 §3.2 以本地抽样与人工导出近似评估，不得宣称已具备远端口径。

| 阶段 | 范围 | 通过标准 | 回滚触发 |
|------|------|----------|----------|
| Internal | 开发团队 (5-10 人) | 无 P0/P1 bug，crash rate < 0.1% | 任一 P0 bug |
| Closed Beta | 封闭测试群 (50-100 人) | crash rate < 0.5%，无数据丢失 | crash rate > 1% 或数据丢失 |
| Open Beta | 公开测试 (1000+ 人) | crash rate < 0.3%，用户反馈正面 | crash rate > 0.5% |
| Production | 全量发布 | crash rate < 0.2% | crash rate > 0.5% 或关键功能不可用 |

### 3.2 崩溃观测口径（「仅本地」声明，2026-10-19 随 WIP 批次合入）

> 对应《移动端工程体系 Flash 报告》P1-2：稳定性观测缺 ANR 与 native 崩溃采集、崩溃上报端点 `CRASH_REPORT_URL` 默认空串即关闭。本小节把现状口径成文，防止把「灰度通过标准里的 crash rate」误读为已具备远端口径。

- **当前口径（2026-10-15 成文）：崩溃观测为「仅本地」**——崩溃率按版本维度由客户端本地 `CrashMetrics`（会话/崩溃计数、90 天日记录）统计；`CRASH_REPORT_URL` 默认空串即上报关闭；仓库无上报接收端实现（`server/` 仅 sync/crdt/integrity 三个原型服务，且云同步已裁撤）。**无 ANR/native tombstone 采集，无远端聚合看板**。
- 因此 §3.1 灰度表中各阶段 crash rate 通过/回滚阈值，**当前以本地抽样与人工导出口径近似评估**；在「接入 ANR（`ApplicationExitInfo`）+ native tombstone 采集并明确上报端点归属」落地之前，不得宣称具备远端口径。
- 改进方向（P1-2，未实施）：接入 ANR + native 崩溃采集，明确上报端点归属或维持「仅本地」；落地后本小节同步改写并随版本登记。

## 4. 分发产物与完整性

| 产物 | 构建位置 | 标签 / 命名 | 完整性手段 |
|---|---|---|---|
| Android APK（release 包） | **远端**（GitHub Actions runner） | `vX.Y.Z`（不可变 semver tag；不使用 floating latest） | `SHA256SUMS.txt`（`.apk.sha256`）防传输损坏；APK 本身走 Android 签名（`apksigner verify --verbose`），签名密钥存 GitHub Secrets、不入库 |

- 不使用容器镜像；不使用 Tauri ed25519 更新器签名（本仓为原生 Android，非 Tauri 壳）。
- **不使用 GPG 分离签名**（2026-10-18 裁决撤掉，见 §10.6）。
- **构建可重复性（本仓特有）**：
  - `gradle/libs.versions.toml` 统一管理所有依赖版本（依赖锁定）。
  - Gradle wrapper 锁定 Gradle 版本（`gradle/wrapper/gradle-wrapper.properties`）。
  - CI 固定 `temurin JDK 17`。
  - 每次 Release 构建后生成 SHA256 校验和，GitHub Release 附 `.apk.sha256`；用户可用 `sha256sum -c app-release.apk.sha256` 校验。

## 5. 回滚

- **判定**（满足任一即触发，对应 §3.1 灰度表）：任一 P0 bug；crash rate 越过上表回滚阈值；数据丢失；关键功能不可用。
- **执行**：
  - `git revert <bad-commit>`（保历史、可再 forward）为首选。
  - 应用商店回滚：**当前不适用**——本仓未接入 Google Play，不存在 Play Console 回滚能力（`release.yml` 只产出 GitHub Release 资产，无 Play 发布步骤；分发渠道决策见 `docs/deploy-download-page/README.md`）。若未来接入 Play 分发，本项改为「在 Play Console 回滚到上一版本（需上一版本仍上架）」。
  - 直接下载回退：维护上一版本 APK + SHA256 校验和，在 GitHub Release 中可访问。
    **数据代价（回退前必读）**：上一版 APK 的 schema 版本低于当前库版本，降级打开时 `core/data` 的
    数据库装配走 `fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)` 分支（见 `DataModule`，
    其注释明确该分支就是给「用户从高版本回退」场景用的），书签 / 链接 / 最近文件 / 片段 / 活动统计 /
    安全事件会被整表重建，且**仓内没有设备侧的导出或恢复通道**（`scripts/backup_data.*` 备份的是本地工作树，
    不是应用数据）。磁盘上的用户文件不在 Room 内，不受此路径影响。
    因此执行本项前先确认两件事：① 能否改用上面的 `git revert` + 前滚发版（不动用户数据，首选）；
    ② 若必须回退，在该版 Release 说明中告知用户本地记录会被重建，不要静默推送旧包。
  - Feature Flag Kill-Switch：通过远程配置（或 BuildConfig）禁用问题功能模块。
- **破坏性动作红线**：重指向 / 删除 tag、移动任何远端 ref 属破坏性远端动作，**须仓库所有者单独授权**。`release.yml` 由 `push: tags` 触发，移动已发布 tag 会当场重跑发布链——历史上因此产出过混年代 Release 资产。
- **回滚后验证**：`./gradlew test` 全部通过；`./gradlew lint` 无新增 error；上一版本 APK 的 `apksigner verify --verbose` 通过；SHA256 校验和可复核。走「直接下载回退」时还须补一条数据面复核：首启动后检查书签 / 片段 / 最近文件列表是否符合上面声明的数据代价（整表重建后为空属预期，不得当回归处理；若非空说明降级分支未被触发，需记录实际路径）。

### 5.1 版本号 → 主干等价提交对照与 tag 可达性核查（本仓特有，2026-10-17 建立）

背景：历史身份改写（`.mailmap` 归并 6 个别名身份）会让发布 tag 指向**与主干无祖先关系**的重复提交。此时按 tag 检出或建 hotfix 分支，拿到的是旧提交线的内容，与 `main` 上的等价内容不同源，合并或比对会出现大量无关差异。

截至 2026-10-17 实测：`git tag | wc -l` = 21，其中 `git tag --no-merged HEAD` = 10 不可达——两枚版本 tag（`v1.0.31`、`v1.0.32`）与 8 枚 `ci-verify-*`。**回退或核对一律用「主干等价提交」列，不要用 tag 列。**

| 版本号 | tag 指向提交（主干不可达） | 内容树 | 主干等价提交（同树） | 主干侧提交标题 |
|---|---|---|---|---|
| 1.0.31 | `74e4a8ac3` | `08a367440873840d48b0e1ed8ff9b26fc812b1d8` | `d1f4bf0` | `release(v1.0.31): bump version + changelog (#82)`（draft Release 从未发布，见 CHANGELOG 该版说明） |
| 1.0.32 | `b16b951d1` | `6fc9023cad56155f0ff7170a2d0aa4e7b041da41` | `957e0e3` | `chore(release): 准备 v1.0.32，落版 CHANGELOG 与版本号 (#101)` |

判据用**树（tree）相等**，不用 patch-id 或提交信息：改写只换提交元数据与 SHA，内容树不变。以下三条命令均为只读，可复核本表：

```bash
git merge-base --is-ancestor v1.0.32 HEAD; echo "rc=$?"          # 非 0 = tag 不在主干可达集
git rev-parse 'v1.0.32^{tree}'                                     # 取 tag 所指提交的内容树
git log --format='%H %T' HEAD | grep "$(git rev-parse 'v1.0.32^{tree}')"   # 主干历史中的同树提交
```

规则：

- 需要「回到某一版的代码状态」（排障回归、比对差异、选回退点）时，按本表检出**主干等价提交**；表内没有的版本号，先用上面三条命令求出同树提交并回填本表，再动手。
- 发布新版本时同步登记本表一行（tag 提交 / 树 / 主干等价提交三元组），并顺手跑一次 `git tag --no-merged HEAD` 确认新增 tag 可达；不可达即说明 tag 建在了非主干提交上，须在发版流程内处置。

## 6. Hotfix 与 SLA

### 6.1 Hotfix 流程

```
1. 从最新主干等价提交创建 hotfix 分支：
   git checkout -b hotfix/v1.0.31.1 <主干等价提交>
   （先按 §5.1 判定该 tag 是否在主干可达集；不可达时改从 §5.1 表的「主干等价提交」建分支，
     否则 hotfix 与 main 无共同祖先，第 6 步无法干净合并回去）
2. 最小化修复（仅修复 P0 问题，不引入新功能）
3. ./gradlew bumpVersion -Pbump=patch（如确需四段式 hotfix 版本号，先扩展 gradle.properties 集中版本位再使用）
4. 在 CHANGELOG.md 添加 hotfix 条目
5. 提交 + 打 tag + 推送触发 release.yml
6. 合并 hotfix 回 main：git checkout main && git merge hotfix/v1.0.31.1 && git branch -d hotfix/v1.0.31.1
```

### 6.2 Hotfix SLA 三级（本仓特有）

| 级别 | 响应时间 | 修复时间 |
|------|----------|----------|
| P0（崩溃 / 数据丢失） | 1 小时内确认 | 24 小时内发布 hotfix |
| P1（核心功能不可用） | 4 小时内确认 | 72 小时内发布 hotfix |
| P2（非核心功能异常） | 24 小时内确认 | 下一个版本修复 |

### 6.3 目标可用性与错误预算

- 目标可用性：待定（负责人 + 触发条件）。
- 错误预算：随目标可用性一并待定（负责人 + 触发条件）；冻结非紧急发布并复盘的阈值待定（负责人 + 触发条件）。

## 7. 向后兼容性（本仓特有）

### 7.1 数据库迁移（Room）

- 所有 Room schema 变更通过 Migration 类实现，**不使用** `fallbackToDestructiveMigration`。
- 新版本发布前须运行 Migration 测试，验证从 N-1 升级路径。
- 最低支持版本：当前版本 - 3（即 4 个版本内的升级路径须测试）。

### 7.2 破坏性变更通知

- 破坏性变更须提前 1 个版本在 CHANGELOG 的 `### Deprecated` 中预告。
- 正式移除时在 `### Removed` 中说明替代方案。
- App 内通过 Onboarding 或通知提示用户。

## 8. 发布前检查清单

- [ ] `versionCode`（= MAJOR*10000+MINOR*100+PATCH，单调递增）与 `versionName` 已更新
- [ ] CHANGELOG.md 的 `[Unreleased]` 已改为版本号 + 日期
- [ ] `./gradlew test` 全部通过
- [ ] `./gradlew lint` 无新增 error
- [ ] `./gradlew assembleRelease` 构建成功
- [ ] APK 签名验证通过（`apksigner verify --verbose`）
- [ ] SHA256 校验和已生成（`.apk.sha256`）
- [ ] 安全模块（AntiDebug / ApkIntegrityChecker）在 Release APK 上正常工作
- [ ] Git tag 已推送，触发 release.yml
- [ ] 本次新打的版本 tag 在主干可达集内（`git tag --no-merged HEAD` 不再列出它），且已在 §5.1 对照表登记「tag 提交 / 内容树 / 主干等价提交」三元组
- [ ] 本地未产出任何发布交付物（全部远端构建，§0）

---

## 禁止事项

- 不得重写 §10 的公共条款（远端优先、tag 驱动、版本位一致、不可变标签、无 GPG 分离签名）。
- 不得引入 floating `latest` 作发布基线；不得保留或新建 GPG 签名流程。
- 不得留空占位符；未定项写「待定（负责人 + 触发条件）」，不得编造阈值。
- 不得重指向 / 删除 tag 或移动远端 ref（§5 破坏性动作红线）。
