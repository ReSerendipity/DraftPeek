# PowerShell：Windows 系统管理和 DevOps 自动化核心语言，跨平台支持 Linux/macOS。
# 本示例覆盖：变量、条件、循环、函数、管道、对象操作、
#   错误处理、哈希表、正则、文件操作、远程执行、类、模块。

# ── 1. 变量 ────────────────────────────────────────────
# $ 前缀声明变量，类型自动推断
$username = "Alice"
$age = 30
$pi = [math]::PI                  # 静态成员访问：:: 操作符
$isAdmin = $true

Write-Host "Hello, $username! Age: $age"
Write-Host "PI: $pi"

# 环境变量：$env:VARNAME
Write-Host "Home: $env:USERPROFILE"

# 变量类型强制转换
[string]$strNum = 42             # 显式转字符串
[int]$intVal = "100"             # 字符串转整数

# ── 2. 数组与集合 ─────────────────────────────────────
$tags = @("admin", "user", "editor")   # @() 创建数组
$tags += "guest"                       # 追加元素
Write-Host "First: $($tags[0])"        # $( ) 子表达式
Write-Host "Count: $($tags.Count)"

# 范围运算符 ..
$range = 1..10                         # 1,2,3,...,10
Write-Host "Range: $range"

# ArrayList（可变大小）
$list = [System.Collections.ArrayList]::new()
$list.Add("item1") | Out-Null
$list.Add("item2") | Out-Null

# ── 3. 条件判断 ────────────────────────────────────────
# -eq 等于, -ne 不等, -gt >, -ge >=, -lt <, -le <=
if ($age -ge 18) {
    Write-Host "Adult"
} elseif ($age -ge 13) {
    Write-Host "Teenager"
} else {
    Write-Host "Child"
}

# 字符串比较：-eq, -ne, -like (通配符), -match (正则)
if ($username -like "A*") { Write-Host "以A开头" }
if ($username -match "^A\w+$") { Write-Host "正则匹配成功" }

# 集合包含：-contains, -in
if ($tags -contains "admin") { Write-Host "包含 admin" }
if ("admin" -in $tags) { Write-Host "admin 在集合中" }   # 逆向写法

# 文件/目录测试
if (Test-Path "C:\Windows") { Write-Host "Windows目录存在" }

# ── 4. 循环 ────────────────────────────────────────────
# foreach 循环
foreach ($tag in $tags) {
    Write-Host "Tag: $tag"
}

# ForEach-Object（管道版本）
$tags | ForEach-Object { Write-Host "Pipeline: $_" }  # $_ 当前对象

# for 循环
for ($i = 1; $i -le 5; $i++) {
    Write-Host -NoNewline "$i "
}
Write-Host ""

# while 循环
$count = 3
while ($count -gt 0) {
    Write-Host "while: $count"
    $count--
}

# do-while 循环（至少执行一次）
$n = 0
do {
    $n++
} while ($n -lt 3)

# ── 5. 管道与对象操作 ─────────────────────────────────
# PowerShell 管道传递的是对象，不是文本
$numbers = 1..10
$evens = $numbers | Where-Object { $_ % 2 -eq 0 }     # 过滤
$doubled = $numbers | ForEach-Object { $_ * 2 }        # 映射

Write-Host "Evens: $($evens -join ', ')"
Write-Host "Doubled: $($doubled -join ', ')"

# Select-Object：选取属性/前N个
Get-Process | Select-Object -First 3 -Property Name, CPU

# Sort-Object：排序
$numbers | Sort-Object -Descending | Select-Object -First 3

# Group-Object：分组
@("a", "ab", "abc", "b") | Group-Object { $_.Length }

# Measure-Object：计数/求和/平均值
$numbers | Measure-Object -Sum -Average -Minimum -Maximum

# ── 6. 哈希表（Hashtable） ─────────────────────────────
$user = @{
    Name = "Alice"
    Age  = 30
    Tags = @("PS", "DevOps")
}
Write-Host $user.Name
$user["Location"] = "Beijing"          # 添加键
$user.Remove("Tags")                   # 删除键

# 遍历哈希表
foreach ($key in $user.Keys) {
    Write-Host "$key = $($user[$key])"
}

