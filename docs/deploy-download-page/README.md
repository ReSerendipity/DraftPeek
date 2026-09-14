# DraftPeek 自托管下载页（发布管理评估 P0-1 工程侧基座）

> 背景：产物当前仅发布到 GitHub Release **draft**，非开发者用户无法便捷获取。
> 分发渠道（Play / 蒲公英 / 自托管）属**产品决策**；本目录提供渠道无关的
> 工程基座——一旦渠道选定，可直接部署或改造，无需再从零实现。
> 详见 `docs/release-version-management-evaluation.md` P0-1 与三问 Q2。

## 目录内容

| 文件 | 作用 |
|------|------|
| `index.html` | 单文件下载页：读取 `latest.json`，展示版本/日期/SHA256 并提供 APK 下载按钮 |
| `latest.json.example` | 版本清单契约示例（`versionName` / `versionCode` / `apkUrl` / `sha256` / `minAndroid` / `releasedAt`） |

## 部署步骤

1. 复制 `latest.json.example` 为 `latest.json`，随发版更新字段：
   - `versionCode` 必须与 `gradle.properties` 计算值一致
     （`major*10000 + minor*100 + patch`，CI `release.yml` 的版本门禁兜底）；
   - `sha256` 与 GitHub Release 附件 `.sha256` 内容一致（发布检查清单第 5 节）。
2. 将 APK 从 GitHub Release（private 仓库 draft → published，或从 CI 产物）拷贝到
   服务器 `apk/` 目录，或将 `apkUrl` 指向对象存储/CDN。
3. 用任意静态服务器托管本目录，例如：

```bash
# 本地验证
python -m http.server 8080 --directory docs/deploy-download-page
# 生产（示例）：Nginx
#   location /download/ { alias /srv/draftpeek/download/; add_header Cache-Control "no-cache"; }
# latest.json 必须 no-cache，APK 可长缓存。
```

4. （HTTPS 必须）in-app 更新检测（规划，未实现）将 GET `latest.json` 并比较
   `versionCode`，HTTP 明文会被 Android 网络安全策略拦截。

## 与发布流程的关系

- `docs/RELEASE_CHECKLIST.md` §5 已包含「分发链接就绪」检查项；
  启用本页后，发版时需同步更新 `latest.json`（建议纳入发版 checklist 的固定动作）。
- 本页**不承载**任何签名密钥或私有源代码；APK 为已签名产物。

## 边界（明确不做 / 未决）

- 渠道选择（自托管 vs Play vs 蒲公英）= 产品决策，本目录只是基座。
- in-app 更新检测模块（Kotlin 侧）未实现，属后续工程项；契约已由
  `latest.json` 固化，实现时无需改服务端。
- 本仓库已全量公开（2026-09 修订）：GitHub Release 资产可匿名下载，下载页可直接链接其资产——
  分发链路已无私有仓库限制，渠道选择仍属产品决策。
