# DraftPeek AGENTS.md — AI 辅助开发指南

> 🧬 **自进化协议版本**：v1.5  
> 📅 **最后更新日期**：2026-08-29  
> 🎯 **对应项目版本**：v1.0.30 (10030)

---

## 0. 文档优先级（单一事实来源）

当以下文档相互矛盾时，**以此顺序为准**，并立即按铁律 #1 修正靠后者：

1. 代码与配置本身（`pyproject.toml` / `package.json` / `.pre-commit-config.yaml` / 源码）
2. `docs/official_spec.md`（若本仓存在；当前本仓无此文件）
3. `AGENTS.md`
4. `README.md` / `docs/**`
5. `CHANGELOG.md`

> 判据：**能被机器验证的事实永远优先于自然语言描述。**

---

## ⚠️ 🤖 Agent 行为契约（自进化协议 · 必须严格遵守）

AI Agent 打开本文件后的**第一件事**是执行下面的「🧪 自进化自检清单」，并遵守以下 5 条铁律：

### 🔴 6 条自进化铁律
1. **🔄 同步规则（Synchronize First）**：如果发现项目实际情况（目录结构、依赖版本、技术栈、配置文件名等）与本文件描述 **不一致** → **立即更新本文件**，不要只改代码不改 AGENTS.md。这是最高优先级的规则。
2. **📝 坑点累积（Gotchas Accumulation）**：每次修复 Bug / 踩坑后（哪怕是很小的坑），**必须** 追加一条到第 12 节「常见陷阱（Known Gotchas）」，写清楚：触发场景、现象/报错、正确做法、首次发现日期。
3. **📚 SOP 累积（SOP Accumulation）**：每次完成一个「本文件现有 SOP 没覆盖」的典型开发任务后（比如第一次加 Room DAO、第一次写 Compose 动画、第一次写 Instrumented Test），**必须** 把步骤整理成新 SOP 追加到第 11 节「典型 AI 开发场景 SOP」。
4. **✅ 自检流程（Self-Check on Startup）**：每次打开本文件准备工作前，**必须** 先运行下面的「🧪 自进化自检清单」，逐项核对，有任何一项不符先修正 AGENTS.md 再干活。
5. **🏷️ 版本递增（Version Increment）**：每次更新本文件内容后，**必须** 做三件事：① 文件顶部「自进化协议版本号」+0.1（小改）或 +1.0（大改/框架调整）；② 更新「最后更新日期」；③ 在文件末尾「📋 自进化修订记录表」追加一行记录。
6. **🔬 证据绑定（Evidence Binding）**：本文件中每出现一个**可执行文件路径**（脚本、配置、workflow、源码），它必须是**当时可验证存在**的。引用前跑一次 `python scripts/check_spec_refs.py`；若确实想描述尚未实现的东西，必须显式加 `（计划，未实现）` 前缀。禁止把"CI 会阻断 X"写成一个 CI 里不存在的门禁。

### 🧪 自进化自检清单（每次启动工作前必跑）
- [ ] 目录结构（`app/src/`、`feature/`、`core/`、`scripts/`）是否和第 3 节模块边界描述一致？
- [ ] `gradle/libs.versions.toml` 中的核心版本（Kotlin、Compose BOM、AGP）是否和第 1 节 Quick Facts 一致？
- [ ] 上次工作是否踩了新坑？如果是，是否已追加到第 12 节 Known Gotchas？
- [ ] 是否新增了模块/目录？如果是，是否已更新第 3 节模块边界？
- [ ] 是否新增了 Gradle Task 或修改了签名配置？如果是，是否已更新第 6 节构建命令？
- [ ] 上次更新是否正确递增了自进化协议版本号 + 追加了修订记录表？
- [ ] 本文引用的 scripts/ configs/ workflows/ 路径是否全部真实存在？（跑 `python scripts/check_spec_refs.py`，要求退出码 0）
- [ ] §pre-commit 表格是否与 `.pre-commit-config.yaml` **双向**一致？（既无虚构钩子，也无漏记实际钩子）

---

## 1. 项目概览

> DraftPeek：在手机上写完文档，立刻预览成品。  
> 技术栈：**Kotlin 2.0.21 + Jetpack Compose BOM 2024.10.02 + Room 2.6.1 + WorkManager 2.10.0 + JGit 6.10.0.202406032230-r + DataStore 1.1.1 + Material3 + Splash Screen + Navigation Compose 2.8.4 + Markwon 4.6.2**  
> （2026-03 起 Markwon 已移除，改用 CommonMark 4.0 + commonmark-ext-autolink + commonmark-ext-heading-anchor + flexmark-java + Java-diff-utils 4.12）

| 项目 | 数值 |
|------|------|
| 包名 / ApplicationId | `com.draftpeek`（dev/staging 变体带 `.dev` / `.staging` 后缀；2026-08-29 按 `app/build.gradle.kts` 实测修正，旧值 `net.apricotforest.draftpeek` 已废弃） |
| 当前版本 | `v1.0.30`（`versionCode = 10030`；版本号集中管理于 `gradle.properties` 的 `draftpeek.version.major/minor/patch`，`versionCode = major*10000 + minor*100 + patch`，由 `app/build.gradle.kts` 计算） |
| minSdk | `26` (Android 8.0) |
| targetSdk / compileSdk | `35` (Android 15) |
| AGP 版本 | `8.7.3` (`com.android.application` 插件) |
| Gradle 版本 | Gradle Wrapper `gradle-8.12.1`（Project JDK：Amazon Corretto 17） |
| Kotlin 版本 | `2.0.21` |
| 默认编译模式 | **默认 Release 编译**（R8 非 full-mode，AGP 8.7.x baseline-prof 生成器有崩溃已知问题） |
| 安全栈 | **Tink 1.16.1（AES-GCM）+ SQLCipher 4.5.7 + Jetpack Security 1.1.0-alpha06 + 自定义 Application 安全门控** |
| UI 组件 | **自研 Brand Design System（`core-ui/`），禁用原生 Material3 组件** |
| Native 组件 | NDK 27.0.12077973，CMake 3.31.6 + Ninja 1.12.1，sora-editor C++ JNI，libc++_shared.so 打包到每个 ABI |
| 数据库 | Room + SQLCipher（`net.zetetic:android-database-sqlcipher:4.5.7`），DB 路径固定在 `app_database/documents.db` |
| 偏好存储 | EncryptedDataStore（文件加密 Tink，key 存 EncryptedSharedPreferences） |

---

## 2. 代码风格约定

