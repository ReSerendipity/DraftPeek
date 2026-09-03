# Contributing to DraftPeek

Thank you for your interest in contributing to DraftPeek — an offline Markdown editor for Android (Kotlin + Jetpack Compose + Gradle).

This document gives a short "10-minute quick start" to get contributors productive, and a concise reference for common contribution tasks.

---

## Quick Start (10 minutes)

1. Clone the repository

```bash
git clone https://github.com/ReSerendipity/DraftPeek.git
cd DraftPeek
```

2. Build & install (debug)

```bash
.\gradlew.bat :app:installDevDebug
```

3. Create a branch for your change

```bash
git checkout -b fix/short-description
# make changes, run tests, then push
git commit -m "fix(editor): short description"
git push origin fix/short-description
```

4. Open a Pull Request using the provided template.

---

## Development (local)

Prerequisites
- JDK 17+ / Android Studio
- Android SDK（见 `local.properties.example`）

Run tests

```bash
.\gradlew.bat test                       # 单元测试（JVM）
.\gradlew.bat spotlessCheck              # 格式检查
.\gradlew.bat :app:assembleDevDebug      # 构建 APK
```

---

## How to File Good Issues

- Bug reports: include Android version, device, steps to reproduce, expected vs actual behavior, and logs.
- Feature requests: describe the use case, proposed solution, and any alternatives.

Use the provided issue templates (bug_report / feature_request).

---

## Pull Request Checklist

- Use a descriptive title and include a short summary in the PR body.
- Link related issues using `Closes #<issue>` when appropriate.
- Add tests for new behavior where feasible.
- Run `spotlessCheck` and unit tests locally before opening the PR.
- Follow Conventional Commits for commit messages (`feat:`, `fix:`, `docs:`, etc.).

---

## License

By contributing, you agree your contributions are licensed under the Apache License 2.0 (see [LICENSE](../LICENSE)).

---

Thank you for contributing — the community makes this project better!
