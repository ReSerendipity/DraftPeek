# Incident Response Plan

## Overview

本文档定义 DraftPeek 项目在发布后遇到严重事故时的响应流程和 SLA。

## Incident Severity Levels

| 级别 | 定义 | 示例 |
| --- | --- | --- |
| **P0 — Critical** | 影响所有用户的核心功能不可用，或存在数据安全风险 | 应用启动崩溃率 >5%；数据库迁移失败导致数据丢失；安全漏洞被利用 |
| **P1 — High** | 影响大部分用户的重要功能异常，但有 workaround | 编辑器无法保存文件；特定文件类型解析崩溃；ANR 率 >3% |
| **P2 — Medium** | 影响少数用户或非核心功能异常 | 特定设备兼容性问题；UI 渲染异常；性能退化但功能可用 |

## Response SLA

| 级别 | 确认时间 | 评估时间 | 修复目标 | 通报范围 |
| --- | --- | --- | --- | --- |
| **P0** | 15 分钟 | 1 小时 | 4 小时内热修复 | 全团队 + 用户公告 |
| **P1** | 1 小时 | 4 小时 | 24 小时内修复 | 开发团队 |
| **P2** | 4 小时 | 1 天 | 下一个版本修复 | 开发团队 |

## Response Procedure

### Step 1: 事故发现与确认 (Detect & Confirm)

1. 事故来源：CrashMetrics 健康报告、用户反馈、监控告警
2. 值班人员确认事故级别（参考上方分级表）
3. 在团队群中通报：`[INCIDENT-P0/P1/P2] 简述`
4. 创建事故跟踪 Issue

### Step 2: 影响评估 (Assess)

1. 确认受影响的用户范围（版本、设备、API 级别）
2. 运行 `CrashMetrics.getHealthReport()` 获取崩溃率报告
3. 评估是否有数据安全风险（数据库损坏、密钥泄露等）
4. 决定是否需要暂停当前灰度发布

### Step 3: 紧急修复 (Remediate)

#### P0 热修复流程

1. 创建 `hotfix/v{version}-hotfix` 分支
2. 修复关键问题（仅修复核心问题，不附带其他变更）
3. 推送 `v{version}-hotfix` tag 触发 release.yml 发布流水线
4. 通知用户通过 GitHub Release 下载紧急修复版本
5. 若无法在 4 小时内修复，考虑回滚到上一稳定版本

#### 回滚流程

1. 将 GitHub Release 的最新版本标记为 "Pre-release / Deprecated"
2. 在 README 中更新推荐版本为上一稳定版
3. 通过 Feature Flag 关闭有问题的功能（若适用）
4. 推送新 tag 重新构建上一个稳定版本

### Step 4: 事后复盘 (Postmortem)

1. 在修复完成后 48 小时内编写事故复盘报告
2. 报告包含：时间线、根因分析、影响范围、修复措施、预防措施
3. 将复盘报告存档至 `docs/postmortems/` 目录

## Rollback Triggers

以下条件触发自动暂停灰度发布或回滚：

| 触发条件 | 阈值 | 动作 |
| --- | --- | --- |
| 崩溃率 | > 1% (CrashMetrics.isHealthy() = false) | 暂停灰度 + 评估回滚 |
| ANR 率 | > 3% | 暂停灰度 |
| 数据库迁移失败 | 任何报告 | 立即回滚 |
| 安全漏洞被利用 | 任何确认 | 立即热修复 |

## On-Call Schedule

| 日期 | 值班人员 | 备班人员 |
| --- | --- | --- |
| 周一-周五 | DevOps Lead | Security Lead |
| 周末 | Release Manager | DevOps Lead |

值班联系方式见 [SECURITY.md](../SECURITY.md#emergency-contact)。

## Escalation Chain

```
发现事故
  → 值班人员 (15min 确认)
    → Release Manager (评估级别)
      → Security Lead (若涉及安全)
        → 团队负责人 (若 P0 级别)
```

## Related Documents

- [SECURITY.md](../SECURITY.md) — 安全漏洞报告流程
- [CHANGELOG.md](../CHANGELOG.md) — 版本变更记录
- `.github/workflows/release.yml` — 自动发布流水线