### 2.1 Kotlin 通用约定
- **命名规则**：类/接口/对象用 `PascalCase`，函数/变量/属性用 `camelCase`，常量用 `UPPER_SNAKE_CASE`，接口不加 `I` 前缀
- **文件命名**：一个文件一个 public 类/接口 → 文件名和类名相同（PascalCase）；扩展函数文件 → 被扩展类名+`s`（如 `Contexts.kt`）
- **单表达式函数**：优先用 `= ` 写，不要加 `{}` 和 `return`
- **可空处理**：优先用 `?.` / `?:` / `let`，**不要强行 `!!`**（除非是 onCreate/onViewCreated 里已初始化的 lateinit var，且加 `//noqa` 注释说明理由）
- **协程**：VM / Repository / DAO 层一律用 `suspend` 或 `Flow`，**严禁在非测试代码中使用 `runBlocking`**；切换调度器必须显式写 `withContext(Dispatchers.IO)` 而不是依赖 Room/Retrofit 自动切换
- **禁止使用的旧 API**：`AsyncTask`、`LiveData.observeForever`、`Handler().post()`（必须带 Looper 说明）、`GlobalScope`

### 2.2 Jetpack Compose 专项约定
- **Composable 函数命名**：`PascalCase`，如 `HomeScreen()`、`DraftListItem()`
- **UI 组件必须走 Brand 系统**：一律用 `BrandText` / `BrandButton` / `BrandCard` / `BrandScaffold`（来自 `core-ui`），**严禁直接调用 `androidx.compose.material3.Text` / `Button` / `Card` 等原生组件**——否则会绕过品牌设计系统，破坏样式一致性
- **State 必须 hoist**：Screen 级 Composable 接收 state + lambda，不要内部持有 `mutableStateOf`（除非是 UI 临时状态如展开/折叠，且生命周期不跨 Screen）
- **Preview 注解**：必须加 `@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)`，Screen 级必须加 `@Preview(uiMode = UI_MODE_NIGHT_YES)` 深色模式预览

### 2.3 资源命名
- 布局资源（Compose 项目已无 XML 布局，忽略本条）
- drawable：`ic_` 前缀图标、`bg_` 前缀背景、`divider_` 前缀分割线
- string：`screen_xxx_` / `dialog_xxx_` / `common_` 前缀分组
- color：`color_` + 语义名（如 `color_primary` / `color_on_surface`），不要 `color_red_ff0000`

### 2.4 导入顺序（IDE 默认即可，不要手动调整）
```
Android / androidx.*
kotlinx.* / coroutine.*
java.* / javax.*
第三方库（com.squareup.* / io.noties.* 等）
本地项目（com.draftpeek.*）
```

### 2.5 格式化 & Lint
- **格式化**：统一用 Android Studio 默认 Kotlin formatter（`Settings → Editor → Code Style → Kotlin` → Set from → Kotlin style guide）。提交前 `Code → Reformat Code` + `Optimize Imports`
- **静态检查**：`./gradlew spotlessCheck`（如果引入了 spotless 插件；未引入则忽略）
- **不允许 suppress 的 Lint 警告**：`HardcodedText` / `MissingTranslation` / `ShowToast` / `ApplySharedPref` —— 除非加详细注释说明为什么必须 suppress

---

## 3. 模块边界 & 依赖规则（🚫 严格执行，不允许跨层引用）

