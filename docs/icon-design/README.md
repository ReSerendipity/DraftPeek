# DraftPeek 图标设计档案

本目录是应用图标优化的**完整历史记录**,请勿删除。

## 内容

| 路径 | 说明 |
|---|---|
| `DraftPeek图标评审板-v11.6.html` | 最终评审板(2026-09-12 快照):G34 弦线家族 13 变体、数学家族、曲线家族、家燕写生/线稿/合成、强调弦色对比条、六维矩阵与 Top3。浏览器直接打开即可查看全部候选。 |
| `icon-export/` | **G34R·红弦定稿导出包**:前景/单色层 vector XML、adaptive icon 定义、背景色值、全密度 PNG(512/192/144/96/72/48)、README 替换指引、`backup-originals/`(替换前的原工程文件备份)。 |
| `scripts/` | 生成与验证脚本:`gen8.js`(评审板生成器,含全部记号几何定义)、`patch*.js`(各轮单变量修改记录)、`audit*.js`(66dp 安全区审计器)、`gen_export.js`/`gen_pngs.py`/`apply_export.js`(资源导出与工程应用)。 |

## 定稿状态(2026-09-13 更新)

- **工程内生效(定稿)**:**G34O 偏心窥圆**——九弦裁入 r27 圆、近眼三弦雾墨色阶(#33475F)、强调弦电光蓝(#3178C6,红弦版经对比落选)、朱砂红眼(#C41E3A)悬浮偏心孔心;背景 #F5F7FA 日夜一致。资源位于 `icon-export-g34o/`,已应用至 `app/src/main/res/`(gradle 构建验证中)。
- **历史定稿**:G34R·红弦(前一版,已应用后被 G34O 替换),其资源与工程备份在 `icon-export/` 与 `icon-export-g34o/backup-previous-g34r/`。
- 复现定稿:`node scripts/export_g34o.js && python scripts/gen_pngs_g34o.py`。

## 复现

```bash
node scripts/gen8.js        # 重新生成评审板(输出路径需按环境调整)
node scripts/audit18.js     # 66dp 安全区审计
```
