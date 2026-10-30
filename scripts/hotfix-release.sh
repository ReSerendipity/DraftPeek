#!/bin/bash
# ============================================================
# DraftPeek 紧急热修复发布脚本
# ============================================================
# 用途：从最新 release tag 创建 hotfix 分支，辅助快速发布修复版本
#
# 用法：
#   ./scripts/hotfix-release.sh <base_version> <hotfix_number>
#
# 示例：
#   ./scripts/hotfix-release.sh 1.0.31 1
#   → 创建 hotfix/v1.0.31.1 分支，更新版本号，引导完成 hotfix
#
# 前提：
#   - 当前在 main 分支
#   - 工作区干净（无未提交变更）
#   - 已配置 git user.name / user.email
# ============================================================

set -euo pipefail

# ---- 参数检查 ----
if [ $# -lt 2 ]; then
    echo "Usage: $0 <base_version> <hotfix_number>"
    echo "Example: $0 1.0.31 1"
    exit 1
fi

BASE_VERSION="$1"
HOTFIX_NUM="$2"
HOTFIX_VERSION="${BASE_VERSION}.${HOTFIX_NUM}"
BASE_TAG="v${BASE_VERSION}"
HOTFIX_BRANCH="hotfix/v${HOTFIX_VERSION}"
HOTFIX_TAG="v${HOTFIX_VERSION}"

# ---- 版本模型核验（在任何写盘动作之前） ----
# 本仓版本权威源是 gradle.properties 的 draftpeek.version.{major,minor,patch} 三槽，
# versionCode 由 app/build.gradle.kts 的公式 major*10000+minor*100+patch 推导。
# 「<base>.<hotfix 号>」拼出的四段号在这套模型里无法表示，放过去只会把公式改写成字面量。
if ! printf '%s' "$HOTFIX_VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
    echo "[ERROR] 派生出的版本号 '$HOTFIX_VERSION' 不符合本仓的三段版本模型。"
    echo "        本仓 hotfix 的正确做法：递增 gradle.properties 的 draftpeek.version.patch"
    echo "        （./gradlew bumpVersion -Pbump=patch），再按 docs/release-governance.md §5"
    echo "        走主干等价提交 + 正常 tag 发版；不要拼四段号。"
    exit 1
fi

# ---- 前置检查 ----
echo "============================================================"
echo "  DraftPeek Hotfix Release Helper"
echo "============================================================"
echo "  Base version:     $BASE_VERSION"
echo "  Hotfix version:   $HOTFIX_VERSION"
echo "  Hotfix branch:     $HOTFIX_BRANCH"
echo "  Hotfix tag:        $HOTFIX_TAG"
echo "============================================================"
echo ""

# 检查当前分支
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo "[ERROR] Must be on 'main' branch (currently on '$CURRENT_BRANCH')"
    exit 1
fi

# 检查工作区状态
if [ -n "$(git status --porcelain)" ]; then
    echo "[ERROR] Working tree is not clean. Commit or stash changes first."
    git status --short
    exit 1
fi

# 检查 base tag 是否存在
if ! git rev-parse "$BASE_TAG" >/dev/null 2>&1; then
    echo "[ERROR] Tag $BASE_TAG does not exist."
    echo "Available tags:"
    git tag --list 'v*' | tail -10
    exit 1
fi

# 检查 hotfix 分支是否已存在
if git rev-parse "$HOTFIX_BRANCH" >/dev/null 2>&1; then
    echo "[ERROR] Branch $HOTFIX_BRANCH already exists."
    exit 1
fi

# 检查 hotfix tag 是否已存在
if git rev-parse "$HOTFIX_TAG" >/dev/null 2>&1; then
    echo "[ERROR] Tag $HOTFIX_TAG already exists."
    exit 1
fi

# ---- 创建 hotfix 分支 ----
echo "Creating hotfix branch from $BASE_TAG..."
git checkout -b "$HOTFIX_BRANCH" "$BASE_TAG"
echo "Created branch $HOTFIX_BRANCH"

# ---- 更新版本号（只改权威源 gradle.properties，不动 build.gradle.kts 的推导公式） ----
echo ""
echo "Updating draftpeek.version.* in gradle.properties..."
HF_MAJOR=$(printf '%s' "$HOTFIX_VERSION" | cut -d. -f1)
HF_MINOR=$(printf '%s' "$HOTFIX_VERSION" | cut -d. -f2)
HF_PATCH=$(printf '%s' "$HOTFIX_VERSION" | cut -d. -f3)
sed -i "s/^draftpeek\.version\.major.*/draftpeek.version.major=${HF_MAJOR}/" gradle.properties
sed -i "s/^draftpeek\.version\.minor.*/draftpeek.version.minor=${HF_MINOR}/" gradle.properties
sed -i "s/^draftpeek\.version\.patch.*/draftpeek.version.patch=${HF_PATCH}/" gradle.properties
echo "Updated gradle.properties to ${HF_MAJOR}.${HF_MINOR}.${HF_PATCH}"

# ---- 更新 CHANGELOG ----
echo ""
echo "Adding hotfix entry to CHANGELOG.md..."
TODAY=$(date +%Y-%m-%d)
# 在 [Unreleased] 下方插入 hotfix 条目
sed -i "/## \[Unreleased\]/a\\\n## [${HOTFIX_VERSION}] - ${TODAY}\n\n### Fixed\n- Hotfix: describe the fix here" CHANGELOG.md
echo "Added CHANGELOG entry for $HOTFIX_VERSION"

# ---- 指引 ----
echo ""
echo "============================================================"
echo "  Hotfix branch ready!"
echo "============================================================"
echo ""
echo "Next steps:"
echo "  1. Make minimal fix (only P0 issue, no new features)"
echo "  2. Update CHANGELOG.md [${HOTFIX_VERSION}] entry with actual fix description"
echo "  3. Run tests: ./gradlew test"
echo "  4. Build release: ./gradlew assembleRelease"
echo "  5. Commit: git commit -am 'release(${HOTFIX_VERSION}): hotfix'"
echo "  6. Tag: git tag ${HOTFIX_TAG}"
echo "  7. Push tag: git push origin ${HOTFIX_TAG}"
echo "  8. After CI passes, publish GitHub Release"
echo "  9. Merge back to main:"
echo "     git checkout main"
echo "     git merge ${HOTFIX_BRANCH}"
echo "     git branch -d ${HOTFIX_BRANCH}"
echo ""
