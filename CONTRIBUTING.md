# 贡献指南（CONTRIBUTING）

感谢关注 DraftPeek！本文件给出从克隆到合并的最短路径。

## 1. 环境搭建

```bash
# Android（JDK 17+ / Android SDK 35）
./gradlew assembleDebug
./gradlew installDebug
```

签名配置走 `local.properties`（已 gitignore），不提交 keystore。

## 2. 本地开发循环

| 场景 | 命令 |
|---|---|
| 编译 | `./gradlew assembleDebug` |
| 单元测试 | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lintDebug` |
| 格式化（Ktlint） | `./gradlew ktlintFormat` |

## 3. 分支与提交规范

- 从最新 `main` 切出：`git checkout -b feat/xxx`。
- **Conventional Commits**：`feat(scope): 描述` / `fix(scope): 描述` / `docs:` / `ci:` / `test:` / `chore:`。
- **DCO 签名**：每个提交必须 `git commit -s`。
- 提交层钩子自动跑 structure-guard / gitleaks / 文件卫生；失败请改代码，禁止 `--no-verify`。

## 4. Pull Request

PR 模板会引导填写变更动机、测试结果、自查项。Issue 请使用现成表单。

## 5. 红线

- `feature/` 下按整洁架构分层（data / domain / ui），禁止跨层直接依赖。
- `local.properties` / keystore / `*.jks` / `*.p12` 已 gitignore，禁止入库。
- 版本号集中管理于 `gradle.properties`，通过 `./gradlew bumpVersion` 更新。
