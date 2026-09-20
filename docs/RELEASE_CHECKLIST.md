# DraftPeek 发布前检查清单（Release Checklist）

> 适用对象：DraftPeek Android（v1.0.x，GitHub Actions 发版）。
> 使用方式：**按顺序逐项打勾**，全部通过后才允许点击发布 GitHub Release（draft → published）。
> 与 CI 门禁的关系：CI（`release.yml` 的版本一致性 step）是兜底；本清单是人工确认层，两者不可互相替代。

---

## 1. 版本一致性（自动化，可由 CI 兜底）

- [ ] `gradle.properties` 的 `draftpeek.version.{major,minor,patch}` 已升到目标版本
      （或已运行 `./gradlew bumpVersion -Pbump=patch|minor|major`）
- [ ] `CHANGELOG.md` 已含 `## [x.y.z] - YYYY-MM-DD` 条目（或由 `./gradlew generateChangelog` 生成后人工校订）
- [ ] 本地预检通过：`python scripts/check_version_consistency.py`（PR 期宽容模式）
- [ ] tag 严格模式预演通过：
      `python scripts/check_version_consistency.py --strict --tag vX.Y.Z`
- [ ] versionCode 自动核算正确（`major*10000 + minor*100 + patch`，脚本已覆盖）

## 2. 构建与质量门禁（CI 执行，人工确认结果）

- [ ] `./gradlew test`（全部模块单测）通过
- [ ] `./gradlew lint` 通过
- [ ] CI `release.yml` 建 Release 之前的步骤全绿（产物构建 + 验签 + checksums + draft 创建）
- [ ] macrobenchmark：**当前不是发布门禁**（PR #88 已把它移到 `Create GitHub Release` 之后，
      且 13/13 受 issue #87 三类原因阻塞）。它转绿后请把本条恢复为门禁项，并补上"报告已人工查看"
- [ ] `apksigner verify --verbose --print-certs` 输出确认 v2+v3 签名方案生效

## 3. 签名与密钥（人工确认）

- [ ] 确认 CI 解码的是**正式** `RELEASE_STORE_FILE_BASE64`（而非 `ci-release.jks` 测试密钥；
      测试密钥只存在于 `android.yml` 的 release-build 验证 job）
- [ ] `local.properties` 未被意外提交（`git status` 无该文件）
- [ ] keystore / 密码未出现在任何日志、构建产物或 Release 说明中

## 4. Tag 与远程（✅ 2026-09 修订：双仓体系已废止，全量公开）

- [ ] tag 打在 main 提交上：`git tag vX.Y.Z`
- [ ] tag 推送标准远程：`git push origin vX.Y.Z`
- [ ] `release.yml` 触发于本仓库的 Actions 页（tag 触发 `v*`）
- [ ] 推送前确认 `git remote -v` 无残留 `private` 远程

## 5. 产物与分发

- [ ] draft Release 已生成：APK + AAB + `.sha256` 三类附件齐全
- [ ] 哈希取的是**本次 run** 的资产：重跑 `release.yml` 会覆盖同名资产并换 APK 哈希
      （同 tag 树两次构建实测：AAB 摘要不变、APK 由 `af76e377…` 变 `ad62508e…`，大小一致）。
      因此报告/验签前重新下载一次，别引用上一轮的数字
- [ ] 本地抽查完整性：下载 APK 后 `sha256sum -c *.apk.sha256` 通过
- [ ] Release 说明中的 SHA256 与 `.sha256` 文件一致
- [ ] 分发链接已就绪（当前：GitHub Release；若启用自托管下载页/更新检测，须先更新
      对应服务端清单再发版）
- [ ] （如适用）`docs/` 中指向下载的链接仍有效（CI `check_spec_refs.py` 兜底）

## 6. 文档与策略同步

- [ ] `SECURITY.md` 支持版本表的「最新 1.0.x」已指向本版本
- [ ] `README.md` 环境要求表与 `gradle/libs.versions.toml` / wrapper 实际版本一致
      （`scripts/check_version_consistency.py` 已覆盖 AGP/Gradle/Kotlin/Build Tools）
- [ ] 依赖变更时：全部受影响模块已重新 `--write-locks` 并入库（STRICT 锁定模式下漏锁即构建失败）

## 7. 发布（最后一击）

- [ ] 以上全部勾选后，在 GitHub Release 页把 draft 发布为正式版
- [ ] 发布后验证：下载页可访问、APK 可安装并启动、首次启动自检（签名校验）通过
- [ ] 发布后记录：在 `CHANGELOG.md` 对应条目确认日期正确，归档 tag 与产物链接

---

## 快速命令汇总

```bash
# 版本一致性（本地）
python scripts/check_version_consistency.py
python scripts/check_version_consistency.py --strict --tag vX.Y.Z

# 构建 + 校验
./gradlew test lint
./gradlew assembleRelease bundleRelease
./gradlew signingReport | grep -i "Variant\|Config\|Store"
sha256sum app/build/outputs/apk/release/*.apk

# 打 tag（2026-09 修订：双仓体系已废止，推 origin）
git tag vX.Y.Z && git push origin vX.Y.Z
```
