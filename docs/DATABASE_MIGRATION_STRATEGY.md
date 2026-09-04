# DraftPeek 数据库迁移安全策略

> 本文档定义 DraftPeek 项目 Room 数据库 schema 变更的迁移规范、测试要求与向后兼容策略。

---

## 1. 迁移原则

### 1.1 核心规则

1. **禁止 `fallbackToDestructiveMigration`**：升级迁移失败时应抛出异常暴露 bug，而非静默删除用户数据
2. **禁止修改已发布的 Migration 类**：已发布的迁移是不可变的历史记录，修改会破坏旧版本升级路径
3. **必须导出 Schema JSON**：`exportSchema = true` + `room.schemaLocation` 确保 schema 快照可审计
4. **必须编写迁移测试**：每个新 Migration 必须配套编写迁移测试，验证数据完整性

### 1.2 降级策略

- 仅允许 `fallbackToDestructiveMigrationOnDowngrade`（用户从高版本回退时重建数据库）
- 升级路径不允许任何 destructive migration

---

## 2. 迁移编写规范

### 2.1 新增 Migration 步骤

```
1. 在 AppDatabase.kt 中添加 MIGRATION_N_N+1
2. 在 DataModule.kt 的 addMigrations() 中注册
3. 更新 @Database(version = N+1)
4. 在 schemas/ 目录确认新 schema JSON 已生成
5. 在 DatabaseMigrationTest.kt 中添加测试：
   - migration_N_N1_*：验证 schema 变更
   - migration_N_N1_dataIntegrity：验证数据完整性
6. 运行迁移测试确认通过
```

### 2.2 安全迁移模式

**列添加**（推荐）：
```kotlin
db.execSQL("ALTER TABLE table_name ADD COLUMN new_col INTEGER NOT NULL DEFAULT 0")
```

**表结构变更**（需数据迁移）：
```kotlin
// 1. 创建新表
db.execSQL("CREATE TABLE IF NOT EXISTS table_name_new (...)")
// 2. 复制数据（使用 COALESCE 处理 NULL）
db.execSQL("INSERT INTO table_name_new (...) SELECT COALESCE(col, 0) FROM table_name")
// 3. 删除旧表
db.execSQL("DROP TABLE table_name")
// 4. 重命名
db.execSQL("ALTER TABLE table_name_new RENAME TO table_name")
// 5. 重建索引
db.execSQL("CREATE INDEX IF NOT EXISTS index_name ON table_name(col)")
```

---

## 3. 测试要求

### 3.1 迁移测试覆盖

每个 Migration 须测试以下维度：

| 测试维度 | 说明 |
|----------|------|
| Schema 变更 | 新表/列/索引结构正确 |
| 数据完整性 | 迁移前后数据一致 |
| DAO 可用性 | 迁移后 CRUD 操作正常 |
| 全链路迁移 | 从 N-4 版本连续迁移至当前版本 |

### 3.2 测试命名规范

```
migration_{from}_{to}_{描述}
migration_9_10_bookmarksTableWorks
migration_10_11_securityEventsTableWorks
migration_8_9_dataIntegrity
fullMigrationChain_allTablesWork
```

---

## 4. 向后兼容策略

### 4.1 支持版本范围

- 最低支持从 N-3 版本升级（即当前版本前 3 个版本）
- 超出范围的升级路径不保证，但迁移链必须完整

### 4.2 破坏性变更流程

1. **预告**：在 CHANGELOG 的 `### Deprecated` 中标记将在下一版本移除的字段/表
2. **过渡**：新版本中同时保留旧字段和新字段，通过 Migration 填充新字段
3. **移除**：下一版本正式移除旧字段，在 `### Removed` 中记录

### 4.3 Schema 版本快照

- `core/data/schemas/` 目录存储所有版本的 schema JSON
- 每次版本升级时 KSP 自动生成新 schema
- Schema 文件须提交到 Git，用于迁移测试验证

---

## 5. 紧急回滚

> 修正（2026-09-04，发布管理评估 P2）：本项目**未接入 Google Play 分发**
> （产物仅发布到 GitHub Release，见 `docs/release-version-management-evaluation.md`），
> 也不存在 Play Console 的「暂停发布」能力；且 Android 客户端升级后**无法强制降级**。
> 原文的「Play Console 暂停发布」为不存在的能力，已按真实分发渠道改写。

如果新版本数据库迁移导致严重问题：

1. **停止扩散**（本项目真实可用的手段，按优先级）：
   - 撤下 GitHub Release：把已发布的 Release 转回 **draft** 或删除（用户不再能下载到坏版本）；
   - 若使用自托管下载页 / in-app 更新检测：下架对应 APK 并把更新检查指向上一稳定版本；
   - 在 `CHANGELOG.md` 中标注该版本不推荐使用（Known Issue）。
2. **止损替代（kill-switch 缺位）**：当前应用**无远程 feature flag / kill-switch**，
   无法远程关闭出问题的功能。在补上远程配置层之前，schema 事故的止血只能靠
   「撤分发 + 发 hotfix」，因此 schema 变更的**发版前迁移测试（§3）是唯一防线**，不可跳过。
3. **数据保护**：`fallbackToDestructiveMigrationOnDowngrade` 确保降级时重建数据库
   （注意：这是兜底，会丢数据，不作为常规回滚手段宣传）。
4. **修复迁移**：在 hotfix 分支修复迁移逻辑，发布 hotfix 版本
   （遵循 `docs/RELEASE_CHECKLIST.md`，tag 只推 `private` 远程）。
5. **长期方向**：引入 feature flag / 远程配置作为 kill-switch 轻量替代
   （评估报告 P2 项），使「坏 schema 功能」可被远程关闭而无需撤整个版本。