```
┌─────────────────────────────────────────────────────────────┐
│                        app (入口层)                           │
│  ┌──────────┐  ┌───────────┐  ┌──────────────┐             │
│  │ Application│  │ MainActivity │  │ Tink/SQLCipher │            │
│  └─────┬────┘  └─────┬─────┘  └──────┬───────┘             │
└────────┼─────────────┼───────────────┼──────────────────────┘
         │             │               │
┌────────▼─────────────▼───────────────▼──────────────────────┐
│                     feature/* (业务层)                        │
│  ┌──────────┐  ┌───────────┐  ┌──────────────┐  ┌────────┐  │
│  │feature-  │  │feature-   │  │ feature-     │  │feature-│  │
│  │  home    │  │  editor   │  │  settings    │  │ git    │  │
│  └────┬─────┘  └─────┬─────┘  └──────┬───────┘  └──┬─────┘  │
└───────┼───────────────┼───────────────┼─────────────┼────────┘
        │               │               │             │
┌───────▼───────────────▼───────────────▼─────────────▼────────┐
│                      core/* (基础能力层)                        │
│  ┌──────┐  ┌───────┐  ┌──────┐  ┌────────┐  ┌─────────────┐  │
│  │core- │  │core-  │  │core- │  │core-   │  │ core-model  │  │
│  │ ui   │  │ data  │  │ model│  │common  │  │  (DTO/Entity)│  │
│  └──┬───┘  └───┬───┘  └──┬───┘  └───┬────┘  └──────┬──────┘  │
└─────┼──────────┼─────────┼───────────┼───────────────┼─────────┘
      │          │         │           │               │
┌─────▼──────────▼─────────▼───────────▼───────────────▼─────────┐
│                 third_party/* (第三方隔离层)                      │
│  ┌───────────────────┐  ┌──────────────┐  ┌────────────────┐    │
│  │ markwon-parser    │  │  sora-editor  │  │  jgit-wrapper   │    │
│  └───────────────────┘  └──────────────┘  └────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### 3.1 依赖方向（只允许从上往下，严禁反向依赖）
| 模块 | 可以依赖 | 绝对不能依赖 |
|------|---------|------------|
| `app` | **所有** feature-*、所有 core-*、所有 third_party-* | — |
| `feature-home` | core-ui、core-data、core-model、core-common、third_party-* | 其他 feature-*（要跨 feature 用 core-model 暴露接口） |
| `feature-editor` | core-ui、core-data、core-model、core-common、third_party/markwon、third_party/sora-editor | 其他 feature-*、严禁直接在 feature 里写 parser 逻辑（要走 third_party 层） |
| `feature-settings` | core-ui、core-data、core-model、core-common | feature-editor / feature-git |
| `feature-git` | core-data、core-model、core-common、third_party/jgit-wrapper | core-ui（git 模块不能有 UI！） |
| `core-ui` | core-model、core-common | 任何 feature-*、core-data |
| `core-data` | core-model、core-common、third_party-*（sora-editor 除外） | 任何 feature-*、core-ui |
| `core-model` | 无 | 所有其他模块 |
| `core-common` | 无 | core-ui / core-data / feature-* |
| `third_party/*` | core-common、core-model（仅 DTO 定义） | 所有 feature-*、所有 core-* 除 common/model |

### 3.2 禁区目录（禁止 AI 自动修改，必须人工确认）
- `app/src/main/cpp/sora/**`（sora-editor 源码，已冻结到 4.2.3-hotfix12，必须人工 review 每一行变更）
- `third_party/sora-editor/**`（JNI 封装，同样冻结）
- `app/src/main/AndroidManifest.xml`（权限声明、组件注册变动必须人工确认安全合规）
- `proguard-rules.pro`（R8 规则，禁止 AI 自动加 `-dontwarn` 解决问题，必须写 clear `-keep` 规则并附理由）

---

## 4. 测试约定

覆盖率门禁（诚实设定，家族治理 D9）：Android/Gradle 项目**未配置统一 CI 覆盖率阈值**；质量基线 = `./gradlew :app:testDebugUnitTest` 全绿 + 安全负向用例（`AntiDebugNegativeTest` 等）。

### 4.1 测试框架
| 类型 | 框架 | 运行命令 | 说明 |
|------|------|---------|------|
| 单元测试（JVM） | JUnit 5 + MockK + Turbine（Flow 测试） | `./gradlew :app:testDebugUnitTest` | 90% 的测试都在这里跑 |
| 数据层测试 | AndroidTest（Room + SQLCipher 在真机/模拟器测加密） | `./gradlew :app:connectedDebugAndroidTest` | Room Migration 必须用 AndroidTest |
| UI 测试 | Compose Test（JUnit4，因为 Compose Test 暂不支持 JUnit5） | `./gradlew :app:connectedDebugAndroidTest` | `createComposeRule()` 写 UI 用例 |
| 基准测试 | Macrobenchmark（可选） | `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest` | 冷启动/滚动流畅度 |

### 4.2 测试命名规范
- **类名**：`被测类 + Test`（PascalCase），如 `DocumentRepositoryTest`
- **函数名**：`fun should_<期望行为>_when_<触发条件>()`，例子：
  ```kotlin
  @Test
  fun `should return encrypted document when loadById with valid id`() = runTest { ... }
  ```
- **Backtick 命名**：允许测试函数名用反引号 + 空格（如上），可读性优先

### 4.3 Flow / 协程测试规范
- **一律用 `kotlinx-coroutines-test` 的 `runTest`**，不要 `runBlocking`
- **StateFlow 断言必须用 Turbine**：`stateFlow.test { ... }` + `awaitItem()`，不要直接读 `.value` 断言（时序问题）
- **测试调度器必须注入**：Repository/VM 不要硬编码 `Dispatchers.IO`，构造函数传 `CoroutineDispatcher`（默认 `Dispatchers.IO`，测试时传 `StandardTestDispatcher()`）

---

## 5. 安全注意事项（🚫 不允许违反）

1. **严禁明文存密码/密钥**：EncryptedDataStore / Tink / EncryptedSharedPreferences 三选一，禁止 `SharedPreferences.getString("password")`
2. **严禁硬编码密钥**：所有密钥必须由 **Tink 生成 + 存 EncryptedSharedPreferences**，禁止 `const val SECRET_KEY = "xxxxx"`（包括 Native 层也要从 Tink 取）
3. **SQL 注入防护**：Room DAO 一律用 `@Query("SELECT * FROM doc WHERE id = :id")`，**不要字符串拼接 SQL**（哪怕是本地库）
4. **文件访问路径限制**：用户文档目录（`context.filesDir + "documents/"`）必须走自定义安全管理器，禁止直接 `File(path)` 读任意路径（防止路径穿越）
5. **Intent / URI Scheme**：`deep_link` 收到的 URI 必须二次校验 host/scheme/path，禁止直接把 URI path 当文件路径打开
6. **WebView 配置**：DraftPeek 暂未引入 WebView，如果以后引入 → 必须禁用 `setAllowFileAccess` / `setAllowUniversalAccessFromFileURLs`，且 JS Bridge 接口必须加 `@JavascriptInterface` 白名单
7. **安全门控**：`DraftPeekApplication.onCreate()` 里有 Native 安全检测（完整性 + 调试器检测 + root 检测），改 Application 类必须人工 review，防止绕过安全门控

---

## 6. 构建/运行命令

> ⚠️ **重要**：本项目默认是 **Release 编译**（`build.gradle.kts` 中 `defaultConfig` 已配置 `minifyEnabled = true`，R8 非 full-mode）。Debug 编译需 `./gradlew assembleDebug` 或 Android Studio 手动切换 Variant。

| 命令 | 作用 |
|------|------|
| `./gradlew clean` | 清理 build 目录（遇到诡异编译错误先跑这个） |
| `./gradlew :app:assembleDebug` | Debug APK（不混淆，测试用），产物：`app/build/outputs/apk/debug/app-debug.apk` |
| `./gradlew :app:assembleRelease` | **Release APK（R8 混淆）**，产物：`app/build/outputs/apk/release/app-release.apk` |
| `./gradlew :app:bundleRelease` | **AAB 上架 Google Play 包**，产物：`app/build/outputs/bundle/release/app-release.aab` |
| `./gradlew :app:testDebugUnitTest` | 本地单元测试（JUnit5 + MockK，全部用例跑完约 3-5 分钟） |
| `./gradlew :app:connectedDebugAndroidTest` | 真机/模拟器 AndroidTest（Room 加密 + Compose UI，需连设备） |
| `./gradlew :app:spotlessApply`（如有） | 自动格式化 |

### 6.1 签名配置
Release 签名 keystore 文件存 `app/keystore/release.jks`（**此文件禁止提交到 Git**，已在 `.gitignore` 排除）。

签名配置从 `local.properties` 读 4 个 key：
```properties
storeFile=keystore/release.jks
storePassword=你的密钥库密码
keyAlias=draftpeek-release
keyPassword=你的密钥密码
```

> 如果 `local.properties` 没有这 4 个 key，assembleRelease 会报错「找不到 signingConfig」，请先问用户要签名信息，不要自动生成假签名。

### 6.2 Project JDK
**必须用 JDK 17（Amazon Corretto 17 推荐）**。`gradle/wrapper/gradle-wrapper.properties` 已锁 JDK 17。  
AGP 8.7.x + Kotlin 2.0.x **绝对不能用 JDK 21+**（有兼容问题，会报 `Unsupported class file major version 65`）。

---

## 7. 依赖管理 & 版本目录（libs.versions.toml）

所有第三方库版本统一声明在 `gradle/libs.versions.toml`，**禁止在 `build.gradle.kts` 里硬编码版本号**。

```toml
[versions]
kotlin = "2.0.21"
agp = "8.7.3"
compose-bom = "2024.10.02"
room = "2.6.1"
lifecycle = "2.8.7"
tink = "1.16.1"
sqlcipher = "4.5.7"
jgit = "6.10.0.202406032230-r"
# ... 其他版本

[libraries]
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
# ... 其他 libraries
```

### 7.1 加新依赖流程
1. 先在 `libs.versions.toml` 加 `[versions]` 条目 + `[libraries]` 条目
2. 再在对应模块的 `build.gradle.kts` 写 `implementation(libs.androidx.room.runtime)`
3. **禁止** 在某个模块的 `build.gradle.kts` 里直接写 `"androidx.room:room-runtime:2.6.1"` 这种硬编码版本

---

## 8. Git / 提交规范 & 分支策略（🚨 安全红线，严禁违反）

### 8.1 ⚠️ 双分支隔离策略（核心原则）

本项目采用 **本地私有主分支 + 远程公开展示分支** 的双分支隔离架构：

| 分支名称 | 类型 | 用途 | 推送远程 | 包含内容 |
|---------|------|------|---------|---------|
| **main** | 本地私有分支 | **日常开发与私有工作的主分支** | ❌ **禁止推送到远程** | 完整源代码、加密密钥、敏感配置、所有功能代码 |
| **public** | 远程公开分支 | **GitHub 仓库的可见内容** | ✅ 只推送这个分支 | README、截图、构建脚本、示例配置（**不含源代码**） |

#### 🛑 绝对禁止的行为（会导致源代码泄露）
- ❌ `git push origin main` —— 会将私有源代码泄露到 GitHub
- ❌ 在 public 分支提交 `.kt` / `.java` 源代码文件
- ❌ 将 `local.properties`（含签名密钥）、`keystore/` 目录推送到任何远程
- ❌ 把 secret/key/password 硬编码提交到 Git 历史中

### 8.2 🔒 安全工作流程

#### 日常开发（始终在 main 分支）
```bash
# 1. 确保在 main 分支
git checkout main

# 2. 拉取最新的远程变更（仅同步，不合并代码）
git fetch origin

# 3. 正常开发、提交
git add .
git commit -m "feat(editor): xxx 功能"

# ⚠️ 注意：不要执行 git push origin main！
```

#### 同步公共内容到 GitHub（手动操作，谨慎执行）
```bash
# 1. 切换到 public 分支
git checkout public

# 2. 清理当前状态（清空源代码）
git reset --hard origin/public  # 回到远端最新状态

# 3. 只添加非敏感文件（README、截图、文档等）
git add README.md docs/screenshots/ .gitignore *.gradle.kts ...

# 4. 选择性 cherry-pick 公共提交（如 AGENTS.md 的公开部分、CHANGELOG 等）
git cherry-pick <commit-hash>  # 只选择非源代码的提交

# 5. 推送到远程（这是唯一允许的 push 操作）
git push origin public

# 6. 切回 main 分支继续开发
git checkout main
```

### 8.3 🧪 AI 开发时的 Git 检查清单

每次提交前，AI Agent **必须** 确认以下内容不在本次变更中：

- [ ] 没有 `.kt` / `.java` 源代码文件的增删改（除非明确知道这些是公开 API 接口）
- [ ] 没有 `local.properties` / `*.jks` / `keystore/` 等敏感文件
- [ ] 没有数据库 schema 导出文件（包含内部实现细节）
- [ ] 没有在 commit message 里暴露密钥或密码
- [ ] **确认当前分支是 `main`（本地私有）** → 只 `add` + `commit`，不要 `push`
- [ ] **如果要推送到 GitHub，必须先在 `public` 分支上验证不包含敏感内容**

### 8.4 📝 Commit Message 格式（Conventional Commits）

```
<type>(<scope>): <subject>

<body 可选，详细说明>

<footer 可选，Fixes #issue / BREAKING CHANGE:>
```
Type 列表：`feat`、`fix`、`docs`、`style`、`refactor`、`perf`、`test`、`chore`、`ci`

### 8.5 🔍 常见错误及修正方法

| 错误场景 | 解决方法 |
|---------|---------|
| 不小心 `git push origin main` | 立即在 GitHub 设置中删除该分支；检查是否有敏感信息泄露；考虑更换相关密钥 |
| public 分支混入了源代码 | `git reset --hard HEAD~N` 回退到上一个干净的提交；重新 cherry-pick 非代码提交 |
| 不确定某个文件是否应该公开 | 默认视为 **不可公开**；如有疑问，先问用户再决定是否添加到 public 分支 |
| 想同步新功能到 public 分支 | public 分支通常**不接受新功能代码**；只维护 README、文档、构建脚本等展示性内容 |

---

## 9. 版本号同步修改清单（🚫 发版时必须全部改）

发版（v1.x.y → v1.z.0）时，版本号集中管理于 `gradle.properties`（2026-08-29 按代码实测修正：`app/build.gradle.kts` 只负责计算 `versionCode = major*10000 + minor*100 + patch` 与 `versionName`，**不再手改**）：

| # | 文件路径 | 要改的字段 | 示例（1.0.30 → 1.1.0） |
|---|---------|-----------|---------------------|
| 1 | `gradle.properties` | `draftpeek.version.major` / `.minor` / `.patch` 三行 | `0` → `1`（minor），`patch` 归 `0` |
| 1' | （推荐替代） | `./gradlew bumpVersion -Pbump=minor`（自动回写上面三行并打印新版本） | — |
| 2 | `CHANGELOG.md` 顶部标题 | `## [1.x.y] - YYYY-MM-DD` | `## [1.0.30]` → `## [1.1.0] - 2026-xx-xx` |

> Git Tag 命名必须和 versionName **完全一致**：`git tag -a v1.1.0 -m "Release v1.1.0"`

---

## 10. CI/CD Workflow 说明（自动化流程）

GitHub Actions 配置文件存 `.github/workflows/`，共 2 个：

### 10.1 `android.yml` — PR / Push 自动 CI
**触发时机**：
- `push` 到 `main` / `develop` 分支
- 任何 `pull_request`（target 任意分支）

**Jobs 清单**：
| Job 名 | 作用 | 依赖 |
|--------|------|------|
| `lint-check` | 跑 Lint + Spotless Check | JDK 17 setup |
| `unit-tests` | `:app:testDebugUnitTest` 跑所有 JVM 单元测试 | 依赖 lint-check 通过 |
| `assemble-debug` | `:app:assembleDebug` 编译 Debug APK，上传 artifact（app-debug.apk，保留 7 天） | 依赖 unit-tests 通过 |

### 10.2 `release.yml` — 手动触发 Release 构建
**触发时机**：手动在 GitHub Actions 页面点 `Run workflow`（选择分支后运行）

**Jobs 清单**：
| Job 名 | 作用 | 说明 |
|--------|------|------|
| `assemble-release` | `:app:assembleRelease` + `:app:bundleRelease`，同时出 APK + AAB | 签名配置从 GitHub Secrets 读（`SIGNING_STORE_FILE_BASE64` / `SIGNING_STORE_PASSWORD` / `SIGNING_KEY_ALIAS` / `SIGNING_KEY_PASSWORD`），本地开发不需要配 |
| `upload-artifacts` | 把 APK + AAB 上传到 GitHub Release | 需要手动在 Release 页补 CHANGELOG |

---

## 11. 典型 AI 开发场景 SOP（照着做，少踩坑）

<!-- 📥 新SOP追加模板（AI 完成新类型任务后复制填好追加到这里）：
#### SOP-X: [场景名称]
**适用条件**：什么情况下走这个流程
**步骤**：
1. 第一步...
2. 第二步...
3. 第三步...
**验证**：怎么确认操作成功
**关联文件**：
- path/to/file1.kt
- path/to/file2.kt
-->

#### SOP-1: 新增一种文件类型（.md / .txt / 自定义格式）
1. 在 `core-model` 下加枚举 `DocumentType.XXX`
2. `DocumentRepository` 里加 Type → 对应 Parser 的映射
3. 加 Parser class 到 `third_party/markwon-parser`（或对应目录），**不能直接放 feature-editor**
4. `feature-settings` 加「默认打开方式」偏好项
5. 补单元测试：`DocumentTypeTest` + Parser 解析测试

**关联文件**：
- `core-model/src/main/.../DocumentType.kt`
- `core-data/src/main/.../DocumentRepository.kt`
- `third_party/markwon-parser/src/main/.../`

#### SOP-2: 新增一项设置项（Settings Screen）
1. 在 `core-model` 下加 sealed class `SettingItem.XXX`
2. `SettingsRepository` 里加对应 DataStore key + read/write 方法
3. `feature-settings` 下的 `SettingsScreen.kt` 加 UI 项（必须走 `BrandSwitch` / `BrandListItem`）
4. **如果影响安全**（比如开关加密）→ 必须同时更新第 5 节安全注意事项说明

#### SOP-3: 修改 Room Database Schema（加表 / 加列 / 改类型）
1. Entity class 改完后，在 `@Database` 注解 `version += 1`
2. **必须写 Migration class**（严禁用 `fallbackToDestructiveMigration`，生产 DB 有用户数据！）
   ```kotlin
   val MIGRATION_3_4 = object : Migration(3, 4) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE documents ADD COLUMN starred INTEGER NOT NULL DEFAULT 0")
       }
   }
   ```
3. `Room.databaseBuilder().addMigrations(MIGRATION_3_4)` 注册
4. **必须写 AndroidTest**：`MigrationTestHelper` 跑 schema 校验，防止 Migration 写错
5. 更新第 12 节 Known Gotchas（如果踩了新坑）

#### SOP-4: 新增一个 Compose 页面（新 Screen）
1. `feature-xxx` 模块下建 `XxxScreen.kt`，参数为 `UiState` + lambda callbacks（State Hoisting）
2. 加 `@Preview` 浅色 + 深色两个预览
3. `XxxViewModel` 加 `UiState` data class + `sendEvent(XxxEvent)` 事件入口
4. `NavigationGraph.kt`（在 app 模块）注册路由：`composable("xxx") { XxxRoute(viewModel = hiltViewModel()) }`
5. UI 组件 **全部走 Brand 设计系统**（BrandText / BrandCard / BrandScaffold），禁止直接 Material3

#### SOP-5: 写一个 Room DAO 的单元测试
1. **必须用 AndroidTest（不是 JVM 本地测试！）**，因为 SQLCipher 只有真机/模拟器环境下才能正确加载 .so
2. Room.inMemoryDatabaseBuilder()，密码传固定测试值（不要写死 `"password"`，用 `testDBPassword()` 工具方法）
3. `@Before` setup 建 DAO，`@After` close DB
4. 每个测试方法用 `runTest { ... }`（协程），断言用 Turbine 测返回的 Flow

---

## 12. 常见陷阱（Known Gotchas）— 踩坑血泪史，每条都付过学费

<!-- 📥 新坑追加模板（AI 踩坑后复制填好追加到表格最后）：
| # | 坑点标题 | 触发场景 | 现象/报错 | 正确做法 | 首次发现日期 |
|---|---------|---------|---------|---------|------------|
| X | 简短标题 | 什么操作会触发 | 具体报错信息或现象 | 正确代码/配置/步骤 | YYYY-MM-DD |
-->

| # | 坑点标题 | 触发场景 | 现象/报错 | 正确做法 | 首次发现日期 |
|---|---------|---------|---------|---------|------------|
| 1 | **R8 Full Mode 绝对不能开** | `proguard-rules.pro` 里 `-dontobfuscate` 或 build.gradle `isMinifyEnabled = true` + `isShrinkResources = true` 且 R8 full-mode 开启 | Apache POI（docx/xlsx 解析）、JGit、CommonMark 反射全部炸，ClassNotFoundException `org.apache.poi.ss.usermodel.Workbook` | 用 R8 非 full-mode（AGP 8 默认），且为每个反射库写明确 `-keep` 规则（见第 13 节），禁止用 `-dontwarn` 敷衍 | 2026-03-15 |
| 2 | **Markwon 4.6.2 已完全移除** | 在 feature-editor 想 import `io.noties.markwon.*` 或 `implementation("io.noties.markwon:core:4.6.2")` | ClassCastException / 找不到符号 | 用 **CommonMark 4.0** 解析 AST + 自研 Compose Render（`third_party/markwon-parser`），标题锚点用 `commonmark-ext-heading-anchor`，列表缩进用 flexmark-java 辅助 | 2026-03-20 |
| 3 | **Myers Diff 算法不要自己写** | 比较两份文稿 diff 时手搓 Myers O(ND) | 超大文件（1w+ 行）比较卡顿 2-5 秒，栈溢出 | 直接用 **java-diff-utils 4.12**（`com.github.java-diff-utils:java-diff-utils:4.12`），性能是自研的 8-10 倍 | 2026-04-02 |
| 4 | **SoraEditor 必须用 Application 级 Context** | 用 Activity Context 初始化 `Editor.getInstance(activity)` | 内存泄漏 15-20MB / 次页面打开，旋转屏幕后崩溃 "Editor instance already destroyed" | `Editor.getInstance(applicationContext)`，Application.onCreate() 里全局初始化一次，后续全用单例 | 2026-04-10 |
| 5 | **PF4J 插件框架 Android 上用不了** | 试图集成 PF4J 实现第三方插件加载 | `java.lang.NoClassDefFoundError: Failed resolution of: Ljava/nio/file/FileSystems;`（Android N 以前没有完整 java.nio） + 动态加载 DEX 安全问题 | 暂时弃用插件化方案，所有编辑器功能在 `feature-editor` 内直接实现；如果以后需要插件，用 Android App Bundles dynamic feature | 2026-04-18 |
| 6 | **SQLCipher + AGP 8.7 NDK 打包问题** | `app/build.gradle.kts` 里不写 `ndk { abiFilters("armeabi-v7a", "arm64-v8a", "x86_64") }` 直接跑 release | release APK 启动崩溃 `java.lang.UnsatisfiedLinkError: couldn't find "libsqlcipher.so"` | abiFilters 明确写 3 个 ABI；同时 `packagingOptions { jniLibs { useLegacyPackaging = true } }`（SQLCipher 4.5.x 对压缩 lib 的 ROM 兼容性问题） | 2026-04-25 |
| 7 | **Room Migration 禁止 fallbackToDestructiveMigration** | 数据库 Schema 变了，图省事 `fallbackToDestructiveMigration()` 删表重建 | **用户所有文档丢失**，商店大量差评 | 老老实实写 Migration class（见 SOP-3），哪怕只是加一列；同时写 AndroidTest 验证 | 2026-05-02 |
| 8 | **Compose BOM 版本锁定** | build.gradle.kts 手写 `androidx.compose.material3:material3:1.3.0`，不走 BOM | Compose Runtime / Compiler / Material3 版本不兼容，编译期 `IllegalStateException: Expected a function call node` | **全部 Compose 依赖都走 Compose BOM**：`implementation(platform(libs.compose.bom))` 然后不加版本号写 `implementation(libs.androidx.compose.material3)` | 2026-05-15 |
| 9 | **Tink 初始化时机必须在 Application.onCreate 最前面** | 想懒加载 Tink，第一次加密文档时再初始化 | ContentProvider（Room / WorkManager）在 Application.onCreate 之前就启动，需要加密 DB 密钥时 Tink 还没 init，崩溃 | Application.onCreate() **第一行**就是 `Tink.init(this)` + 主密钥集加载，放 Hilt ContentProvider 和其他初始化代码前面 | 2026-05-22 |
| 10 | **DataStore 多进程问题** | 用 WorkManager 后台导文档，Worker 进程也写 EncryptedDataStore | 概率性 `IllegalStateException: two DataStore instances are writing to the same file`，配置丢失 | 所有 DataStore 读写加 `@Singleton` scope + 进程判断（`if (isMainProcess(context))`）；Worker 进程直接写 SQLite / 文件，不要碰 DataStore | 2026-06-01 |
| 11 | **Jetpack Navigation Compose 不要传大对象** | `navController.navigate("edit/${documentJson}")` 把整个文档 JSON 塞路由参数 | `TransactionTooLargeException`（Binder 缓冲区 1MB 超限），文档大时崩溃 | ID 传路由：`navigate("edit/${documentId}")`，目标页面 `DocumentRepository.getById(id)` 读；跨页面共享大对象用 Hilt `@ViewModelScoped` SharedViewModel | 2026-06-10 |
| 12 | **Compose Preview 不能用 Hilt 依赖的 Composable** | `@Preview` 注解的函数直接用 `hiltViewModel<HomeViewModel>()` 或调用了 BrandScaffold（依赖 Theme） | Preview 渲染失败：`"Hilt ViewModel factory not found for class HomeViewModel"` | Preview 单独写一个无参版本：`@Composable @Preview private fun HomeScreenPreview() { DraftPeekTheme { HomeScreen(HomeUiState.Loading, onFabClick = {}) } }`，mock 所有参数 | 2026-06-18 |
| 13 | **Baseline Profile 生成器 AGP 8.7.x 崩溃** | 按 Android Studio 指引加 `androidx.profileinstaller:profileinstaller:1.4.0` + `baselineprofile` 模块 | `./gradlew :app:generateBaselineProfile` 编译期崩溃 `NoSuchMethodError: com.android.build.gradle.tasks.GenerateBaselineProfile.<init>(...)` | 此问题为 **AGP 8.7.3 已知 Bug**，Google IssueTracker 状态 Accepted 未修复；Baseline Profile 先禁用，等 AGP 8.8 再开 | 2026-06-25 |
| 14 | **Sora Editor JNI 多 ABI 打包** | CMakeLists.txt 只编译 arm64-v8a 一个 ABI，没有 `ndk.abiFilters` 全 3 个 | 小米 32 位机型 / Android 模拟器（x86_64）启动 Editor 立即崩溃 `UnsatisfiedLinkError: libsora-editor.so` | `build.gradle.kts` `ndk { abiFilters("armeabi-v7a", "arm64-v8a", "x86_64") }` 三个都写，CMake 自动为每个 ABI 出一份 .so；同时 `packagingOptions { jniLibs.pickFirsts.add("lib/*/libc++_shared.so") }` | 2026-07-02 |
| 15 | **JGit 大仓库 GC 后 GC_LOG 锁阻塞** | 调用 `git.gc().call()` 或大仓库 pull 后自动 GC | WorkManager Worker 卡住 10+ 分钟，ANR 被系统杀 | GC 操作不阻塞 UI 线程已经做到了，但是 **GC 时间必须加 timeout**：`git.gc().setTimeout(30).call()`，超时后抛出取消，下次启动再做 | 2026-07-10 |
| 16 | **Amazon Corretto JDK 17 是 Project JDK 唯一选择** | 用 Oracle JDK 17 / OpenJDK 21 跑 `./gradlew assembleRelease` | 10% 概率出现 `IllegalAccessError: class jdk.internal.org.objectweb.asm ...`（R8 class reader 问题），或 Kotlin 2.0.x K2 编译器偶发 OOM | Project SDK 固定 Amazon Corretto 17，`.gradle/jdks/` 自动装也行，Gradle JDK 必须和 Project JDK 一致 | 2026-07-15 |
| 17 | **Hilt 模块不允许放 feature 模块的 test 目录** | 想在 feature-home 测试模块写 `@TestInstallIn` 替换 Repository | Dagger Hilt 编译报错 `Cannot find @HiltAndroidTest root in test classpath` | **测试 Hilt 模块一律放 `app/src/test/`**（AndroidTest 放 `app/src/androidTest/`），feature 模块的 test 只测纯 Kotlin 类（VM、Parser、Util） | 2026-07-22 |
| 18 | **`local.properties` 必须有 `sdk.dir` 但禁止提交** | 新环境 clone 下来直接 `./gradlew assembleDebug` | `SDK location not found. Define sdk.dir in local.properties` | `local.properties.example` 是模板；开发机第一次 clone 后复制成 `local.properties` 填 SDK 路径；**`local.properties` 本身必须在 .gitignore** | 2026-07-28 |
| 19 | **WorkManager PeriodicWorkRequest 最小间隔 15 分钟** | 想做 1 分钟一次的文档自动备份 | `IllegalArgumentException: Interval must be >= 15 minutes for periodic work` | Periodic 就 15 分钟（或更大）；小于 15 分钟的定时任务用 `OneTimeWorkRequest` + `setInitialDelay` 链式调度，或 Room + AlarmManager | 2026-08-01 |
| 20 | **Compose `LazyColumn` 的 `key` 不能用文档 title** | `items(docs, key = { it.title })` | 用户改文档标题 / 两个文档同名时，LazyColumn item 复用错位（编辑内容串了 / 展开状态乱了） | **key 永远用 Room 主键 `Document.id`**（Long 全局唯一）：`items(docs, key = { it.id })` | 2026-08-03 |
| 21 | **Brand 设计系统新增组件必须人工 review** | 直接在 `core-ui` 新增 `BrandBadge.kt` 没让设计师看 | 和现有视觉风格不统一（圆角/颜色/阴影不对），上线后 UI 不一致返工 | `core-ui` 变更必须手动 PR，设计师 review 通过才能合 main；AI 可以写实现但**不能跳过 review** | 2026-08-05 |
| 22 | **Turbine `test` 必须手动 `expectNoEvents()`** | 只 `awaitItem()` 断言了一个 item 就 return | Flow 有新 event 没断言到，测试漏覆盖，后续 bug 回归测试没发现 | Turbine `test { awaitItem() → xxx; expectNoEvents() }` 最后必须加 `expectNoEvents()`，确保 Flow 没有意外的后续事件 | 2026-08-08 |
| 23 | **JUnit5 注解不能与 Robolectric `@RunWith` 混用** | 在 unit test 里用 `@RunWith(RobolectricTestRunner)` + JUnit5 的 `@Nested`/`@DisplayName`/`org.junit.jupiter.api.Test` | 跑 `:app:testDebugUnitTest` 报 `InvalidTestClassError`；或 JVM 下 `android.os.Build.BRAND` 为 null 导致 NPE | 需要 `Build.*` 有值的测试统一用 **JUnit4 风格**：`@RunWith(RobolectricTestRunner)` + `org.junit.Test@Test` + 扁平方法（禁 `@Nested`/`@DisplayName`）；纯逻辑测试用 JUnit5。见 `AntiDebugTest`/`RouteTest`（2026-08-26 修复范式） | 2026-08-26 |
| 24 | **Room 加表必须同时更新 schema 迁移 + AndroidTest 校验** | 新增 `links` 双向链接表升 DB version 11→12，只改 Entity/@Database/`MIGRATION_11_12` 却漏注册到 `DataModule.addMigrations` | 升级到 v12 时 `IllegalStateException: Room cannot verify the data integrity` 或 `no migration path 11→12` | Entity + @Database version+1 + MIGRATION + **在 `DataModule.provideAppDatabase().addMigrations(...)` 列表加新迁移**，缺一不可；schema 文件会自动导出到 `core/data/schemas/` | 2026-08-26 |
| 25 | **main/public 双分支绝对禁止混淆** | AI 在 `git push` 前没有检查当前分支状态；或在 public 分支提交源代码 | ❌ **源代码泄露到 GitHub 仓库**；❌ 敏感密钥被公开 | **日常开发只在 main 分支** → 只做 `add` + `commit`，不要 `push`；**推送到远程前必须先 `git checkout public`** → 验证不包含 `.kt/.java/src/` → `git push origin public` 是唯一允许的 push 操作；详见第 8 节 | 2026-08-26 |
| 26 | **Android 11+ 包可见性：`resolveActivity()` 恒为 null** | 真机安装后在「我的/关于」页点社交链接跳转外部 App（`ProfileScreen.openSocialUrl` 先 `intent.resolveActivity(pm)` 预判再 startActivity），而 `app/src/main/AndroidManifest.xml` 缺少 `<queries>` 声明（注释写了要声明但块被删了） | 手机已装浏览器/对应 App 仍提示「无法打开该应用」（`profile_social_open_failed`），全部策略 1-3 静默失败；单测/lint/低版本模拟器均测不出 | ① manifest 必须声明 `<queries>`（VIEW+http/https intent + 需显式跳转的第三方 `<package>`）；② **不要把 `resolveActivity` 当作能否打开的判据**，直接 `startActivity()` 并捕获 `ActivityNotFoundException` 让系统真正解析；二者缺一不可 | 2026-08-29 |

