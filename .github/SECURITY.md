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
- **DEX 完整性校验**：运行时 CRC 校验框架已实现；构建期基线注入**尚未接线**（`DEX_CRC_BASELINE` 恒为 0，校验自动跳过，属计划未实现项）。当前实际生效的是 APK 签名校验
- **反调试保护**：多层检测（调试器、Frida、Root、模拟器）
- **Native C 反检测**：`native_security.so` 提供 C 层反调试（加载失败时显式上报 `THREAT_NATIVE_UNAVAILABLE`，不再静默降级）
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
