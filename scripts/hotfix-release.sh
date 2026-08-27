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

# ---- 更新版本号 ----
echo ""
echo "Updating version in app/build.gradle.kts..."
sed -i "s/versionCode = .*/versionCode = $(echo $HOTFIX_VERSION | awk -F. '{print $1*10000+$2*100+$3}')/" app/build.gradle.kts
sed -i "s/versionName = .*/versionName = \"$HOTFIX_VERSION\"/" app/build.gradle.kts
echo "Updated versionCode and versionName"

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