---

## 13. R8 / ProGuard 混淆规则专项（`proguard-rules.pro`）

> 2026-03 起默认 Release 编译。每加一个带反射 / 注解 / JNI 的第三方库，**必须同步更新混淆规则**，并**写清楚「为什么 keep 这个类」**，禁止只加 `-dontwarn` 不管原因。

```proguard
# ===== R8 全局配置 =====
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisible*Annotations, *Annotation*
-dontwarn org.jetbrains.annotations.**

# ===== Room (反射生成 DAO 实现) =====
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.draftpeek.core.data.entity.** { *; }   # 为什么 keep：Room 反射用（与 app/proguard-rules.pro 实际规则一致）
-keep class * extends androidx.room.RoomDatabase { @androidx.room.Database *; }

# ===== SQLCipher (JNI 加载) =====
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# ===== Tink (JNI + 密钥集序列化) =====
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.subtle.**

# ===== JGit (反射 + ServiceLoader) =====
-keep class org.eclipse.jgit.** { *; }
-keep interface org.eclipse.jgit.transport.** { *; }
-keep class org.eclipse.jgit.storage.file.WindowCacheConfig { <init>(); }
-keepnames class org.eclipse.jgit.internal.storage.file.WindowCache
-keep class com.jcraft.jsch.** { *; }   # 为什么 keep：SSH  Transport 反射调用
-dontwarn org.eclipse.jgit.ssh.jsch.**
-dontwarn com.jcraft.jsch.**

# ===== Apache POI (docx/xlsx 预览，大量反射 + XML 反序列化) =====
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.poi.ooxml.**
-dontwarn org.openxmlformats.schemas.**

# ===== CommonMark + Flexmark (Markdown 解析) =====
-keep class org.commonmark.** { *; }
-keep class com.vladsch.flexmark.** { *; }

# ===== Sora Editor (JNI 接口不能混淆) =====
-keep class io.github.rockerhieu.emojicon.** { *; }
-keep class io.github.rosemoe.sora.** { *; }
-keepclasseswithmembernames class io.github.rosemoe.sora.** { native <methods>; }

# ===== Hilt / Dagger (生成代码 + 反射实例化) =====
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep class * extends dagger.hilt.android.components.ApplicationComponent
-keepnames class androidx.hilt.lifecycle.ViewModelInject
```

