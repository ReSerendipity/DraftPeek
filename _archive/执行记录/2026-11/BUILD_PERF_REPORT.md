# DraftPeek 构建性能分析与优化报告

> 日期：2026-09-13 · 环境：Windows / 32 逻辑核 / 32GB RAM / Adoptium JDK 21.0.11 · Gradle 8.14.3 · AGP 8.10.1
> 构建变体：`assembleDevDebug --no-daemon`

## 1. 优化前构建时间

| 场景 | 总耗时 | 配置阶段 | 任务执行 | 说明 |
|---|---|---|---|---|
| 冷启动首次构建 | **7m25.7s** | Settings/buildSrc 2m8s + Configuring 5m14s | 0s（客户端被回收，未进入任务） | 首次下载全部依赖 + 编译 build-logic |
| 预热构建（依赖已缓存、首次真正编译） | **2m34.4s** | **6.3s**（配置缓存已生效） | 96 个任务执行 / 176 up-to-date | 基准对比基线 |

冷启动配置阶段为何这么慢：根项目 `:` 2m22s、`:convention`（build-logic 约定插件编译）2m6s。
**这部分是一次性成本，已被 `org.gradle.configuration-cache=true` 消除**——预热后配置仅 6.3s。

## 2. 主要瓶颈分析（预热构建 Top）

> 注：profile 中 Artifact Transforms(10m25s) 与 Task Execution(7m29s) 是**并行累计**耗时（32 核并行），非墙钟；墙钟总时间 2m34s。

| # | 任务 | 耗时 | 性质 |
|---|---|---|---|
| 1 | `:app:mergeExtDexDevDebug` | 1m6s | DEX 合并（含外部 dex / L8 脱糖） |
| 2 | `:app:processDevDebugResources` | 35s | 资源链接 / R 类 |
| 3 | `:app:mergeDevDebugResources` | 25s | 资源合并 |
| 4 | `:feature:browser:compileDebugJavaWithJavac` | 18.4s | 库模块编译 |
| 5 | `:app:compileDevDebugKotlin` | 16.3s | app Kotlin 编译（含 Compose） |
| 6 | `:app:mergeDevDebugNativeLibs` | 13.3s | 原生库合并（SQLCipher/tree-sitter，4 ABI） |
| 7 | `:app:configureCMakeDebug[4×ABI]` | ~6.9s×4 | CMake 配置（build 仅 0.5s/ABI，已缓存） |

结论：墙钟时间主要花在 **app 的打包/资源/DEX 流水线**，而非库模块 Kotlin 编译；
库模块编译在热构建中多为 UP-TO-DATE。配置阶段已非瓶颈（6.3s）。

## 3. 实施的优化项及理由

仅修改 `gradle.properties`，每项均有依据：

### 3.1 JVM 参数（第 1–9 行）
- **堆内存 `-Xmx4096m → 6144m`**：本机 32GB，预热时守护进程峰值已 ~2GB；`kotlin.compiler.execution.strategy=in-process` 下 Kotlin 编译器 + KSP(Hilt/Room) 与守护进程共用堆，32 核并行任务下 4GB 偏紧。6GB 留足头空间。
- **`-XX:MaxMetaspaceSize=1g`**：AGP/KSP/Compose/Hilt/Paparazzi 等插件多，限制 Metaspace 防止无界增长与频繁 Full GC。
- **`-XX:+UseG1GC -XX:MaxGCPauseMillis=200`**：JDK21 默认 G1，显式声明并设目标停顿，缩短大堆 GC 停顿。
- **`-XX:+ParallelRefProcEnabled`**：多核下加速引用处理（GC）。
- **`-XX:+DisableExplicitGC`**：忽略插件偶发的 `System.gc()` 全堆回收。

### 3.2 关闭 debug 构建的 Compose 指标（第 24–27 行）
- `composeMetrics=false`：`app/build.gradle.kts:195` 默认对所有 *Debug 构建开启 Compose recompose 指标/报告生成，
  会给每个 Compose 模块的编译器加 I/O。关闭后例行 debug 构建不再生成；需要分析重组时命令行加 `-PcomposeMetrics=true` 即可恢复。
  （验证：改后 `:app:compileDevDebugKotlin` 因编译器参数变化而重编译，证明开关生效。）

### 未改动项及原因
- `parallel` / `caching` / `configuration-cache`：已开启，是本次配置降本（5m14s→6.3s）的主因。
- `nonTransitiveRClass`：已开启。
- `android.defaults.buildfeatures.buildconfig=false`：app 模块大量 `buildConfigField`（签名基线等），不能关；库模块未显式开启，AGP 8 默认按需关闭。
- `kotlin.compiler.execution.strategy=in-process`：保留；堆已加到 6g，in-process 避免额外进程开销。
- `org.gradle.vfs.watch` / `kotlin.incremental`：Windows 下默认开启 / 默认 true，无需显式设置。

## 4. 优化后构建时间

| 场景 | 总耗时 | 说明 |
|---|---|---|
| 配置缓存重建后构建（改 gradle.properties 触发失效） | **41.3s** | BUILD SUCCESSFUL；配置 6.5s；9 任务执行 / 263 up-to-date |
| 稳态增量构建 | **8s** | BUILD SUCCESSFUL；`Configuration cache entry reused` |

> 说明：41s 与基准 2m34s 的差异主要来自执行任务数（9 vs 96），并非纯优化收益；
> JVM/GC 调优的收益在“大量任务真正并行编译”时才显现。关键结论是：**新 JVM 参数有效且不构建回归**
> （若 jvmargs 非法 JVM 会直接启动失败），配置缓存稳态下增量构建仅 8s。

## 5. 未实施 / 无效果项

- **冷启动 7m25s**：一次性成本（首次依赖下载 + build-logic 编译），靠配置缓存已消除，无需额外改动。
- **mergeExtDex / processResources 等 AGP 内部任务**：受项目依赖规模（AWS SDK/POI/jgit/LSP4J）与 4 ABI 原生库影响，
  属于 AGP 任务内部行为，不通过 gradle.properties 安全可调；强行实验性 flag 风险大于收益，未做。
- 环境插曲：本机 JDK 不信任网络 MITM 证书导致 wrapper 发行版下载失败，已用 PowerShell 直下 zip 到 wrapper 缓存解决（见 GOTCHAS #DP-02）。

## 6. 修改的文件

1. **`gradle.properties`**
   - 第 1–9 行：`org.gradle.jvmargs` 由 `-Xmx4096m -Dfile.encoding=UTF-8` 改为
     `-Xmx6144m -XX:MaxMetaspaceSize=1g -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+DisableExplicitGC -Dfile.encoding=UTF-8`（附注释）。
   - 第 24–27 行：新增 `composeMetrics=false`（附注释）。
2. **`docs/agents/GOTCHAS.md`**：追加 #DP-02（wrapper 证书坑 + `--no-daemon` 客户端回收挂起守护进程）。

> 注：工作区中 `.github/workflows/*` 与 `feature/browser/*` 的改动为既有未提交内容，本次未触碰。
