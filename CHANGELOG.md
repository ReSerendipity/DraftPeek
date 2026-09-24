# Changelog

本项目所有重要变更均记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

---

## [Unreleased] (开发中)

## [1.0.32] - 2026-09-24

> 本版收录 `v1.0.31`（tag `74e4a8a`）之后的 14 个 PR。`v1.0.31` 的 draft Release 从未发布，
> 因为它构建自的 tag 树不含下面第一条（无 WebView 设备的预览降级）——用户拍板不发旧包，改切本版。

### Added
- `app` 的 release 清单声明 `<profileable android:shell="true"/>`，让 Macrobenchmark 能在非 debuggable 的 release 包上读帧（仅声明、不改运行行为，API < 29 平台忽略）（#98）
- CI 门禁 `server` 依赖锁可解析性检查（blocking），用于把"锁文件写坏"挡在合并前；`security.yml`（#94）
- GPG Release 分离签名链路与公开验证密钥 `release-signing-public-key.asc`（`gpg-signed-release.yml` 在 `GPG_PRIVATE_KEY` 未配置时以 notice 跳过，不影响发版）（#89、#84）
- 版本一致性本地预检 `precheck.ps1` 与其测试覆盖 `tests/test_version_consistency.py`（#84）

### Changed
- 发布链路顺序调整：`release.yml` 先产出 draft Release（APK + AAB + `.sha256` + Release 正文），再跑 Macrobenchmark。此前 benchmark 排在创建 Release 之前，它一红就连带跳过 checksums 与 draft 生成，等于性能测试卡住发版（#88）
- Macrobenchmark 用例不再依赖 Hilt：`FileReadBenchmark` 去掉 `@HiltAndroidTest` / `HiltAndroidRule` / `@Inject`，改为直接构造 `EditorFileRepositoryImpl`；`:benchmark` 相应摘除 `hilt-android-testing` 与失活的 multidex 并重建 `gradle.lockfile`（#98）
- `docs/RELEASE_CHECKLIST.md` §2 改口径：把"release.yml 全链路绿（含 macrobenchmark）"拆成"建 Release 之前全绿"为真门禁，benchmark 明确标注当前不是门禁（#91）
- Dependabot：`/server` 的 minor-and-patch 组 3 项依赖升级（#92）；全量 `pip-audit` 从合并门禁降为非阻塞提示，避免上游 advisory 阻塞与锁无关的 PR（#97）

### Fixed
- 编辑器 Markdown 预览在**没有 WebView provider 的设备**上不再一点开就崩：检测 provider 不可用时降级为原生预览并提示「当前设备没有可用的 WebView，已切换为原生预览（公式 / 流程图 / 代码高亮暂不可用）」，新增 `editor_preview_no_webview` 文案并同步 zh / zh-TW / en / ja / ko 五个 locale；同时补上 WebView 渲染进程崩溃后的恢复路径（`onRenderProcessGone` 后复位状态并允许重渲染）（#80，#98）
- `release.yml` 按 flavor 取构建产物路径（有 `productFlavors` 后 `app/build/outputs/apk/release/*.apk` 这一层不存在，产物上传与校验都取不到文件），并新增 `workflow_dispatch(tag)` 逃生口，使 workflow 自身修复不必再靠挪 tag（#83）
- `/dev/kvm` 权限恢复补齐到 `android.yml` 的 Macrobenchmark job 与 `release.yml` 的 benchmark 步骤（此前只写在 `.github/actions/instrumented-tests` 里）。缺失时模拟器退回软渲染、轮询 `sys.boot_completed` 到超时并以 exit 224 收场，长期被当成随机抖动；`release.yml` 侧改为内联执行，因其按 tag 树检出、无法引用该 tag 里尚不存在的本地 action（#85、#86）
- `release.yml` 的 benchmark `script:` 压成单行自闭合命令：`android-emulator-runner` 会把 `script:` 逐行交给 `/usr/bin/sh -c`，跨行 `if/fi` 被拆进两次调用后报 `Syntax error: end of file unexpected` 并以 exit 2 收场，脚本从未真正执行过安装（#90）
- `server` 依赖锁中 `pydantic` 与 `pydantic-core` 的配对被分组升级拆坏，并在 `.github/dependabot.yml` 增加忽略规则防止复发（#93）
- 夜间档 Macrobenchmark 的 job 从未安装被测应用：它只跑 `:app:assembleRelease`（只构建不安装），而 `:benchmark` 是自 instrument 的 library 模块、`connectedDebugAndroidTest` 只装测试 APK，因此 target 包 `com.draftpeek` 一直缺席，13 条用例成批报 `Unable to find target package com.draftpeek, is it installed?`；`android.yml` 里那句「被测应用由上一步 assembleRelease 安装」是假注释。现按 `release.yml` 已验证的单行写法在 `script:` 内安装 production release APK，并订正注释（#99）

## [1.0.31] - 2026-09-20