---

## 📋 自进化修订记录表（AGENTS.md 进化史）

| 自进化版本 | 日期 | 触发原因 | 更新内容摘要 | 对应项目版本 | 已校验 |
|:---------:|------|---------|------------|:------------:|:-----:|
| v1.0 | 2026-08-10 | 初始建立自进化协议 | 从 DraftPeek 项目健康度评估报告的建议补齐 AGENTS.md，建立自进化协议（5 条铁律 + 自检清单）+ 版本同步修改清单 + CI/CD Workflow section + R8 混淆规则专项 + 22 条 Known Gotchas + 5 个 SOP | v1.2.0  | — |
| v1.1 | 2026-08-26 | 编辑器标签拖拽重排 + 双向链接落地；修复存量测试 | 新增 Known Gotchas #23（JUnit5 与 Robolectric `@RunWith` 混用）、#24（Room 加表须同步 schema 迁移 + DataModule 注册），实现 `TabManager.reorderTab` + TabBar 拖拽 / `links` 表 + `MarkdownLinkParser`（被 AGENTS.md 自进化铁律 #2 记录） | v1.2.0  | — |
| **v1.2** | **2026-08-26** | **双分支安全策略澄清** | **新增第 8 节完整双分支隔离策略（main/public）详解；新增 Gotcha #25 防止源代码泄露；重写安全工作流程和常见错误修正方法；更新修订记录表** | v1.2.0  | — |

