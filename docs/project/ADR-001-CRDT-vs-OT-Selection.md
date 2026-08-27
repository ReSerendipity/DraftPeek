# ADR-001: CRDT vs Operational Transformation 选型决策

> **状态**: Accepted (已移除，保留作为历史参考)
> **日期**: 2026-03-10 (初始决策) / 2026-08-13 (移除) / 2026-08-27 (ADR 补录)
> **决策者**: DraftPeek Team

## 背景

DraftPeek 是一个 Android 端富文本/代码编辑器，需要支持多设备间的文档同步和潜在的实时协同编辑功能。在选择冲突解决策略时，需要考虑以下约束：

1. **移动网络不稳定**：设备经常离线、网络分区是常态
2. **离线编辑需求**：用户在无网络时也要能编辑文档
3. **单服务器资源有限**：不部署大型服务器集群
4. **代码/文本编辑的字符级精度要求**：不能丢失操作

## 决策

选择 **Yjs CRDT**（基于 RGA-Split 数据结构）而非 Operational Transformation (OT)。

## 理由

### 选择 CRDT 的原因

| 维度 | CRDT (Yjs) | OT |
|------|-----------|-----|
| 离线编辑 | ✅ 天然支持，自动合并 | ❌ 需要中心服务器仲裁 |
| 网络分区容忍 | ✅ 分区恢复后自动收敛 | ❌ 需要操作序列仲裁 |
| 服务器复杂度 | ✅ 可做无状态中继 | ❌ 需要维护操作变换矩阵 |
| 收敛保证 | ✅ 数学保证最终收敛 | ⚠️ 依赖正确实现 OT 函数 |
| 生态成熟度 | ✅ Notion/Atlassian 生产验证 | ✅ Google Docs 生产验证 |
| 移动端适配 | ✅ 允许长时间离线 | ❌ 长离线后需复杂 catch-up |

### 选择 Yjs 而非 Automerge 的原因

- Yjs 的 RGA-Split 实现对文本编辑场景优化更好
- y-websocket 协议成熟，服务器实现简单
- ypy 提供了 Python 绑定（虽然有 Maven 缺失问题，见下文）
- 社区活跃度高，文档完善

### CAP 理论定位

选择 **AP**（Availability + Partition Tolerance）：
- **一致性弱化**：接受最终一致性，客户端可能读到过期数据
- **可用性优先**：用户随时可编辑，不因同步阻塞
- **分区容忍必选**：移动网络天然不稳定

## 后果

### 正面

- 离线编辑无需特殊处理
- 服务器实现简单（WebSocket 中继 + state vector diff）
- 不需要维护操作序列

### 负面

- "最终一致"无时间上限
- 冲突对用户不透明（自动合并但用户不知发生了冲突）
- 无强一致性保证（不适合需要强一致的场景）

## 移除说明 (2026-08-13)

该功能已于 2026-08-13 从项目规划中正式移除，原因：
1. `simple-yjs 1.0.0` Kotlin 绑定不存在于 Maven Central
2. Kotlin Gradle 模块集成需大规模重构
3. 端到端协同测试需部署 Yjs WebSocket 服务器

服务器端代码保留作为技术参考，2026-08-27 进行了原型级质量改进。

## 参考

- [Yjs](https://github.com/yjs/yjs)
- [ypy](https://github.com/y-crdt/ypy)
- [y-websocket protocol](https://github.com/yjs/y-websocket)
- Shapiro et al., "A comprehensive study of Convergent and Commutative Replicated Data Types" (2011)
- [CAP Theorem](https://en.wikipedia.org/wiki/CAP_theorem)
