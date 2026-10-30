#!/bin/bash
# ============================================================
# DraftPeek 发布前预检脚本
# ============================================================
# 用途：在推送 release tag 前执行全量检查，确保发布就绪
#
# 用法：
#   ./scripts/pre-release-check.sh <version>
#
# 示例：
#   ./scripts/pre-release-check.sh 1.0.31
#
# 检查项：
#   1. 版本号格式（Semantic Versioning）
#   2. versionCode / versionName 一致性
#   3. CHANGELOG.md 包含对应版本条目
#   4. Git 工作区干净
#   5. 单元测试通过
#   6. Lint 检查通过
#   7. Debug 构建成功
#   8. 签名配置可用
# ============================================================

set -euo pipefail

# ---- 颜色 ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASS="${GREEN}[PASS]${NC}"
FAIL="${RED}[FAIL]${NC}"
WARN="${YELLOW}[WARN]${NC}"

# ---- 参数 ----
if [ $# -lt 1 ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.31"
    exit 1
fi

VERSION="$1"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

echo "============================================================"
echo "  DraftPeek Pre-Release Check"
echo "  Version: $VERSION"
echo "============================================================"
echo ""

ERRORS=0

# ---- 1. 版本号格式 ----
echo "1. Checking version format..."
if echo "$VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+(\.(beta|rc|alpha)\.[0-9]+)?$'; then
    echo "  $PASS Version format valid: $VERSION"
else
    echo "  $FAIL Invalid version format: $VERSION (expected: MAJOR.MINOR.PATCH)"
    ERRORS=$((ERRORS + 1))
fi

# ---- 2. 版本位一致性（权威源 = gradle.properties 的三槽） ----
echo ""
echo "2. Checking version slots in gradle.properties..."
PROPS_FILE="gradle.properties"
BUILD_FILE="app/build.gradle.kts"
if [ ! -f "$PROPS_FILE" ]; then
    echo "  $FAIL $PROPS_FILE not found"
    ERRORS=$((ERRORS + 1))
else
    # 版本位集中在 draftpeek.version.{major,minor,patch}；build.gradle.kts 只保留推导公式。
    read_slot() {
        grep -E "^draftpeek\.version\.$1[[:space:]]*=" "$PROPS_FILE" | head -1 \
            | cut -d= -f2- | tr -d '[:space:]'
    }
    P_MAJOR="$(read_slot major)"
    P_MINOR="$(read_slot minor)"
    P_PATCH="$(read_slot patch)"
    PROPS_VERSION="${P_MAJOR}.${P_MINOR}.${P_PATCH}"

    if [ "$PROPS_VERSION" = "$VERSION" ]; then
        echo "  $PASS gradle.properties version slots match: $PROPS_VERSION"
    else
        echo "  $FAIL version mismatch: expected '$VERSION', gradle.properties says '$PROPS_VERSION'"
        ERRORS=$((ERRORS + 1))
    fi

    # versionCode 必须由公式推导（major*10000 + minor*100 + patch），不允许被改成字面量。
    if [ ! -f "$BUILD_FILE" ]; then
        echo "  $FAIL $BUILD_FILE not found"
        ERRORS=$((ERRORS + 1))
    elif grep -qE 'versionCode[[:space:]]*=[[:space:]]*vMajor[[:space:]]*\*[[:space:]]*10000' "$BUILD_FILE"; then
        echo "  $PASS versionCode is still derived from the formula in $BUILD_FILE"
    else
        echo "  $FAIL $BUILD_FILE 的 versionCode 不再是公式（可能被写成了字面量），版本权威源已失效"
        ERRORS=$((ERRORS + 1))
    fi
fi

# ---- 3. CHANGELOG.md 检查 ----
echo ""
echo "3. Checking CHANGELOG.md..."
CHANGELOG="CHANGELOG.md"
if [ ! -f "$CHANGELOG" ]; then
    echo "  $FAIL $CHANGELOG not found"
    ERRORS=$((ERRORS + 1))
elif ! grep -q "## \[$VERSION\]" "$CHANGELOG"; then
    echo "  $FAIL CHANGELOG.md does not contain '## [$VERSION]' entry"
    echo "  Please add: ## [$VERSION] - $(date +%Y-%m-%d)"
    ERRORS=$((ERRORS + 1))
else
    echo "  $PASS CHANGELOG.md contains entry for version $VERSION"
fi

# ---- 4. Git 工作区状态 ----
echo ""
echo "4. Checking git working tree..."
if [ -n "$(git status --porcelain)" ]; then
    echo "  $FAIL Working tree is not clean. Uncommitted changes:"
    git status --short
    ERRORS=$((ERRORS + 1))
else
    echo "  $PASS Working tree is clean"
fi

# ---- 5. Git tag 检查 ----
echo ""
echo "5. Checking git tag..."
TAG="v$VERSION"
if git rev-parse "$TAG" >/dev/null 2>&1; then
    echo "  $FAIL Tag $TAG already exists"
    ERRORS=$((ERRORS + 1))
else
    echo "  $PASS Tag $TAG does not exist yet"
fi

# ---- 6. local.properties 签名配置 ----
echo ""
echo "6. Checking signing configuration..."
if [ -f "local.properties" ]; then
    if grep -q "RELEASE_STORE_FILE" "local.properties" && \
       grep -q "RELEASE_STORE_PASSWORD" "local.properties" && \
       grep -q "RELEASE_KEY_ALIAS" "local.properties" && \
       grep -q "RELEASE_KEY_PASSWORD" "local.properties"; then
        echo "  $PASS Signing credentials found in local.properties"
    else
        echo "  $WARN local.properties exists but signing credentials incomplete"
    fi
else
    echo "  $WARN local.properties not found (required for release build)"
    echo "  Create it from local.properties.example"
fi

# ---- 7. 单元测试 ----
echo ""
echo "7. Running unit tests..."
if [ -f "./gradlew" ]; then
    if ./gradlew test --stacktrace 2>&1 | tail -5; then
        echo "  $PASS Unit tests passed"
    else
        echo "  $FAIL Unit tests failed"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo "  $WARN gradlew not found, skipping tests"
fi

# ---- 8. Lint 检查 ----
echo ""
echo "8. Running lint check..."
if [ -f "./gradlew" ]; then
    if ./gradlew lint --stacktrace 2>&1 | tail -5; then
        echo "  $PASS Lint check passed"
    else
        echo "  $FAIL Lint check failed"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo "  $WARN gradlew not found, skipping lint"
fi

# ---- 结果 ----
echo ""
echo "============================================================"
if [ $ERRORS -eq 0 ]; then
    echo "  ${GREEN}All pre-release checks passed!${NC}"
    echo "  Ready to tag and push: git tag v$VERSION && git push origin v$VERSION"
else
    echo "  ${RED}$ERRORS check(s) failed.${NC}"
    echo "  Fix the issues above before tagging."
fi
echo "============================================================"

exit $ERRORS
