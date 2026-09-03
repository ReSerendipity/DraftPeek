# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | ✅ 安全更新         |
| < 1.0   | ❌ 不再支持         |

## Reporting a Vulnerability

DraftPeek 团队重视安全问题。如果您发现安全漏洞，请按以下流程报告：

### 报告渠道

1. **首选**：发送邮件至 `security@draftpeek.com`，主题以 `[SECURITY]` 开头。
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

## Security Measures

DraftPeek 已实施以下安全措施：

- **APK 签名校验**：运行时校验签名证书 SHA-256 指纹
- **DEX 完整性校验**：构建期基线注入 + 运行时 CRC 校验
- **反调试保护**：多层检测（调试器、Frida、Root、模拟器）
- **Native C 反检测**：`native_security.so` 提供 C 层反调试
- **AI 对抗防护**：AI 逆向检测 + 三级响应（WARNING/LOCKED/SELF_DEFEND）
- **SQLCipher 加密数据库**：AES-256 加密本地数据
- **网络安全配置**：禁止明文 HTTP 流量 + 证书锁定（待启用）
- **CodeQL 静态分析**：CI 流水线自动安全扫描
- **Trivy 密钥扫描**：全仓库密钥泄露检测
- **dependency-review**：PR 依赖变更漏洞审查

## Emergency Contact

| 角色 | 职责 | 联系方式 |
| --- | --- | --- |
| Security Lead | 安全评估与响应决策 | security@draftpeek.com |
| Release Manager | 发布流程控制与回滚决策 | release@draftpeek.com |
| DevOps Lead | CI/CD 紧急操作 | devops@draftpeek.com |
