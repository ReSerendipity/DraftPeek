@echo off
REM ============================================================
REM build-release.bat — 构建 DraftPeek Release APK
REM ============================================================
REM 前提：local.properties 已配置签名凭据
REM 输出：app\build\outputs\apk\release\app-release.apk
REM ============================================================

cd /d "%~dp0\.."

echo.
echo ============================================================
echo  DraftPeek Release Build
echo ============================================================
echo.

REM 检查 local.properties 是否存在
if not exist "local.properties" (
    echo [ERROR] local.properties not found!
    echo Please copy local.properties.example to local.properties
    echo and configure signing credentials.
    exit /b 1
)

REM 检查签名配置
findstr /C:"RELEASE_STORE_FILE" local.properties >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Signing credentials not configured in local.properties!
    echo Please add RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD,
    echo RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD to local.properties
    exit /b 1
)

echo [OK] Signing credentials found.
echo.

call gradlew.bat assembleRelease

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================================
    echo  Release Build SUCCESS
    echo  APK: app\build\outputs\apk\release\app-release.apk
    echo ============================================================
) else (
    echo.
    echo ============================================================
    echo  Release Build FAILED (exit code %ERRORLEVEL%)
    echo ============================================================
    exit /b 1
)