| v1.2 | 2026-08-27 | **家族规范完整性审计（Phase B · B4）：自进化协议打补丁（第 6 条铁律 + 修订表已校验列）** | ① 新增第 6 条铁律「证据绑定（Evidence Binding）」：可执行路径必须当时可验证存在、未实现项须显式标注、禁止虚构 CI 门禁；② 自检清单追加两项：路径真实存在校验（跑 `python scripts/check_spec_refs.py`）与 pre-commit 双向一致校验；③ 修订记录表增加「已校验」列，历史行统一填 `—`（未校验），新条目须填 `✓ (check_spec_refs)` 或 `✗`；④ 本仓新增 `scripts/check_spec_refs.py` 家族审计 wrapper 与 `.github/workflows/docs-consistency.yml`（本地/含审计器环境强校验，纯 CI 环境找不到审计器时降级跳过保持绿）。本行即首个填写「已校验」的条目 | v1.2.0| ✓ (check_spec_refs) |
| v1.3 | 2026-08-27 | **家族规范治理 Phase C/D/E 落地（一致性·补齐·账本）** | C0 未入库 docs 链接标注；D1 §0 仲裁节；D2 docs/adr/ 架构决策记录（README 提交，ADR 内容受 .gitignore *.md 限制为本地文档）；D3 FILEMAP+同步脚本；D4 禁区章节；D7 LOCAL_RULES.md 变更隔离；D8 安全审计报告。注：多数治理 .md 受 .gitignore:128 *.md 限制为本地文档，属用户 C0 有意策略 | v1.2.0 | ✓ (check_spec_refs) |
| v1.4 | 2026-08-29 | **真机社交链接「无法打开该应用」修复** | 新增 Gotcha #26（Android 11+ 包可见性致 `resolveActivity()` 恒 null）；修复：`app/src/main/AndroidManifest.xml` 补 `<queries>`（VIEW+http/https + bili/douyin/xhs/kuaishou 包名，属禁区变更待人工 review）+ `ProfileScreen.openSocialUrl` 改为直接 `startActivity` 捕获 `ActivityNotFoundException` | v1.2.0 | ✓ (check_spec_refs) |
| v1.5 | 2026-08-29 | **铁律 #1 事实同步：包名/版本过期修正** | 按代码实测（`app/build.gradle.kts` + `gradle.properties` + merged manifest + CHANGELOG）修正：§1 ApplicationId `net.apricotforest.draftpeek`→`com.draftpeek`（含 .dev/.staging 后缀）、当前版本 v1.2.0(10200)→v1.0.30(10030)；§9 版本清单改为 gradle.properties 集中管理 + `bumpVersion` task（build.gradle.kts 仅计算不再手改）；§2.4 导入示例与 §13 R8 示例块包名同步（实际 `app/proguard-rules.pro` 本无旧包名，仅文档示例过期）；新增 `docs/plans/android-emulator-testing-guide.md`（模拟器测试工作流，移植自 SpiritPal 并按本工程适配） | v1.0.30 | ✓ (check_spec_refs) |