# PSCustomObject（结构化对象）
$person = [PSCustomObject]@{
    Name = "Bob"
    Age  = 25
    IsAdmin = $false
}
$person | Format-List                  # 列表显示

# ── 7. 函数 ────────────────────────────────────────────
function Get-UserInfo {
    param(
        [Parameter(Mandatory = $true)] # 必填参数
        [string]$Name,

        [ValidateRange(0, 150)]        # 参数验证
        [int]$Age = 18,

        [switch]$Verbose               # 开关参数
    )

    if ($Verbose) { Write-Host "获取 $Name 的信息..." }

    # 返回对象
    return [PSCustomObject]@{
        Name    = $Name
        Age     = $Age
        IsAdult = $Age -ge 18
    }
}

$result = Get-UserInfo -Name "Bob" -Age 25 -Verbose
$result | Format-Table -AutoSize

# ── 8. 错误处理 ────────────────────────────────────────
try {
    $riskyResult = 10 / 0                # 除以零
} catch [System.DivideByZeroException] {
    Write-Host "除以零错误: $($_.Exception.Message)"
} catch {
    Write-Host "其他错误: $_"
} finally {
    Write-Host "finally 总是执行"
}

# -ErrorAction 控制错误行为
# SilentlyContinue: 静默, Stop: 抛异常, Continue: 显示但继续, Ignore: 忽略
Get-Item "nonexistent.txt" -ErrorAction SilentlyContinue

# ── 9. 文件操作 ────────────────────────────────────────
# 创建/写入文件
"Hello PowerShell" | Out-File -FilePath "output.txt" -Encoding UTF8
"追加一行" | Add-Content -Path "output.txt"

# 读取文件
$content = Get-Content -Path "output.txt" -Raw   # -Raw 读取整个文件
Write-Host "文件内容: $content"

# 遍历每一行
Get-Content "output.txt" | ForEach-Object { Write-Host "行: $_" }

# 文件信息
$fileInfo = Get-Item "output.txt"
Write-Host "大小: $($fileInfo.Length) 字节"

# 删除文件
Remove-Item "output.txt" -Force

# ── 10. JSON / CSV 操作 ────────────────────────────────
$data = @(
    @{ Name = "Alice"; Age = 30 }
    @{ Name = "Bob";   Age = 25 }
)

# 转为 JSON
$json = $data | ConvertTo-Json
Write-Host "JSON: $json"

# JSON 转对象
$obj = $json | ConvertFrom-Json
Write-Host "First: $($obj[0].Name)"

# CSV
$data | Export-Csv -Path "users.csv" -NoTypeInformation
$csvData = Import-Csv -Path "users.csv"

# ── 11. 正则表达式 ─────────────────────────────────────
$text = "Email: alice@example.com, Phone: 123-4567"
if ($text -match "Email: (\S+@\S+)") {
    Write-Host "Matched: $($Matches[1])"   # 捕获组
}

# -replace 替换
$cleanText = $text -replace "\d{3}-\d{4}", "***-****"
Write-Host "替换后: $cleanText"

# ── 12. 多线程与后台任务 ───────────────────────────────
# Start-Job 后台任务
$job = Start-Job -ScriptBlock {
    Start-Sleep -Seconds 1
    return "后台任务完成"
}
# 等待并接收结果
$job | Wait-Job | Out-Null
$jobResult = $job | Receive-Job
$job | Remove-Job
Write-Host "Job 结果: $jobResult"

# ── 13. 作用域与脚本块 ────────────────────────────────
$global:globalVar = "全局变量"          # 显式全局作用域
$script:scriptVar = "脚本变量"          # 脚本作用域（脚本级别）

# ScriptBlock：可延迟执行的代码块
$action = { param($x, $y) $x + $y }
Write-Host "ScriptBlock: $(& $action 3 4)"   # & 调用操作符

# ── 14. 远程执行（WinRM） ──────────────────────────────
# Invoke-Command -ComputerName "Server01" -ScriptBlock { Get-Service }
# Enter-PSSession -ComputerName "Server01"

# ── 15. 注册表操作 ─────────────────────────────────────
# Get-ItemProperty -Path "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion"
# Set-ItemProperty -Path "HKLM:..." -Name "Key" -Value "Value"

Write-Host "PowerShell 示例完成"
