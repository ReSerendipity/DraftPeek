# DraftPeek 图标导出包 · G34R「红弦」（历史归档）

> **HISTORICAL / DO NOT APPLY**
>
> 本文件是 G34R 历史设计记录，不参与构建，**不要按下方步骤复制进 `app/src/main/res/`**。
> 当前生效图标为 G34O「偏心窥圆」，见 `docs/icon-design/icon-export-g34o/`。

历史方案:九弦 20° 扇形展开,每弦切于内圆 r12.5、端点落在外圆 r26.5,第 4 弦(顶部水平弦)朱砂红、其余石墨墨,无中心元素。前景内容包络半径 31.1dp,在 Android 66dp 安全圆内。零填充、纯描边矢量。

## 文件清单

| 文件 | 用途 |
|---|---|
| `res/drawable/ic_launcher_foreground.xml` | 前景矢量(当时替换原 `drawable-nodpi/ic_launcher_foreground.png`) |
| `res/drawable/ic_launcher_monochrome.xml` | Android 13+ 主题单色层(当时工程缺失,本包新增) |
| `res/mipmap-anydpi/ic_launcher.xml` | adaptive icon 定义(含 monochrome 引用) |
| `res/mipmap-anydpi/ic_launcher_round.xml` | 同上(圆形) |
| `res/values/ic_launcher_background.xml` | 背景色 → `#F5F7FA`(日/夜一致;深底会使墨线不可读) |
| `res/values-night/ic_launcher_background.xml` | 夜间背景色(同上) |
| `store-png/` | Google Play 512 全方图、圆角 512、192/144/96/72/48 各密度(README/文档站用) |

## 历史替换步骤（已废弃，不要执行）

> 以下步骤仅记录 2026-09 当时 G34R 如何进入工程；当前工程已是 G34O，禁止再执行。

1. 备份原文件(本包原 `backup-originals/` 已归档至 `../../initial-2026-08-15/backup-originals/`)。
2. 将 `res/` 下各文件复制到 `app/src/main/res/` 对应目录(覆盖同名文件)。
3. **删除**(或移走)`app/src/main/res/drawable-nodpi/ic_launcher_foreground.png`——避免与新增的 `drawable/ic_launcher_foreground.xml` 资源名冲突(原文件已移入上述 backup-originals)。
4. 执行 `gradlew.bat :app:assembleDebug` 验证构建,并在启动器/主题壁纸下核对:圆/方/水滴遮罩无裁切、29px 通知栏可辨、主题单色正常。

## 参数速查

- 弦数 9 · 角距 20° · 切点半径 12.5 · 半弦长 23.7 · 线宽 4(均为 100 网格单位,×1.08 → 108dp)
- 墨 `#1C2B3A` · 红 `#C41E3A` · 底 `#F5F7FA`
