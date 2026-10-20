# SRE 运行手册（日志落点 / 勘误指引）

> 2026-09-18 建立：补齐家族统一的日志落点文档（勘误用）。

## 日志落点（勘误用）

| 服务 | 日志文件 | 说明 |
|------|----------|------|
| Sync Server | `server/logs/sync_server.log` | RotatingFileHandler（5MB × 3） |
| CRDT Server | `server/logs/crdt_server.log` | 同上；设 `CRDT_AUDIT_LOG=1` 可输出审计级日志 |
| Integrity Server | `server/logs/integrity_server.log` | 同上 |

> 日志由 `server/_logging_setup.py` 统一配置；`server/logs/.gitkeep` 保证目录随仓库存在，日志文件在服务首次运行后生成。勘误时如目录为空，先确认服务已启动。
> 另：`git log` / CI 中的勘误入口见 `FIX_LOG.md`。
