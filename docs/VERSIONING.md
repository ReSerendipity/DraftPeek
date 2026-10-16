# DraftPeek 版本管理规范

> 本文档定义 DraftPeek 项目的版本号命名、发布流程、灰度策略与紧急热修复能力规范。
> 遵循 [Semantic Versioning 2.0.0](https://semver.org/lang/zh-CN/) 与 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

---

## 1. 版本号规范

### 1.1 格式

```
MAJOR.MINOR.PATCH[-PRERELEASE]
```

| 段 | 含义 | 递增条件 |
|----|------|----------|
| MAJOR | 不兼容的 API 变更 | 数据库 schema 不兼容迁移、核心架构重构 |
| MINOR | 向后兼容的新功能 | 新增 feature 模块、新语言语法支持 |
| PATCH | 向后兼容的 Bug 修复 | 安全补丁、UI 修复、性能优化 |
| PRERELEASE | 预发布标识 | `-alpha.N`、`-beta.N`、`-rc.N` |

### 1.2 versionCode 规则

- `versionCode` 必须单调递增（整数）
- 每次发布（含 hotfix）必须递增 `versionCode`
- 公式：`versionCode = MAJOR * 10000 + MINOR * 100 + PATCH`
- 示例：`1.0.30` → `versionCode = 10030`

### 1.3 预发布标签

| 标签 | 用途 | 示例 |
|------|------|------|
| `-alpha.N` | 内部测试，功能不完整 | `1.1.0-alpha.1` |
| `-beta.N` | 封闭测试，功能完整待验证 | `1.1.0-beta.1` |
| `-rc.N` | 发布候选，候选正式版 | `1.1.0-rc.1` |

---

## 2. CHANGELOG 管理

### 2.1 格式

CHANGELOG.md 遵循 Keep a Changelog 格式：

```markdown
## [VERSION] - YYYY-MM-DD

### Added
- 新增功能描述

### Changed
- 变更描述

### Deprecated
- 废弃功能描述

### Removed
- 移除功能描述

### Fixed
- 修复描述

### Security
- 安全修复描述
```

### 2.2 规则

- 每次 PR 合并至 `main` 时，开发者须在 `## [Unreleased]` 下追加变更条目
- 正式发布时将 `[Unreleased]` 改为 `[VERSION] - YYYY-MM-DD`
- 变更条目须与 commit message 中的 type 对应：`feat → Added`、`fix → Fixed`、`refactor → Changed`

---

## 3. 发布流程

### 3.1 正式发布流程

```
1. 确认 [Unreleased] 变更条目完整
2. 运行 `./gradlew bumpVersion -Pbump=patch|minor|major`（自动回写 `gradle.properties` 集中版本位；勿手改 `app/build.gradle.kts`，versionCode 由 major*10000+minor*100+patch 自动核算）
3. 将 [Unreleased] 改为 [VERSION] - DATE
4. 提交：git commit -m "release(v1.0.31): bump version + changelog"
5. 打 tag：git tag v1.0.31
6. 推送 tag：git push origin v1.0.31  （触发 release.yml 自动构建）
7. GitHub Actions 构建完成后，在 GitHub Release 页面审核 draft
8. 确认无误后发布 Release
```

### 3.2 灰度发布策略

| 阶段 | 范围 | 通过标准 | 回滚触发 |
|------|------|----------|----------|
| Internal | 开发团队 (5-10人) | 无 P0/P1 bug，crash rate < 0.1% | 任一 P0 bug |
| Closed Beta | 封闭测试群 (50-100人) | crash rate < 0.5%，无数据丢失 | crash rate > 1% 或数据丢失 |
| Open Beta | 公开测试 (1000+人) | crash rate < 0.3%，用户反馈正面 | crash rate > 0.5% |
| Production | 全量发布 | crash rate < 0.2% | crash rate > 0.5% 或关键功能不可用 |

### 3.2.1 崩溃观测口径（「仅本地」声明）

> 对应《移动端工程体系 Flash 报告》P1-2：稳定性观测缺 ANR 与 native 崩溃采集、崩溃上报端点 `CRASH_REPORT_URL` 默认空串即关闭。本小节把现状口径成文，防止把「灰度通过标准里的 crash rate」误读为已具备远端口径。

- **当前口径（2026-10-15 成文）：崩溃观测为「仅本地」**——崩溃率按版本维度由客户端本地 `CrashMetrics`（会话/崩溃计数、90 天日记录）统计；`CRASH_REPORT_URL` 默认空串即上报关闭；仓库无上报接收端实现（`server/` 仅 sync/crdt/integrity 三个原型服务，且云同步已裁撤）。**无 ANR/native tombstone 采集，无远端聚合看板**。
- 因此 §3.2 灰度表中各阶段 crash rate 通过/回滚阈值，**当前以本地抽样与人工导出口径近似评估**；在「接入 ANR（`ApplicationExitInfo`）+ native tombstone 采集并明确上报端点归属」落地之前，不得宣称具备远端口径。
- 改进方向（P1-2，未实施）：接入 ANR + native 崩溃采集，明确上报端点归属或维持「仅本地」；落地后本小节同步改写并随版本登记。

### 3.3 回滚机制

- **应用商店回滚**：在 Google Play Console 中回滚到上一版本（需上一版本仍上架）
- **直接下载回退**：维护上一版本 APK + SHA256 校验和，在 GitHub Release 中可访问
- **Feature Flag Kill-Switch**：通过远程配置（或 BuildConfig）禁用问题功能模块
- **按版本号回退代码状态**：用 §3.4 的「主干等价提交」列检出，勿直接用版本 tag（部分 tag 不在主干可达集）

### 3.4 版本号 → 主干修订对照与 tag 可达性核查（2026-10-17 建立）

背景：历史身份改写（`.mailmap` 归并 6 个别名身份）会让发布 tag 指向**与主干无祖先关系**的重复提交。此时按 tag 检出或建 hotfix 分支，拿到的是旧提交线的内容，与 `main` 上的等价内容不同源，合并或比对会出现大量无关差异。

截至 2026-10-17 实测：`git tag | wc -l` = 21，其中 `git tag --no-merged HEAD` = 10 不可达 —— 两枚版本 tag（`v1.0.31`、`v1.0.32`）与 8 枚 `ci-verify-*`。版本号到主干修订的对应关系如下表，**回退或核对一律用「主干等价提交」列，不要用 tag 列**。

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
- **重指向或删除 tag、移动任何远端 ref 均属破坏性/远端动作，需仓库所有者单独授权**：`release.yml` 由 `push: tags` 触发，移动已发布 tag 会当场重跑发布链，历史上有过因此产出混年代 Release 资产的情况。

---

## 4. 紧急热修复（Hotfix）

### 4.1 流程

```
1. 从最新 release tag 创建 hotfix 分支：
   git checkout -b hotfix/v1.0.31.1 v1.0.31
   （先按 §3.4 判定该 tag 是否在主干可达集；不可达时改从 §3.4 表的「主干等价提交」建分支，
     否则 hotfix 与 main 无共同祖先，第 6 步无法干净合并回去）

2. 最小化修复（仅修复 P0 问题，不引入新功能）

3. 版本号走集中机制：`./gradlew bumpVersion -Pbump=patch`（如确需四段式 hotfix 版本号，先扩展 `gradle.properties` 集中版本位再使用）

4. 在 CHANGELOG.md 添加 hotfix 条目

5. 提交 + 打 tag + 推送触发 release.yml

6. 合并 hotfix 回 main：
   git checkout main
   git merge hotfix/v1.0.31.1
   git branch -d hotfix/v1.0.31.1
```

### 4.2 SLA 目标

| 级别 | 响应时间 | 修复时间 |
|------|----------|----------|
| P0（崩溃/数据丢失） | 1 小时内确认 | 24 小时内发布 hotfix |
| P1（核心功能不可用） | 4 小时内确认 | 72 小时内发布 hotfix |
| P2（非核心功能异常） | 24 小时内确认 | 下一个版本修复 |

---

## 5. 构建可重复性

### 5.1 依赖锁定

- `gradle/libs.versions.toml` 统一管理所有依赖版本
- Gradle wrapper 锁定 Gradle 版本（`gradle-wrapper.properties`）
- CI 使用 `temurin JDK 17` 固定 JDK 发行版

### 5.2 校验和

- 每次 Release 构建后生成 SHA256 校验和
- GitHub Release 附带 `.apk.sha256` 文件
- 用户可通过 `sha256sum -c app-release.apk.sha256` 验证

---

## 6. 向后兼容性

### 6.1 数据库迁移

- 所有 Room schema 变更通过 Migration 类实现，不使用 `fallbackToDestructiveMigration`
- 新版本发布前须运行 Migration 测试验证从 N-1 升级路径
- 最低支持版本：当前版本 - 3（即 4 个版本内的升级路径须测试）

### 6.2 破坏性变更通知

- 破坏性变更须提前 1 个版本在 CHANGELOG 的 `### Deprecated` 中预告
- 正式移除时在 `### Removed` 中说明替代方案
- App 内通过 Onboarding 或通知提示用户

---

## 7. 发布前检查清单

发布前须逐项确认：

- [ ] `versionCode` 和 `versionName` 已更新
- [ ] CHANGELOG.md 的 `[Unreleased]` 已改为版本号 + 日期
- [ ] `./gradlew test` 全部通过
- [ ] `./gradlew lint` 无新增 error
- [ ] `./gradlew assembleRelease` 构建成功
- [ ] APK 签名验证通过（`apksigner verify --verbose`）
- [ ] SHA256 校验和已生成
- [ ] 安全模块（AntiDebug / ApkIntegrityChecker）在 Release APK 上正常工作
- [ ] Git tag 已推送，触发 release.yml
- [ ] 本次新打的版本 tag 在主干可达集内（`git tag --no-merged HEAD` 不再列出它），且已在 §3.4 对照表登记「tag 提交 / 内容树 / 主干等价提交」三元组
