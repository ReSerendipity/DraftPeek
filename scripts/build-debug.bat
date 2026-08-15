@echo off
REM ============================================================
REM build-debug.bat — 构建 DraftPeek Debug APK
REM ============================================================
REM 双击运行或在命令行执行：scripts\build-debug.bat
REM 输出：app\build\outputs\apk\debug\app-debug.apk
REM ============================================================

cd /d "%~dp0\.."

echo.
echo ============================================================
echo  DraftPeek Debug Build
echo ============================================================
echo.

call gradlew.bat assembleDebug

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================================
    echo  Build SUCCESS
echo  APK: app\build\outputs\apk\debug\app-debug.apk
echo ============================================================
) else (
    echo.
    echo ============================================================
    echo  Build FAILED (exit code %ERRORLEVEL%)
    echo ============================================================
    exit /b 1
)
