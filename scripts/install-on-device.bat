@echo off
REM ============================================================
REM install-on-device.bat — 编译并安装 DraftPeek 到连接的设备
REM ============================================================
REM 前提：设备已连接并开启 USB 调试
REM 使用 adb install 安装 Debug APK
REM ============================================================

cd /d "%~dp0\.."

echo.
echo ============================================================
echo  DraftPeek Install on Device
echo ============================================================
echo.

REM 检查是否有设备连接
adb devices | findstr /V "List of devices" | findstr /R "." >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] No device connected!
    echo Please connect a device with USB debugging enabled.
    exit /b 1
)

echo [OK] Device connected.
echo.

echo Building Debug APK...
call gradlew.bat assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build FAILED (exit code %ERRORLEVEL%)
    exit /b 1
)

echo.
echo Installing APK to device...
adb install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================================
    echo  Install SUCCESS
    echo  DraftPeek has been installed on your device.
    echo ============================================================
) else (
    echo.
    echo ============================================================
    echo  Install FAILED (exit code %ERRORLEVEL%)
    echo ============================================================
    exit /b 1
)