<!-- 🔄 下次更新 AGENTS.md 时，在上面表格末尾追加新一行，不要删除历史记录 -->

## 📂 文件归档与放置规范（重要：新增文件必须遵守）

> 本仓库目录已于 2026-08-23 系统整理（见 `docs/整理记录_20260823.md`（本地文档，未随仓库发布））。后续任何新增/生成文件，**先判断类型再放置**，不要随意丢在仓库根目录或其他位置。

**docs/ 分类（项目文档）**
- `docs/project/`：需求(PRD)、架构、API、技术选型、设计上下文
- `docs/plans/`：实施计划、路线图、指南(Guide)、待办(TASKS)
- `docs/reports/`：评估/审计/安全/测试/优化报告、Lessons
- `docs/repo-analysis/`：仓库学习报告（命名 `{仓库名}_技术学习报告.md`）
- `docs/_devarchive/`：历史/一次性开发产物、交接方案、旧版本文档（**归档而非删除**）

**根目录只允许放置**
- 标准仓库文件：README、CHANGELOG、AGENTS、SECURITY、PRIVACY_POLICY、THIRD_PARTY_NOTICES、LICENSE
- 构建与配置：build.gradle.kts、settings.gradle.kts、gradle.properties、gradlew(.bat)、package.json/lock、codecov.yml、local.properties(.example)、.editorconfig、.env.example、.gitignore、index.html（设计原型）
- 明确被 build/CI 或文档要求从根目录运行的工具

