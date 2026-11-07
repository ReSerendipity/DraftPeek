# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x（最新发布版） | ✅ 安全更新         |
| 更早的 1.0.x | ❌ 仅严重问题时考虑 backport |
| < 1.0   | ❌ 不再支持         |

**维护责任**：「最新 1.0.x」以 [CHANGELOG.md](../CHANGELOG.md) 最新发布条目为准。
本表由维护者**每次发版时复核**（对应 `docs/RELEASE_CHECKLIST.md` §6 固定检查项），
避免支持版本表随版本发布而过期失效。

## Reporting a Vulnerability

DraftPeek 团队重视安全问题。如果您发现安全漏洞，请按以下流程报告：

### 报告渠道

1. **首选**：发送邮件至 `ReSerendipity@outlook.com`，主题以 `[SECURITY]` 开头（2026-09-15 变更：原 `security@draftpeek.com` 域名未能核实归属，统一使用家族联系邮箱）。
2. **备选**：通过 GitHub 私密安全公告（Security Advisory）提交。
   - 访问 https://github.com/ReSerendipity/DraftPeek/security/advisories/new
   - 选择 "Report a vulnerability"

### 报告内容

请在报告中包含以下信息：
- 漏洞类型（如 XSS、SQL 注入、信息泄露等）
- 受影响的版本号
- 复现步骤（详细描述或 PoC）
- 漏洞影响评估
- 建议的修复方案（如有）

### 响应时间

| 级别 | 响应时间 | 修复目标 |
| --- | --- | --- |
| Critical (RCE/数据泄露) | 24 小时内确认 | 72 小时内修复 |
| High (权限绕过/注入) | 48 小时内确认 | 7 天内修复 |
| Medium (XSS/CSRF) | 72 小时内确认 | 14 天内修复 |
| Low (信息泄露/配置) | 7 天内确认 | 30 天内修复 |

### 规则

- 请勿在公开 Issue 中报告安全漏洞。
- 我们承诺在修复发布前不公开漏洞详情。
- 遵循负责任披露（Responsible Disclosure）原则。
- **密钥泄露**（`release.jks` / CI Secrets）请单独标注 `KEY-COMPROMISE` 并说明泄露范围；
  轮换依赖 APK Signature Scheme v3（`app/signing.gradle`）。
- 涉及 `server/`（Python 同步服务）时请注明监听地址与是否启用鉴权
  （已知遗留风险见 `SECURITY_AUDIT_DraftPeek.md`）。

## Security Measures

DraftPeek 已实施以下安全措施：

- **APK 签名校验**：运行时校验签名证书 SHA-256 指纹
- **DEX 完整性校验**：运行时 CRC 校验 + **构建期基线已接线**（2026-11-07）。基线由 `DexCrcBaselineTask` 在打包前算出该变体全部 `classes*.dex` 的 CRC32 之和，写成 `assets/dex_crc_baseline.txt`（`sum=` / `entries=` 两行）；运行时 `AiDetector.verifyDexIntegrity()` 从 assets 读回，与 APK 内全部 `classes*.dex` 条目 CRC 之和逐位比对，不等即 `DEX_TAMPERED`；基线缺失/不可解析时跳过校验（零误报）。基线**不放进 `BuildConfig`**：常量编进 dex 会改 dex 自身字节，形成"基线描述 dex / dex 含基线"的循环。构建期闸门 `./gradlew :app:verifyDexCrc` 对 3 个变体 APK 逐一复核（实测 3/3 OK；故意把输入收窄成只算 `classes.dex` 后 3/3 报错并 FAILED，闸门有效性已验证）
  - **已知限制（2026-11-07 真机实测）**：「我的 → 关于与安全」显示的状态读的是 `AiProtectionStateHolder` 快照（`AboutSecurityScreen.kt`），而 `AiDetector.assess()` 整体包在 `try/catch(Exception)` 里；检测没跑完或中途抛异常时 holder 不更新，页面仍显示「DEX 完整性：✅ 通过」（fail-open）。负向对照实测：把 beta 包内基线 `sum=` 改成错误值并重签安装，页面**未**变「❌ 已修改」。故构建期闸门有效，运行时比对的可视证据尚不成立，待补三态（未检测/通过/失败）后再宣称运行时校验生效
- **反调试保护**：多层检测（调试器、Frida、Root、模拟器）
- **Native C 反检测**：`native_security.so` 提供 C 层反调试（加载失败时显式上报 `THREAT_NATIVE_UNAVAILABLE`，不再静默降级）。**JNI 类名必须与 R8 保持一致**：`native_security.c` 用硬编码路径 `com/draftpeek/security/AiDetector` 做 `FindClass` + `RegisterNatives`，故 `proguard-rules.pro` 对该类用 `-keep`（不可混淆）而不是 `-keep,allowobfuscation`；`JNI_OnLoad` 每处失败分支都 `ExceptionClear`，避免挂起异常抛穿 `System.loadLibrary` 导致整个 `NativeSecurityChecker` 初始化失败。2026-11-07 实测 1.0.32 正式包（minify 开）即因该类被改名而 native 层整体失效（logcat `ClassNotFoundException: com.draftpeek.security.AiDetector`），修复后 `beta` 构建逐 dex 可查到该类名、真机 logcat 无该异常
- **AI 对抗防护**：AI 逆向检测 + 三级响应（WARNING/LOCKED/SELF_DEFEND）
- **SQLCipher 加密数据库**：AES-256 加密本地数据
- **网络安全配置**：全局禁止明文 HTTP 流量；`api.github.com` 证书锁定**已启用**（双 pin 备份，有效期至 2027-08-27）
- **远程策略验签**：远程安全策略（kill switch）经 Ed25519 验签（构建期注入公钥），验签失败/缺签名/过期一律忽略（fail-closed），防伪造与 MITM
- **CodeQL 静态分析**：CI 流水线自动安全扫描
- **Trivy 密钥扫描**：全仓库密钥泄露检测
- **dependency-review**：PR 依赖变更漏洞审查

### 证书固定轮换 SOP（R3）

`app/src/main/res/xml/network_security_config.xml` 中 `api.github.com` 的 pin 有效期至 **2027-08-27**。pin 过期 = 应用内全部 GitHub 请求失败（可用性事故），必须提前轮换。

| 时间点 | 动作 |
|---|---|
| 2027-05-29（到期前 90 天） | 启动轮换：获取 GitHub 当前证书链，计算新的 SPKI SHA-256 |
| 轮换窗口 | 将新 pin **追加**（而非替换）进 `<pin-set>`（现有双 pin 即此模式），随新版本发布 |
| 旧证书确认下线后 | 在后续版本中移除旧 pin |
| 到期前 7 天仍未轮换 | 升级为 P0 发布事故 |

季度人工核验（GitHub 可能提前轮换证书）：

```bash
openssl s_client -connect api.github.com:443 -servername api.github.com </dev/null 2>/dev/null \
  | openssl x509 -noout -subject -dates
```

## Emergency Contact

| 角色 | 职责 | 联系方式 |
| --- | --- | --- |
| Security Lead | 安全评估与响应决策 | security@draftpeek.com |
| Release Manager | 发布流程控制与回滚决策 | release@draftpeek.com |
| DevOps Lead | CI/CD 紧急操作 | devops@draftpeek.com |
