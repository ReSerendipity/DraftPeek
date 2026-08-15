@echo off
REM ============================================================
REM run-unit-tests.bat — 运行 DraftPeek 单元测试
REM ============================================================
REM 运行所有模块的单元测试，完成后提示测试报告路径
REM ============================================================

cd /d "%~dp0\.."

echo.
echo ============================================================
echo  DraftPeek Unit Tests
echo ============================================================
echo.

call gradlew.bat test --stacktrace

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================================
    echo  All Tests PASSED
    echo ============================================================
    echo.
    echo Test Reports:
    echo   core\common:    core\common\build\reports\tests\testDebugUnitTest\index.html
    echo   core\data:      core\data\build\reports\tests\testDebugUnitTest\index.html
    echo   core\domain:    core\domain\build\reports\tests\testDebugUnitTest\index.html
    echo   feature\editor: feature\editor\build\reports\tests\testDebugUnitTest\index.html
    echo   feature\settings: feature\settings\build\reports\tests\testDebugUnitTest\index.html
    echo   feature\terminal: feature\terminal\build\reports\tests\testDebugUnitTest\index.html
    echo.
    echo Opening test report...
    start "" "core\common\build\reports\tests\testDebugUnitTest\index.html"
) else (
    echo.
    echo ============================================================
    echo  Tests FAILED (exit code %ERRORLEVEL%)
    echo  Check reports in build\reports\tests\ directories
    echo ============================================================
    exit /b 1
)
