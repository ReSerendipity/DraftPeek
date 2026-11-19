# =============================================================================
# check_r1_save_regression.ps1 — R1（富文本保存丢失）端到端保存回归检查
# =============================================================================
# 用 adb 驱动模拟器完成一次真实用户路径：
#   新建 Markdown 文件 → 编辑模式输入内容 → 切富文本追加内容 → 切预览
#   → 保存 → 强制停止 App → 重新打开 → 重新打开该文件 → 读回磁盘内容
# 断言保存后的磁盘内容与重开后一致，且包含键入的全部标记；失败时输出卡住步骤。
#
# 前置条件：
#   - 已连接且启动完成的模拟器/设备（默认 emulator-5554），adb 在 PATH 中
#   - 目标包必须是 debuggable 构建（脚本用 run-as 读内部存储断言内容）；
#     release 包在模拟器上会被 AntiDebug 判定 SUSPICIOUS 并禁用写入，无法用于本检查。
#     构建示例：./gradlew.bat :app:assembleProductionDebug
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts/check_r1_save_regression.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/check_r1_save_regression.ps1 `
#       -Apk app/build/outputs/apk/production/debug/app-production-debug.apk
#   可选参数：-Serial（默认 emulator-5554）、-Apk（安装/覆盖安装后再测）、
#             -TimeoutSec（单步等待上限，默认 20）、-ArtifactDir（失败截图目录）
#
# 退出码：0 = PASS；1 = FAIL（日志含失败步骤与最后一个界面快照路径）
# =============================================================================

[CmdletBinding()]
param(
    [string]$Serial = "emulator-5554",
    [string]$Apk = "",
    [int]$TimeoutSec = 20,
    [string]$ArtifactDir = ""
)

$ErrorActionPreference = "Stop"
$Pkg = "com.draftpeek"
$script:StepNo = 0
$script:LastXml = ""
$script:FileName = ""
$script:ExpectedContent = ""

if ([string]::IsNullOrWhiteSpace($ArtifactDir)) {
    $ArtifactDir = Join-Path (Get-Location) "build\r1-save-regression"
}
if (-not (Test-Path $ArtifactDir)) { New-Item -ItemType Directory -Path $ArtifactDir -Force | Out-Null }

function Write-Info([string]$Message) { Write-Host "[INFO] $Message" -ForegroundColor Cyan }
function Write-Ok([string]$Message)   { Write-Host "[ OK ] $Message" -ForegroundColor Green }
function Write-Err([string]$Message)  { Write-Host "[FAIL] $Message" -ForegroundColor Red }

function Invoke-Adb {
    param([string[]]$AdbArgs, [switch]$AllowFail)
    $output = & adb -s $Serial @AdbArgs 2>&1
    $code = $LASTEXITCODE
    $text = ($output | Out-String).Trim()
    if ($code -ne 0 -and -not $AllowFail) {
        throw "adb $($AdbArgs -join ' ') 失败（exit=$code）：$text"
    }
    return [pscustomobject]@{ Code = $code; Text = $text }
}

function Get-UiXml {
    $dump = Invoke-Adb @("shell", "uiautomator", "dump", "/sdcard/r1_ui.xml") -AllowFail
    if ($dump.Code -ne 0) { throw "uiautomator dump 失败：$($dump.Text)" }
    $local = Join-Path $env:TEMP "r1_ui.xml"
    $pull = Invoke-Adb @("pull", "/sdcard/r1_ui.xml", $local) -AllowFail
    if ($pull.Code -ne 0) { throw "拉取 UI dump 失败：$($pull.Text)" }
    $raw = [System.IO.File]::ReadAllText($local)
    # uiautomator 对 & < > 做 XML 转义；解码后再按可见文本匹配
    $script:LastXml = $raw.Replace("&amp;", "&").Replace("&lt;", "<").Replace("&gt;", ">").Replace("&quot;", '"')
    return $script:LastXml
}

# 若系统 ANR 弹窗挡住界面（慢速模拟器首次启动常见），点 Wait 继续
function Dismiss-AnrIfPresent([string]$Xml) {
    if ($Xml -match "isn't responding" -or $Xml -match "没有响应") {
        $wait = Find-Nodes -Xml $Xml -Value "Wait"
        if ($wait.Count -eq 0) { $wait = Find-Nodes -Xml $Xml -Value "等待" }
        if ($wait.Count -gt 0) {
            Tap-Node $wait[0]
            Start-Sleep -Milliseconds 600
        }
    }
}

# 返回所有匹配节点的屏幕坐标（bounds 中心点）；Attr 可选 text / content-desc
function Find-Nodes {
    param([string]$Xml, [string]$Value, [string]$Attr = "text")
    $escaped = [regex]::Escape($Value)
    $pattern = '<node[^>]*' + $Attr + '="' + $escaped + '"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    $result = @()
    foreach ($m in [regex]::Matches($Xml, $pattern)) {
        $l = [int]$m.Groups[1].Value; $t = [int]$m.Groups[2].Value
        $r = [int]$m.Groups[3].Value; $b = [int]$m.Groups[4].Value
        $result += [pscustomobject]@{
            X = [int](($l + $r) / 2); Y = [int](($t + $b) / 2)
            L = $l; T = $t; R = $r; B = $b
        }
    }
    return $result
}

function Find-ClassNode {
    param([string]$Xml, [string]$Class)
    $escaped = [regex]::Escape($Class)
    $pattern = '<node[^>]*class="' + $escaped + '"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    $m = [regex]::Match($Xml, $pattern)
    if (-not $m.Success) { return $null }
    $l = [int]$m.Groups[1].Value; $t = [int]$m.Groups[2].Value
    $r = [int]$m.Groups[3].Value; $b = [int]$m.Groups[4].Value
    return [pscustomobject]@{ X = [int](($l + $r) / 2); Y = [int](($t + $b) / 2); L = $l; T = $t; R = $r; B = $b }
}

function Tap-Point([int]$X, [int]$Y) {
    Invoke-Adb @("shell", "input", "tap", "$X", "$Y") | Out-Null
}

function Tap-Node($Node) {
    if ($null -eq $Node) { throw "目标节点不存在，无法点击" }
    Tap-Point $Node.X $Node.Y
}

# 等待某文本/内容描述出现，返回节点；超时返回 $null
function Wait-Node {
    param([string]$Value, [string]$Attr = "text", [int]$Seconds = -1, [scriptblock]$Filter = $null)
    if ($Seconds -lt 0) { $Seconds = $TimeoutSec }
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        $xml = Get-UiXml
        Dismiss-AnrIfPresent $xml
        $nodes = Find-Nodes -Xml $xml -Value $Value -Attr $Attr
        if ($Filter) { $nodes = @($nodes | Where-Object $Filter) }
        if ($nodes.Count -gt 0) { return $nodes[0] }
        Start-Sleep -Milliseconds 700
    } while ((Get-Date) -lt $deadline)
    return $null
}

function Wait-AnyNode {
    param([string[]]$Values, [string]$Attr = "text", [int]$Seconds = -1)
    if ($Seconds -lt 0) { $Seconds = $TimeoutSec }
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        $xml = Get-UiXml
        Dismiss-AnrIfPresent $xml
        foreach ($v in $Values) {
            $nodes = Find-Nodes -Xml $xml -Value $v -Attr $Attr
            if ($nodes.Count -gt 0) { return [pscustomobject]@{ Value = $v; Node = $nodes[0] } }
        }
        Start-Sleep -Milliseconds 700
    } while ((Get-Date) -lt $deadline)
    return $null
}

function Type-Ascii([string]$Text) {
    # adb input text 用 %s 表示空格；本脚本只使用字母/数字/下划线/空格
    $encoded = $Text -replace " ", "%s"
    Invoke-Adb @("shell", "input", "text", $encoded) | Out-Null
}

function Save-Diagnostic([string]$Tag) {
    try {
        $name = "r1_fail_$Tag.png"
        $remote = "/sdcard/$name"
        Invoke-Adb @("shell", "screencap", "-p", $remote) -AllowFail | Out-Null
        Invoke-Adb @("pull", $remote, (Join-Path $ArtifactDir $name)) -AllowFail | Out-Null
        $xmlPath = Join-Path $ArtifactDir "r1_fail_$Tag.xml"
        $script:LastXml | Set-Content -Path $xmlPath -Encoding UTF8
        Write-Err "诊断快照：$(Join-Path $ArtifactDir $name) ／ $xmlPath"
    } catch {
        Write-Err "诊断快照保存失败：$($_.Exception.Message)"
    }
}

function Step {
    param([string]$Name, [scriptblock]$Body)
    $script:StepNo++
    Write-Host ("[STEP {0}] {1} ..." -f $script:StepNo, $Name)
    try {
        & $Body
        Write-Ok ("[STEP {0}] {1}" -f $script:StepNo, $Name)
    } catch {
        Write-Err ("[STEP {0}] {1} — {2}" -f $script:StepNo, $Name, $_.Exception.Message)
        throw
    }
}

function Get-RemoteFileContent([string]$Name) {
    $cat = Invoke-Adb @("shell", "run-as", $Pkg, "cat", "files/user_files/$Name")
    return $cat.Text
}

# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------
Write-Host "=== DraftPeek R1 保存回归检查 ===" -ForegroundColor Cyan
Write-Host "device=$Serial  package=$Pkg"

try {
    Step "检查设备与系统启动状态" {
        $devices = (& adb devices 2>&1 | Out-String)
        if ($devices -notmatch [regex]::Escape($Serial)) {
            throw "未找到设备 $Serial。当前 adb devices：`n$devices"
        }
        $boot = (Invoke-Adb @("shell", "getprop", "sys.boot_completed")).Text
        if ($boot -ne "1") { throw "设备尚未启动完成（sys.boot_completed=$boot）" }
        $props = Invoke-Adb @("shell", "getprop", "ro.build.version.release")
        Write-Info "Android $($props.Text)"
    }

    if (-not [string]::IsNullOrWhiteSpace($Apk)) {
        Step "安装待测 APK：$Apk" {
            if (-not (Test-Path $Apk)) { throw "APK 不存在：$Apk" }
            $installed = Invoke-Adb @("install", "-r", $Apk) -AllowFail
            if ($installed.Code -ne 0) {
                Write-Info "覆盖安装失败（可能签名不一致），先卸载再安装"
                Invoke-Adb @("uninstall", $Pkg) -AllowFail | Out-Null
                $retry = Invoke-Adb @("install", $Apk)
                if ($retry.Code -ne 0) { throw "安装失败：$($retry.Text)" }
            }
            Write-Info ($installed.Text + " " + $installed.Code)
        }
    }

    Step "确认目标包为 debuggable 构建（run-as 可用）" {
        $runAs = Invoke-Adb @("shell", "run-as", $Pkg, "true") -AllowFail
        if ($runAs.Code -ne 0) {
            throw "run-as 不可用。release 包在模拟器上会被安全门控禁用写入，无法做保存回归；请安装 debuggable 包（:app:assembleProductionDebug）"
        }
        $ver = Invoke-Adb @("shell", "dumpsys", "package", $Pkg) -AllowFail
        $m = [regex]::Match($ver.Text, "versionName=([^\s]+)")
        if ($m.Success) { Write-Info "已安装版本：$($m.Groups[1].Value)" }
    }

    Step "冷启动应用（force-stop 后重新拉起）" {
        Invoke-Adb @("shell", "am", "force-stop", $Pkg) | Out-Null
        Start-Sleep -Milliseconds 600
        Invoke-Adb @("shell", "monkey", "-p", $Pkg, "-c", "android.intent.category.LAUNCHER", "1") -AllowFail | Out-Null
        $entry = Wait-AnyNode -Values @("New file", "Agree & Start", "Skip", "Create and open") -Seconds 30
        if (-not $entry) { throw "启动 30s 内未出现可操作界面（未找到 New file / 首次启动引导）" }
        Write-Info "首屏命中：$($entry.Value)"
    }

    Step "处理首次启动引导与条款（如出现）" {
        $xml = Get-UiXml
        $skip = Find-Nodes -Xml $xml -Value "Skip"
        if ($skip.Count -gt 0) {
            Tap-Node $skip[0]
            Start-Sleep -Seconds 1
            $xml = Get-UiXml
        }
        $agree = Find-Nodes -Xml $xml -Value "Agree & Start"
        if ($agree.Count -eq 0) { Write-Info "无需处理（已有用户数据）"; return }
        $checkbox = Find-Nodes -Xml $xml -Value "I have read and agree to the User Agreement, Privacy Policy and the terms above"
        if ($checkbox.Count -gt 0) { Tap-Node $checkbox[0]; Start-Sleep -Milliseconds 400 }
        $agree = Find-Nodes -Xml (Get-UiXml) -Value "Agree & Start"
        if ($agree.Count -eq 0) { throw "同意条款按钮消失，无法继续" }
        Tap-Node $agree[0]
        $ok = Wait-Node -Value "New file" -Seconds 15
        if (-not $ok) { throw "同意条款后未回到文件页（未出现 New file）" }
    }

    Step "进入文件页并打开新建文件对话框" {
        $xml = Get-UiXml
        if ((Find-Nodes -Xml $xml -Value "New file").Count -eq 0) {
            $filesTab = Find-Nodes -Xml $xml -Value "Files" | Where-Object { $_.T -gt 2000 }
            if ($filesTab.Count -eq 0) { throw "找不到底部 Files 标签" }
            Tap-Node $filesTab[0]
            Start-Sleep -Seconds 1
        }
        $newFile = Wait-Node -Value "New file" -Seconds 10
        if (-not $newFile) { throw "找不到 New file 入口" }
        Tap-Node $newFile
        if (-not (Wait-Node -Value "File name" -Seconds 10)) { throw "新建文件对话框未出现（未找到 File name）" }
    }

    Step "填写文件名并选择 Markdown" {
        $xml = Get-UiXml
        $field = Find-ClassNode -Xml $xml -Class "android.widget.EditText"
        if (-not $field) { throw "新建文件对话框缺少文件名输入框" }
        Tap-Node $field
        Start-Sleep -Milliseconds 500
        Invoke-Adb @("shell", "input", "keyevent", "123") | Out-Null   # MOVE_END
        for ($i = 0; $i -lt 30; $i++) { Invoke-Adb @("shell", "input", "keyevent", "67") | Out-Null } # DEL
        $script:FileName = "r1_check_" + (Get-Date -Format "yyyyMMdd_HHmmss")
        Type-Ascii $script:FileName
        Start-Sleep -Milliseconds 400
        $md = Wait-Node -Value "MD" -Seconds 5
        if (-not $md) { throw "语言网格中找不到 MD（Markdown）选项" }
        Tap-Node $md
        Start-Sleep -Milliseconds 600
        $after = Get-UiXml
        if (-not [regex]::IsMatch($after, 'text="\.md"')) { throw "选择 Markdown 后扩展名未变为 .md" }
        Write-Info "文件名：$($script:FileName).md"
    }

    Step "创建文件并进入编辑器" {
        Invoke-Adb @("shell", "input", "keyevent", "4") | Out-Null  # 收起 IME
        Start-Sleep -Milliseconds 500
        $create = Wait-Node -Value "Create and open" -Seconds 10
        if (-not $create) { throw "找不到 Create and open 按钮" }
        Tap-Node $create
        $editor = Wait-AnyNode -Values @("富文本", "预览", "编辑") -Seconds 20
        if (-not $editor) { throw "创建后未进入编辑器（未出现模式切换标签）" }
        Write-Info "编辑器已打开（命中模式标签：$($editor.Value)）"
        Start-Sleep -Milliseconds 600
    }

    $token = "R1_SAVE_CHECK_" + (Get-Date -Format "HHmmss")
    $second = "SECOND_LINE"

    Step "编辑模式输入内容：$token" {
        # 编辑器正文区：模式栏下方、工具栏上方
        Tap-Point 540 1100
        Start-Sleep -Milliseconds 500
        Type-Ascii $token
        Invoke-Adb @("shell", "input", "keyevent", "66") | Out-Null  # Enter
        Type-Ascii $second
        Start-Sleep -Milliseconds 900
        $xml = Get-UiXml
        # Sora 原生编辑器可能不向可访问性暴露正文，此处仅作提示；权威断言在保存后的磁盘内容
        if ($xml -notmatch [regex]::Escape($token)) {
            Write-Info "编辑区未暴露文本（原生编辑器可访问性），以富文本/保存后断言为准"
        }
    }

    Step "切换富文本并追加内容（R1 触发路径）" {
        $wysiwyg = Wait-Node -Value "富文本" -Seconds 10
        if (-not $wysiwyg) { throw "找不到 富文本 模式标签" }
        Tap-Node $wysiwyg
        if (-not (Wait-Node -Value "粗体" -Seconds 15)) { throw "富文本编辑器未加载（未出现格式工具栏 粗体）" }
        Tap-Point 540 1100
        Start-Sleep -Milliseconds 600
        Invoke-Adb @("shell", "input", "keyevent", "123") | Out-Null  # MOVE_END
        Type-Ascii "_RICH_EDIT"
        Start-Sleep -Milliseconds 900
        $xml = Get-UiXml
        if ($xml -notmatch "RICH_EDIT") { throw "富文本编辑后界面未出现追加内容 RICH_EDIT" }
    }

    Step "切到预览（保存前视图）" {
        $preview = Wait-Node -Value "预览" -Seconds 10
        if (-not $preview) { throw "找不到 预览 模式标签" }
        Tap-Node $preview
        Start-Sleep -Seconds 1
        $xml = Get-UiXml
        # 预览是 WebView：若可访问性未暴露文本，以保存后的磁盘内容为准
        if ($xml -notmatch [regex]::Escape($token)) {
            Write-Info "预览层未暴露文本（WebView 可访问性），内容一致性以保存后磁盘断言为准"
        }
    }

    Step "保存" {
        $save = Wait-Node -Value "保存" -Attr "content-desc" -Seconds 10
        if (-not $save) { $save = Wait-Node -Value "保存" -Seconds 3 }
        if (-not $save) { throw "找不到保存按钮（content-desc/text 保存）" }
        Tap-Node $save
        Start-Sleep -Seconds 1
        $content = Get-RemoteFileContent "$($script:FileName).md"
        if ([string]::IsNullOrWhiteSpace($content)) { throw "保存后磁盘内容为空（R1：富文本/预览保存未落盘）" }
        if ($content -notmatch [regex]::Escape($token)) { throw "保存后磁盘内容缺少 $token，内容=$content" }
        if ($content -notmatch "RICH_EDIT") { throw "保存后磁盘内容缺少富文本追加内容，内容=$content" }
        $script:ExpectedContent = $content
        Write-Info "已保存内容（$($content.Length) 字符）：$($content -replace "`r?`n", " \n ")"
    }

    Step "强制停止应用" {
        Invoke-Adb @("shell", "am", "force-stop", $Pkg) | Out-Null
        Start-Sleep -Milliseconds 800
        $pid = (Invoke-Adb @("shell", "pidof", $Pkg) -AllowFail).Text
        if (-not [string]::IsNullOrWhiteSpace($pid)) { throw "force-stop 后进程仍存活（pid=$pid）" }
    }

    Step "重新打开应用并找回文件" {
        Invoke-Adb @("shell", "monkey", "-p", $Pkg, "-c", "android.intent.category.LAUNCHER", "1") -AllowFail | Out-Null
        if (-not (Wait-AnyNode -Values @("New file", "编辑", "预览") -Seconds 30)) {
            throw "重开后 30s 内未出现文件页或编辑器"
        }
        $xml = Get-UiXml
        # 路径 A：重开后直接恢复到编辑器
        if ((Find-Nodes -Xml $xml -Value "富文本").Count -gt 0 -or (Find-Nodes -Xml $xml -Value "预览").Count -gt 0) {
            Write-Info "重开后自动恢复到编辑器"
            return
        }
        # 路径 B：文件页 / Recent 列表中找到文件（列表可能省略扩展名）
        $target = Find-Nodes -Xml $xml -Value "$($script:FileName).md"
        if ($target.Count -eq 0) { $target = Find-Nodes -Xml $xml -Value $script:FileName }
        if ($target.Count -eq 0) {
            $recent = Find-Nodes -Xml $xml -Value "Recent"
            if ($recent.Count -gt 0) {
                Tap-Node $recent[0]
                Start-Sleep -Seconds 1
                $xml = Get-UiXml
                $target = Find-Nodes -Xml $xml -Value "$($script:FileName).md"
                if ($target.Count -eq 0) { $target = Find-Nodes -Xml $xml -Value $script:FileName }
            }
        }
        if ($target.Count -eq 0) { throw "重开后找不到文件 $($script:FileName).md（Files/Recent 均未命中）" }
        Tap-Node $target[0]
        if (-not (Wait-AnyNode -Values @("富文本", "预览", "编辑") -Seconds 20)) {
            throw "点击文件后未进入编辑器"
        }
        Start-Sleep -Milliseconds 800
    }

    Step "读取重开后的文件内容并断言一致" {
        $reopened = Get-RemoteFileContent "$($script:FileName).md"
        $normalizedExpected = ($script:ExpectedContent -replace "`r`n", "`n").Trim()
        $normalizedActual = ($reopened -replace "`r`n", "`n").Trim()
        if ($normalizedActual -ne $normalizedExpected) {
            throw "重开后内容与保存前不一致。期望=$normalizedExpected ｜ 实际=$normalizedActual"
        }
        if ($normalizedActual -notmatch "RICH_EDIT") { throw "重开后内容缺少富文本追加内容" }
        $xml = Get-UiXml
        if ($xml -notmatch [regex]::Escape($token)) { throw "重开后的界面未渲染标记 $token" }
        Write-Info "重开后内容（$($normalizedActual.Length) 字符）：$($normalizedActual -replace "`n", " \n ")"
    }

    Write-Host ""
    Write-Host "RESULT: PASS  (R1 保存 → 强停 → 重开 内容一致)" -ForegroundColor Green
    exit 0
} catch {
    Write-Host ""
    Write-Err $_.Exception.Message
    Save-Diagnostic ("step" + $script:StepNo)
    Write-Host "RESULT: FAIL  (卡在 STEP $($script:StepNo))" -ForegroundColor Red
    exit 1
}
