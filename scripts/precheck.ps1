# precheck.ps1 - push precheck (DraftPeek Android/Kotlin)
param([switch]$Full)
$ErrorActionPreference = 'Continue'
$script:failed = $false

function Step($name, [scriptblock]$cmd) {
  Write-Host "==> $name"
  & $cmd
  if ($LASTEXITCODE -ne 0) { Write-Host "[FAIL] $name" -ForegroundColor Red; $script:failed = $true }
  else { Write-Host "[PASS] $name" -ForegroundColor Green }
}
function WarnSkip($name, $why) {
  Write-Host "==> $name [SKIP] ($why)" -ForegroundColor Yellow
}

if (Test-Path gradlew.bat) {
  Step "gradlew wrapper check" { .\gradlew.bat --version }
} else {
  WarnSkip "gradlew wrapper check" "no gradlew.bat"
}

if (Test-Path gradlew.bat) {
  Step "compileDebugKotlin" { .\gradlew.bat :core:common:compileDebugKotlin --no-daemon -q }
} else {
  WarnSkip "compileDebugKotlin" "no gradlew.bat"
}

if (Test-Path gradlew.bat) {
  Step "lintDebug" { .\gradlew.bat lintDebug --no-daemon -q }
} else {
  WarnSkip "lintDebug" "no gradlew.bat"
}

if ($Full -and (Test-Path gradlew.bat)) {
  Step "testDebugUnitTest" { .\gradlew.bat testDebugUnitTest --no-daemon -q }
} else {
  WarnSkip "testDebugUnitTest" "not Full mode"
}

if ($script:failed) { Write-Host "`nPrecheck failed" -ForegroundColor Red; exit 1 }
Write-Host "`nPrecheck passed" -ForegroundColor Green
exit 0
