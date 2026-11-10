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

4. （HTTPS 必须）in-app 更新检测**已实现**（2026-11-07），但走的是 GitHub Releases API
   通道（`GET /repos/ReSerendipity/DraftPeek/releases/latest`，比较 `tag_name` 与
   本地 `versionName`），**不读**本目录的 `latest.json`；本页下载链路依旧只服务
   浏览器下载。两条通道的分歧见下文「边界」。

## OG 图片与生产域名

当前仓库没有确定的生产/发布域名；根 `index.html` 与本目录 `index.html` 的 `og:image`
保持 G34O 的相对路径，离线打开可用。若要对社交平台提供外链预览，需在部署域名确定后
把 `og:image` 改为绝对 URL；在此之前不得写入未核实的域名。

## 与发布流程的关系

- `docs/RELEASE_CHECKLIST.md` §5 已包含「分发链接就绪」检查项；
  启用本页后，发版时需同步更新 `latest.json`（建议纳入发版 checklist 的固定动作）。
- 本页**不承载**任何签名密钥或私有源代码；APK 为已签名产物。

## 边界（明确不做 / 未决）

- 渠道选择（自托管 vs Play vs 蒲公英）= 产品决策，本目录只是基座。
- **当前默认分发渠道：GitHub Releases 直链下载**（资产可匿名下载，见本节末条）。
  本目录（自托管下载页）**保持基座态、不部署**；`latest.json` 不随 GitHub Release 更新。
  **切换触发条件**（满足任一即启用自托管页，并改 in-app 更新通道，见下条）：
  ① 确定对外发布域名与服务器；② 决定不再依赖 GitHub 作为唯一分发入口（Play / 蒲公英 / 镜像站落地）。
  切换时本目录 `index.html` 直接可部署，`og:image` 按 §OG 图片节同步改绝对 URL。
- **versionCode 契约（唯一规则）**：`versionCode = MAJOR * 10000 + MINOR * 100 + PATCH`，
  单调递增；与 `versionName` 的对应关系为 `versionName = "MAJOR.MINOR.PATCH"`（预发布加 `-alpha.N` /
  `-beta.N` / `-rc.N` 后缀）。`latest.json` 的 `versionCode` 必须等于 `gradle.properties`
  集中版本位按上式的计算值，`versionName` 必须与同版本 tag 一致；两者任一对不上即为契约破坏。
- in-app 更新检测已实现，入口在「我的 → 关于与安全 → 检查更新」
  （`feature/settings/.../AboutSecurityScreen.kt`），判定与网络层在
  `core/common/.../update/{UpdateChecker,UpdateVersion}.kt`。
  当前实现读 GitHub Releases API 的 `tag_name`，与本目录 `latest.json` 的
  `versionCode` 契约**尚未统一**：若日后切到自托管通道，需改 `UpdateChecker`
  的取数地址与比较口径（`latest.json` 契约无需改服务端）。
- 本仓库已全量公开（2026-09 修订）：GitHub Release 资产可匿名下载，下载页可直接链接其资产——
  分发链路已无私有仓库限制，渠道选择仍属产品决策。
