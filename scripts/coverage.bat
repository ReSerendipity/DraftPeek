@echo off
REM 代码覆盖率报告生成脚本
REM 用于本地开发和 CI 流程

echo ========================================
echo DraftPeek 代码覆盖率报告生成
echo ========================================

REM 清理旧的覆盖率数据
echo [1/4] 清理旧的覆盖率数据...
del /q /s *.exec *.ec 2>nul

REM 运行所有单元测试
echo [2/4] 运行所有单元测试...
call gradlew testDebugUnitTest --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo 测试失败，请修复后重试
    exit /b 1
)

REM 生成覆盖率报告
echo [3/4] 生成 JaCoCo 覆盖率报告...
call gradlew jacocoTestReport --no-daemon

REM 打开报告
echo [4/4] 生成完成！
echo.
echo 覆盖率报告位置：
echo   - HTML: build\reports\jacoco\html\index.html
echo   - XML:  build\reports\jacoco\report.xml
echo.
echo 提示：在浏览器中打开 HTML 报告查看可视化覆盖率

REM 检查覆盖率阈值
echo.
echo ========================================
echo 覆盖率阈值要求 (codecov.yml):
echo   - 项目整体：60%%
echo   - core-common: 70%%
echo   - core-data: 75%%
echo   - core-network: 80%%
echo   - feature-editor: 60%%
echo   - feature-browser: 60%%
echo   - app: 50%%
echo ========================================
echo.

exit /b 0