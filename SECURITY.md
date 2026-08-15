# Security Policy (安全政策)

> DraftPeek Android 应用安全政策与防护机制文档。
> 借鉴 SeedVR2 / Image_MultiModel 的安全政策格式，并结合 Android 移动端运行时安全特性编写。

## Supported Versions (支持的版本)

DraftPeek 采用语义化版本。安全修复仅适用于以下版本：

| 版本 | 支持状态 |
|------|----------|
| 1.0.x（main 分支） | ✅ 积极维护 |
| < 1.0 | ❌ 不再维护 |

## Reporting a Vulnerability (报告安全漏洞)

我们非常重视 DraftPeek 的安全问题。如果您发现安全漏洞，请**不要**通过公开 Issue 报告，而是按照以下流程私下披露：

### 报告渠道

- **邮箱**：请发送邮件至 `security@draftpeek.dev`（标题注明 `[SECURITY]`）
- **GitHub Security Advisory**：推荐使用 GitHub 的[私密漏洞报告功能](https://github.com/ReSerendipity/DraftPeek/security/advisories/new)

### 报告内容

为帮助我们快速定位和修复问题，请在报告中包含：

1. **漏洞描述**：问题的清晰描述及其影响范围
2. **复现步骤**：详细的复现方法（最小化 PoC 优先）
3. **影响评估**：可能的攻击场景和受影响的用户范围
4. **环境信息**：Android 版本、设备型号、DraftPeek 版本
5. **建议修复方案**（可选）

### 响应时间承诺

| 阶段 | 时间承诺 |
|------|----------|
| 确认收到报告 | 48 小时内 |
| 初步评估与分类 | 5 个工作日内 |
| 严重（Critical/High）漏洞优先修复 | 优先发布补丁 |
| 修复版本发布 | 30 天内（严重漏洞 7 天内） |

## Disclosure Process (披露流程)

1. **私下报告**：漏洞通过上述渠道私下报告给维护团队
2. **确认与评估**：维护团队确认漏洞并评估严重程度
3. **修复开发**：在私有分支中开发修复方案
4. **修复发布**：发布修复版本，并在 Release Notes 中说明安全问题
5. **公开公告**：修复版本发布后，通过 GitHub Security Advisory 公开披露漏洞详情
6. **致谢**：在征得报告者同意后，在公告中致谢漏洞报告者

## Security Measures (内置安全措施)

DraftPeek 在运行时采取多层防护机制，覆盖**代码混淆、完整性校验、反调试、数据加密、供应链安全**等维度：

### 代码保护与混淆

- **R8 代码收缩/优化/混淆**：Release 构建启用 `isMinifyEnabled`，细粒度 keep 规则保护反射库（Apache POI、JGit、sora-editor、tree-sitter）
- **混淆字典**：使用 `proguard-dict.txt` 自定义类/方法/包混淆字典，提高逆向难度
- **Release 日志剥离**：通过 `-assumenosideeffects` 移除所有 `android.util.Log` 调用，防止 logcat 泄露内部信息
- **Native C 层反检测**：`native_security.c` 通过 JNI 提供比 Java/Kotlin 更难被 Frida Hook 的反调试能力

### 完整性校验与反篡改

- **APK 完整性校验**：`ApkIntegrityChecker` 校验 APK 签名一致性
- **DEX 完整性校验**：`DexIntegrityChecker` 校验 DEX 文件 CRC 基线（构建期注入，CI 无签名时自动跳过）
- **Play Integrity API**：调用 Google Play Integrity 验证设备与包完整性
- **安全门禁**：`SecurityIntegrityChecker` 统一编排各完整性检查，采用 allowobfuscation 防止安全类被定位

### 反调试与反篡改

- **AntiDebug**：检测调试器附加、Frida 等动态调试工具
- **Native 反调试**：C 层的 `IsDebuggerPresent` 等检测
- **AI 对抗防护**：`AiDetector` / `AiResponseExecutor` / `LegalDeterrence` 检测 AI 驱动的自动化逆向，并内建合法性威慑；`decoy/` 虚假类包用于迷惑逆向分析

### 数据安全

- **SQLCipher 加密数据库**：Room 数据库使用 SQLCipher 全量加密
- **SecureFileStorage / SecurePreferences**：基于 Keystore 自研 AES-256-GCM 加密（已替代废弃的 Jetpack security-crypto）
- **Keychain 密钥管理**：敏感密钥存取走系统 Keychain

### 输入安全

- **HTML 消毒**：富文本编辑走 Compose 原生富文本编辑器（仅 Markdown 内容，不渲染原始 HTML）；Markdown 预览渲染前使用 markdown-preview.html 内置白名单 sanitizer（标签/属性白名单 + 危险标签黑名单）与 MarkdownSanitizer 正则层双重过滤，防止 XSS
- **网络配置白名单**：`network_security_config.xml` 限制允许的传输域

### 依赖安全

- **依赖版本锁定**：`gradle/libs.versions.toml` 集中管理依赖版本
- **依赖漏洞审查 CI**：`security.yml` 中 dependency-review + CodeQL 自动扫描 PR 依赖变更

## 安全审查机制 (Security Review)

DraftPeek 通过 `.github/workflows/security.yml` 建立自动化安全审查流程：

| 审查维度 | 工具 | 触发时机 |
|----------|------|----------|
| 依赖漏洞审查 | dependency-review | PR 依赖变更时 |
| 静态安全分析 | CodeQL (kotlin/java) | push / PR / 每周定时 |
| 密钥泄露扫描 | Trivy FS | push / 每周定时 / 手动 |
| 供应链漏洞 | dependency-review (OSV) | PR 依赖变更时 |

---

版权所有 © 2026 DraftPeek. 本安全政策遵循相应开源许可证。