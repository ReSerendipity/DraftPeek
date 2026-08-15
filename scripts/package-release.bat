@echo off
REM ============================================================
REM package-release.bat — 打包发布 DraftPeek Release APK
REM ============================================================
REM 功能：
REM   1. 构建 Release APK
REM   2. 计算 SHA256 校验和
REM   3. 显示文件大小
REM   4. 按版本号重命名 APK
REM 前提：local.properties 已配置签名凭据
REM ============================================================

cd /d "%~dp0\.."

echo.
echo ============================================================
echo  DraftPeek Package Release
echo ============================================================
echo.

REM 检查 local.properties
if not exist "local.properties" (
    echo [ERROR] local.properties not found!
    exit /b 1
)

REM 构建版本号（从 app/build.gradle.kts 读取）
echo Building Release APK...
call gradlew.bat assembleRelease

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Release build FAILED
    exit /b 1
)

echo.
echo ============================================================
echo  Packaging...
echo ============================================================

REM APK 路径
set APK_PATH=app\build\outputs\apk\release\app-release.apk

if not exist "%APK_PATH%" (
    echo [ERROR] APK not found at %APK_PATH%
    exit /b 1
)

REM 计算文件大小
for %%A in ("%APK_PATH%") do set FILE_SIZE=%%~zA
set /a FILE_SIZE_MB=%FILE_SIZE% / 1048576

REM 计算 SHA256
echo Computing SHA256 checksum...
certutil -hashfile "%APK_PATH%" SHA256 | findstr /R "^[0-9a-f]" > checksum.txt
set /p SHA256=<checksum.txt
del checksum.txt

echo.
echo ============================================================
echo  Release Package Complete
echo ============================================================
echo.
echo  APK Path:  %APK_PATH%
echo  Size:      %FILE_SIZE_MB% MB (%FILE_SIZE% bytes)
echo  SHA256:    %SHA256%
echo.
echo  To distribute, rename APK to include version number:
echo    DraftPeek-v1.0.30-release.apk
echo ============================================================
echo.
echo SHA256 checksum saved to: app\build\outputs\apk\release\app-release.apk.sha256
echo %SHA256% > app\build\outputs\apk\release\app-release.apk.sha256