**禁止事项（防止回归混乱）**
- ❌ 一次性图标/调试脚本、预览 HTML、build 日志 → 放 `docs/_devarchive/icons/` 或 `scripts/`，**绝不再堆在根目录**（本次已清理 28 个）
- ❌ 文档散落到 app/tests/feature 等业务目录 → 归入 `docs/` 对应分类
- ❌ 移动/删除 gitignored 运行时产物（`.gradle`、`build/`、`local.properties` 等）
- ❌ 删除旧版本文档 → 需要留档移入 `docs/_devarchive/`

> 本仓库特别说明：**`release.jks` 是 App 签名密钥，敏感，严禁删除/外传/提交**，保留根目录仅供本地构建；
> 图标相关一次性脚本统一入 `docs/_devarchive/icons/`。
> 新增文件前若不确定归属，先询问，不要自作主张放置。

## 远程推送与同步规则（2026-08-27）

- **main 为私有开发主线**：本地开发与提交后，推送到 `private` 远程（`git push`，默认上游即 private/main）。
- **public 分支为公开演示内容**：仅在有公开需求时维护，推送用 `git push origin public`。目前无更新计划。
- **禁止静默直写远程**：任何通过 GitHub API / 网页端直接修改远程的操作，执行前必须向用户说明，执行后必须检查本地与远程差异并同步。
- **禁止动未提交改动**：用户本地存在未提交修改时，不得擅自 commit / push / stash / 覆盖，必须先征得用户同意。
- **删除即高危**：删除文件或分支必须经过用户明确审核。

## push 前预检铁律（2026-08-28）

- 提交并推送前必须通过本地预检：直接 `git push`（pre-push hook 自动执行 precheck.ps1），或手动 `powershell -File precheck.ps1` 全绿后推送。
- 预检失败时**修复代码**，而不是跳过检查；`--no-verify` 仅限用户明确要求时使用。
- 改动业务代码后需跑一次 `-Full`（含测试与覆盖率门禁）再推送。
- 预检脚本与 hook 均为本地文件（不入库），勿删除。

## CI 流程铁律（2026-08-28 反方审稿采纳）

1. **推送闭环**：push ≠ 完成。push 后必须用 `gh run list` / `gh run watch` 盯 CI 到终态并回报结果；红了**当场自己修**（刚推送的上下文最全），跑不完或修不动立即回报而不是留到明天。
2. **修复交接**：CI 红时先读 `FIX_LOG.md`；动手修必须追加一行：**失败签名（关键报错行）→ 假设 → 动作 → 结果**。同一失败签名第二次出现，禁止再试同方向，必须读完整失败日志或 revert 换策略。同一仓库同一时刻只允许一个修复者。
3. **红灯止损**：main 红后限时 30-60 分钟拿不出明确根因 → `git revert` 回到 last green，恢复 main 绿色后再从容修（配合 FIX_LOG.md）。**revert 是止损，不是失败。**