### Added
- 新增 `.mailmap` 历史身份归并与 `docs/` 治理执行总结（钩子四级回退 / 日志落点 / 约定速查），并在 README 增加「分发边界」小节，说明 `AGENTS.md` 家族为仅本地保留、有意不入库，缺少它属预期行为
- 新增 `README.md` 项目说明文档
- 新增 `local.properties.example` 配置模板
- 新增 `CHANGELOG.md` 更新日志
- 新增 `docs/README.md` 文档索引与目录结构速览（原列 `docs/ARCHITECTURE.md` 实际未创建，2026-09-17 从本清单移除并指正）
- 新增 `scripts/` 辅助脚本目录（build-debug、build-release、run-unit-tests、install-on-device、clean-project、package-release）
- 新增 `.env.example` 环境变量模板
- 清理根目录 40+ 临时日志文件
- 更新 `.gitignore` 排除临时日志文件
- 交叉补齐文档锚点（D2）：README 项目结构树补列 `core/crdt`、`core/sync`、`feature/knowledge`（标记未接入 settings.gradle）；模块依赖图对齐实际 `build.gradle.kts` 声明依赖；修正 `FILEMAP.md` 路径、`repos/`（计划未实现）、`scripts/coverage.sh`（不存在）与 `core-network`（无对应模块）等不一致

### Changed
- 删除根 `CONTRIBUTING.md`，社区健康文件交回组织级 `ReSerendipity/.github` 继承。PR #71 把它加回根目录后，main 的 `自净化检查（非阻塞）` job 持续 failure，只因该 job 非阻塞而 workflow 整体显示 success，长期无人发现
- 依赖坐标全面集中至 `gradle/libs.versions.toml`：清除 7 个模块 12 处硬编码版本，并对齐 `androidx.test.ext:junit`（benchmark 1.1.5 → 1.2.1）与 `androidx.test:runner`（benchmark 1.5.2 → 1.6.2）跨模块漂移
- 依赖锁定升级为 `LockMode.STRICT`：为全部 15 个项目生成 `gradle.lockfile`（此前仅 app 与 root 有锁，其余模块缺锁静默通过），新依赖绕过版本目录将直接构建失败
- README 环境要求与实际工具链对齐（AGP 8.8.0→8.10.1、Gradle 8.13→8.14.3、Android Studio Meerkat 2024.3.2+）
- `bumpVersion` 任务输出提示：tag 推送目标由 `origin` 改为 `private` 远程（原提示若照做会把私有 main 历史带入公开仓库）【注：2026-09 双仓双分支体系已废止、仓库全量公开，本提示与 `private` 远程不再适用】
- 数据库迁移策略 §5 紧急回滚改写：移除不存在的 Play Console 能力，按真实分发渠道（GitHub Release）重写止损步骤

### Security
- 收口 CodeQL error 级告警中的 4 处真实输入面（#73）：`sync_server` 的 `download_file` 原先调用 `_resolve_safe_path()` 却**丢弃返回值**、实际按 DB 里的 `storage_path` 读盘（元数据一旦被改写即可把读取路径带出存储目录），现改为只读校验后的 `safe_path`、DB 值仅作纯字符串核对；路径校验改逐段构造并拒绝控制字符；用户可控值经 `_for_log()` 剥离 CR/LF 与控制字符后再入日志；batch 的两处 `except Exception` 不再把 `str(e)` 回显客户端；`markdown-preview.html` 的 `file` 查询参数加协议 / host / `.md` 三重白名单；`TerminalService` 两处 PendingIntent 显式 `setPackage`。error 级 open 告警 **16 → 0**（14 条由重扫自动判 fixed，9 条按绑定理由记为 mitigated）
- 新建发布签名密钥并接通发布链路：仓库原 `release.jks` 经全量核查确认不可得（本机全盘无 `.jks`、`local.properties` 无 `RELEASE_*`、GitHub secrets 为空，`release.yml` 在缺 secret 时硬失败），故生成新密钥（RSA-3072 / PKCS12 / 有效期 30 年 / 别名 `draftpeek-release`，证书 SHA-256 指纹 `08:17:77:13:73:A8:95:6F:AB:91:46:0D:CB:E4:F7:46:00:52:8D:9C:E1:83:F2:CC:CC:17:70:6B:C2:B9:FA:64`）并配置 `RELEASE_STORE_FILE_BASE64` / `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD`。**自本版本起为新的签名身份**，此前若有以旧密钥签名的分发件，无法覆盖升级，需卸载重装
- 修复发版链路签名缺陷：`release.yml` keystore 解码路径与 `app/signing.gradle` 的 app/ 相对解析不一致（旧实现解码到仓库根，正式发版必然找不到密钥）
- CI 新增三处版本一致性硬门禁（`scripts/check_version_consistency.py`：tag == CHANGELOG == gradle.properties == README 声明 == versionCode 公式），并前移到 test/lint 之前 fail-fast
- APK 签名默认仅启用 v2+v3（关闭 minSdk 26 下冗余的 v1/JAR 签名）
- pre-push hook 新增红线硬拦截：拒绝向 `origin` 推送 `main`（任何方向），防止 2026-09-02 类误推事故复发
- 新增自托管下载页基座与版本更新清单契约（`docs/deploy-download-page/`），渠道决策待定

