# 安全审计 — DraftPeek

> 只读审计 · 适配版（Android Compose/Kotlin + Gradle + Python CRDT 同步服务，私有仓库）
> 审计日期：2026-09-01 · 审计对象：`server/`（Python）+ `app/feature/core`（Kotlin）

## 执行摘要（总体评级：中 / Medium）

三仓中**服务端安全最扎实**的一档：`release.jks` 已 gitignore、签名凭据外置且 fail-fast（与 SpiritPal 形成对照），同步服务有路径穿越拦截、参数化 SQL、常量时间口令比对、文件魔数校验。主要风险是 **`integrity_server.py` 硬编码 `0.0.0.0` 且无强制鉴权**，以及 **Python 服务端依赖未 pin**。共 5 项发现（1 中高 / 1 中 / 1 低中 / 2 信息级良性）。

## 按维度发现

### 1. 凭据 / 密钥
- **良性（亮点）**：`release.jks` 存在磁盘（2766B）但 `.gitignore:61` 以 `*.jks` 忽略（已 `git check-ignore` 确认**未入库**，故 push 不泄露）；`app/signing.gradle:29-37` 从 gitignored 的 `local.properties` 读 `RELEASE_STORE_PASSWORD` 等，**无默认口令**、缺失即 fail-fast。与 SpiritPal S2 的 `spiritpal123` 默认口令形成鲜明对照。✅
- **良性**：`local.properties` 未被追踪（仅 `.example` 入库）。✅

### 2. 依赖供应链
- **[D2-Medium] Python 服务端依赖未 pin**：`server/requirements.txt:8-15` 全部仅 `>=`（如 `fastapi>=0.109.0`），**无 lock 文件** → 不可复现构建 + 供应链漂移/投毒风险。建议：用 `pip-compile` 生成 `server/requirements-lock.txt`（参考 MiniMax）。
- **良性**：`settings-gradle.lockfile` 存在，Gradle 依赖已锁。✅

### 3. 网络暴露
- **[D1-Medium/High] integrity_server 硬编码全网卡**：`server/integrity_server.py:202` `uvicorn.run(app, host="0.0.0.0", port=PORT)`，且 `INTEGRITY_AUTH_TOKEN` 仅为「optional but recommended」（`integrity_server.py:24`）。默认即：全网监听 + 无鉴权 → 可能被当作 Google Play Integrity API 的**开放中继**（配额/费用滥用 + 信息泄露）。建议：默认 `127.0.0.1`，或在 `0.0.0.0` 时强制要求 token（参照下条 crdt_server 的守卫）。
- **[D3-Low/Medium] sync_server 失败开放**：`server/sync_server/main.py:136-137` 当 `_AUTH_USER/_AUTH_PASS` 为空时 `_verify_basic_auth` 直接返回 `True`（默认无鉴权）；host 默认 `127.0.0.1`（`main.py:539`）尚可，但设为 `0.0.0.0` 且无鉴权时**无告警**。建议：镜像 crdt_server 的守卫（host≠loopback 且无 auth 时警告/拒绝）。
- **良性**：`server/crdt_server.py:532-541` 在 `0.0.0.0` 且无 token 时主动 `logger.warning("...exposed to the network!")`，并支持 WSS；`main.py:514` host 来自 `CRDT_HOST`（默认 `127.0.0.1`）。✅

### 4. 路径 / 注入
- **良性（亮点）**：`server/sync_server/main.py:150-159` `_resolve_safe_path` 拦截 null 字节、`..`、绝对路径、Windows 盘符，再 `(STORAGE_DIR / rel_path).resolve()` 做包含性校验；SQL 全部参数化（`:356/:375/:398`）；auth 用 `hmac.compare_digest` 常量时间比对（`:145-146`）；并有文件魔数校验 `_MAGIC_*`（`:120-131`）。✅

### 5. 配置-实现一致性
- 服务端用环境变量配置（`os.environ.get`），`CRDT_HOST`/`SYNC_HOST` 默认值均为 loopback 且文档化；crdt_server 的 token 守卫确实生效（非假控制）。✅ 仅 D1/D3 的「暴露即警告/失败」未做到 fail-closed（属健壮性缺口，非假控制）。

### 6. 前端 / 客户端（Android）
- **良性（亮点）**：`feature/editor/.../ui/HtmlPreview.kt` 用 `WebViewStyleHelper.configureStaticHtmlWebView`——`core/common/.../WebViewStyleHelper.kt:40-42` 显式 `javaScriptEnabled=false`、`blockNetworkLoads=true`、`blockNetworkImage=true`，渲染用户 HTML 无 JS/无网络。Markdown 预览 WebView（`:64-71`）虽启 JS（KaTeX/Mermaid 所需），但 `blockNetworkLoads=true`、`allowFileAccess=false`，且经 jsoup 消毒（注释 VULN-004）。✅
- **[D5-Low/Info] 导出 VIEW 深链接无 host 限制**：`app/src/main/AndroidManifest.xml:17-22` 注册 `http/https` 的 `ACTION_VIEW` intent-filter（无 `android:host` 限制）。建议：核查该接收 Activity 不把传入 URI 直接 `loadUrl` 进 WebView 或执行；必要时加 host 白名单。

## 门禁适用性说明

Python 版 `check_config_refs.py` **对 Android 部分不适用**（Kotlin/Gradle）；对 **`server/` 的 Python 部分部分适用**——但本仓配置走环境变量、无 pydantic `config_models.py`、无 `config.yaml` 的 `security:` 段，故原脚本的「声明-消费」校验无标的可检。

**最小改造点（若要移植门禁）**：
1. **loopback 强制器**（最高价值，直接防 D1/D3）：扫描 `server/**/*.py` 的 `uvicorn.run` / `host=` 字面量，断言不为 `0.0.0.0`，除非同一文件存在显式 auth 标志位（可复用 `crdt_server.py:532` 的守卫逻辑，改为 CI 失败而非仅警告）；
2. **env 控制声明-消费门禁**：把 README/文档里声称的服务端安全控制（auth token、TLS、rate-limit）做成清单，扫描 `os.environ.get("XXX")` 是否真实读取并进入分支——未消费即失败；
3. **依赖锁门禁**：要求 `server/requirements-lock.txt` 存在且与 `requirements.txt` 一致（防 D2）；
4. **Android 侧**：以 `apkanalyzer`/manifest 解析把导出组件 + intent-filter 列入审查（防 D5 滥用）。
