@echo off
REM Batch：Windows 批处理脚本，用于自动化任务。
REM 本示例覆盖：变量、条件、循环、标签/子程序、文件操作、
REM   字符串操作、算术、重定向、管道、CHOICE 交互。

setlocal enabledelayedexpansion     ! 启用延迟变量展开，允许在循环中使用 !var!

REM ── 1. 变量 ────────────────────────────────────────────
REM set 设置变量，%var% 引用变量
set USERNAME=Alice
set AGE=30
set PI=3.14159

echo Hello, %USERNAME%! Age: %AGE%

REM 变量拼接
set GREETING=Hello %USERNAME%, you are %AGE% years old.
echo %GREETING%

REM ── 2. 条件判断 ────────────────────────────────────────
REM if 的三种形式：比较、文件测试、错误码
REM 比较运算符：EQU NEQ LSS LEQ GTR GEQ（必须大写）
if %AGE% GEQ 18 (
    echo Adult
) else if %AGE% GEQ 13 (
    echo Teenager
) else (
    echo Child
)

REM 字符串比较（==）
if "%USERNAME%"=="Alice" echo This is Alice
if not "%USERNAME%"=="Bob" echo Not Bob

REM 文件/目录测试
if exist "C:\Windows\notepad.exe" (
    echo Found notepad.exe
) else (
    echo File not found
)

REM 检查变量是否已定义
if defined USERNAME echo USERNAME is defined

REM ── 3. 循环 ────────────────────────────────────────────
REM for 循环遍历列表
set TAGS=admin user editor guest
for %%t in (%TAGS%) do (
    echo Tag: %%t
)

REM for /L 计数循环：for /L %%var in (start,step,end)
for /L %%i in (1, 1, 5) do (
    echo Number: %%i
)

REM for /F 解析文件/命令输出
REM for /F "tokens=*" %%a in ('dir /b') do echo File: %%a

REM ── 4. 延迟变量展开（!var!） ────────────────────────────
REM 在循环中修改变量并立即使用，必须用 !var! 而非 %var%
set /a COUNTER=0
for /L %%i in (1, 1, 5) do (
    set /a COUNTER=!COUNTER! + 1
    echo Step !COUNTER!
)
echo Final counter: %COUNTER%

REM ── 5. 标签与子程序（函数模拟） ────────────────────────
REM call :label 调用子程序，goto :eof 返回
call :greet "Alice"

REM 延迟调用
call :greet_with_delay "World" 2

goto :skip_subroutines          ! 跳过子程序定义区域

:greet
    setlocal
    set "name=%~1"              ! %1 参数，%~1 去掉外层引号
    echo Hello, %name%!
    endlocal
    goto :eof                   ! 返回调用处

:greet_with_delay
    setlocal
    set "name=%~1"
    set "sec=%~2"
    echo Waiting %sec% seconds for %name%...
    timeout /t %sec% /nobreak >nul
    echo Hello, %name%! (delayed)
    endlocal
    goto :eof

:skip_subroutines

REM ── 6. 算术运算 ────────────────────────────────────────
REM set /a 执行算术运算
set /a SUM=5 + 3
set /a PRODUCT=10 * 5
set /a DIVIDED=10 / 3
set /a MODULO=10 %% 3
echo Sum=%SUM%, Product=%PRODUCT%, Divided=%DIVIDED%, Mod=%MODULO%

REM 复合赋值
set /a NUM=5
set /a NUM+=10
echo NUM after +=10: %NUM%

REM ── 7. 字符串操作 ──────────────────────────────────────
set STR=Hello World

REM 子串提取：%var:~start,length%
echo First 5 chars: %STR:~0,5%       ! Hello

REM 从末尾截取：负索引
echo Last 5 chars: %STR:~-5%         ! World

REM 字符串替换：%var:search=replace%
set STR2=%STR:World=DraftPeek%
echo Replaced: %STR2%

REM 字符串长度（无内置函数，变通方案）
call :strlen STR
echo Length of "%STR%": %STRLEN%

REM ── 8. 文件操作与重定向 ────────────────────────────────
REM >  覆盖写入
REM >> 追加写入
REM 2> 重定向错误
echo Hello Batch > output.txt
echo World >> output.txt
type output.txt

REM 管道 |：前一个命令的输出传给后一个命令
dir /b | find ".txt" >nul

REM 读取文件每行
for /F "tokens=*" %%a in (output.txt) do (
    echo Read line: %%a
)

REM ── 9. 用户交互 ────────────────────────────────────────
REM set /p 获取用户输入
set /p INPUT=Enter your name: 
echo You entered: %INPUT%

REM CHOICE 命令（适用于 Windows Vista+）
choice /c YNC /m "Continue? (Y=Yes, N=No, C=Cancel)"
if errorlevel 3 echo Cancel
if errorlevel 2 echo No
if errorlevel 1 echo Yes

REM ── 10. 错误处理与退出码 ───────────────────────────────
REM errorlevel 存储上一条命令的退出码
dir nonexistent.txt 2>nul
if errorlevel 1 (
    echo Command failed with errorlevel %errorlevel%
)

REM 设置退出码
exit /b 0                         ! 成功退出

REM ── 11. 环境变量操作 ───────────────────────────────────
REM 读取环境变量
echo User home: %USERPROFILE%
echo Computer name: %COMPUTERNAME%

REM 临时修改环境变量（仅当前会话）
set PATH=%PATH%;C:\MyTools

REM ── 12. 实用技巧 ───────────────────────────────────────
REM 隐藏命令回显
@echo off

REM 获取当前日期时间
echo Current date: %DATE%
echo Current time: %TIME%

REM 创建文件夹（如果不存在）
if not exist "temp" mkdir temp

REM 删除文件夹
rmdir /s /q temp

REM 复制文件
REM copy source destination
REM 移动文件
REM move source destination
REM 删除文件
REM del filename

REM 从注册表读取
REM reg query "HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer"

echo Batch script completed
exit /b 0

REM ── 字符串长度函数 ─────────────────────────────────────
:strlen
    setlocal enabledelayedexpansion
    set "s=!%~1!"
    if not defined s (endlocal & set STRLEN=0 & goto :eof)
    set "len=0"
    :strlen_loop
    if defined s (
        set "s=%s:~1%"
        set /a len+=1
        goto :strlen_loop
    )
    endlocal & set STRLEN=%len%
    goto :eof