### Fixed
- 修复 nightly instrumented 的两层确定性失败（#72 / #75）：① `WysiwygRenderVerificationTest` 断言的 `test_tag_markdown_preview` / `test_tag_markdown_rich_editor` 在生产码里从未声明（全仓 `app|core|feature` 的 `src/main` 中 `testTag` 命中数为 0），该类 8 条用例自 2026-08-27 写下起从未通过；② `:feature:terminal` 声明了 `testInstrumentationRunner` 而 androidTest 类路径没有 `androidx.test:runner`，设备侧在 instrumentation 绑定阶段即 `ClassNotFoundException` 崩溃，gradle 中止后连带让 `:app` 永远排不到。判据用同 runner 的 A/B 对照：`feature_editor` 由 `tests=12 failures=8` 变 `failures=0`，main 全矩阵复验中 API 30 / 34 两档首次转绿
- 修复 CodeQL 慢性红灯（创建即红，自 `ci: add codeql analysis` 每跑必败）：manual build-mode 下 `codeql-config.yml` 的 `build-command` 字段不被 `github/codeql-action/analyze` 执行；且 `Cache Gradle wrapper` 步骤恢复的 build cache 让 `compile*Kotlin`/`compile*JavaWithJavac` 命中 `FROM-CACHE`，CodeQL tracer 抓不到源码 → 改为 workflow 内 `Initialize CodeQL` 与 `Perform CodeQL Analysis` 之间显式 `./gradlew assembleDebug --no-daemon --no-build-cache --no-configuration-cache --rerun-tasks`，run `34591254159` 三语言 job 全绿。详见 `docs/agents/GOTCHAS.md` #DP-01 与 `FIX_LOG.md`。（commit `356b289`）

## [1.0.30] - 2026-08-10

### Added
- 新增 `values-zh-rTW` 繁体中文资源（所有模块）
- 新增 `BookmarkDaoTest`、`SecurityEventDaoTest`、`SnippetDaoTest` 单元测试
- 新增 `core/data` 测试 DI 模块和 Repository 测试
- 新增 `core/domain` 测试目录
- 新增 `core/ui` 测试目录
- 新增 `StartupBenchmark` 启动基准测试

### Changed
- versionCode 29 → 30, versionName 1.0.29 → 1.0.30
- 更新启动器图标（前景图、单色图标）
- 优化 GitHub Actions CI 工作流

## [1.0.29] - 2026-07

### Added
- 集成 sora-editor 语法高亮引擎 (TextMate + Tree-sitter)
- Tree-sitter Java 语法支持 (sora-language-treesitter 0.24.6 + tree-sitter 4.3.2)
- 实验性 LSP 客户端 (Eclipse LSP4J 0.22.0)，支持 Python/Kotlin/JS/Java/Go/Dart
- Markdown 原生解析管线 (CommonMark 0.24.0 + GFM 扩展)
- Markdown WYSIWYG 编辑 (richeditor-compose 1.0.0-rc13)
- Office 文档解析 (Apache POI 5.3.0：Word/Excel/PPT → HTML → WebView)
- PDF 渲染 (Android PdfRenderer + android-pdf-viewer)
- Diff 引擎 (java-diff-utils 4.12 替换自研 Myers diff)
- Glance AppWidget 最近文件快捷访问
- 首次启动引导流程 (OnboardingActivity)
- 插件系统 (PF4J 3.11.0)

### Changed
- Markwon 移除，替换为 CommonMark 原生解析 + WebView 管线
- DiffEngine 使用 java-diff-utils 替换自研 Myers diff
- 安全-加密库 security-crypto 移除，替换为自建 SecureFileStorage + SecurePreferences
- SQLCipher 迁移至 net.zetetic:sqlcipher-android (4.6.0)
- R8 Full Mode 禁用 (兼容 Apache POI/JGit 反射链)

### Security
- Native C 反检测 (native_security.so：Frida/TracerPid/Zygisk 检测)
- Play Integrity API 设备完整性校验 (VULN-009)
- APK Signature Scheme v3 启用
- jsoup HTML 消毒 (VULN-004)
- 字符串加密 IR 插件 + 假类生成 (防逆向)

## [1.0.28] - 2026-06

### Added
- 整洁架构分层：core/* (common/data/designsystem/domain/testing/ui) + feature/* (browser/editor/settings/stats/terminal)
- build-logic Convention Plugins 统一构建配置
- Hilt 2.58 依赖注入
- Room 2.7.1 + SQLCipher 加密数据库
- 终端模块 (Proot 会话管理 + 终端模拟器)
- Git 集成 (GitHub 仓库导入 + JGit 操作)
- VFS 抽象层 (local/FTP/SFTP 文件系统)
- 可访问性设计 (色盲模式、高对比度、自适应布局)
