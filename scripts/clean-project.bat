@echo off
REM ============================================================
REM clean-project.bat — 清理 DraftPeek 构建缓存
REM ============================================================
REM 执行 gradlew clean，清理所有模块的 build/ 目录
REM ============================================================

cd /d "%~dp0\.."

echo.
echo ============================================================
echo  DraftPeek Clean Project
echo ============================================================
echo.

call gradlew.bat clean

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================================
    echo  Clean SUCCESS
    echo  All build directories have been cleaned.
    echo ============================================================
) else (
    echo.
    echo ============================================================
    echo  Clean FAILED (exit code %ERRORLEVEL%)
    echo ============================================================
    exit /b 1
)
