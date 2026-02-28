param(
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$Package = "com.dailywell.android",
    [int]$Events = 300,
    [int]$ThrottleMs = 120
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $AdbPath)) {
    throw "ADB not found at '$AdbPath'."
}

$artifactsDir = Join-Path $PSScriptRoot "smoke-artifacts"
New-Item -ItemType Directory -Force -Path $artifactsDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$screenshotRemote = "/sdcard/dailywell_smoke_$timestamp.png"
$screenshotLocal = Join-Path $artifactsDir "dailywell_smoke_$timestamp.png"
$logLocal = Join-Path $artifactsDir "dailywell_logcat_$timestamp.txt"

$devicesRaw = & $AdbPath devices
$connectedDevices = $devicesRaw | Where-Object { $_ -match "device$" -and $_ -notmatch "^List of devices" }
if (-not $connectedDevices) {
    throw "No authorized Android device detected."
}

Write-Output "DEVICE_OK=TRUE"
Write-Output "STEP=PREP"
& $AdbPath logcat -c | Out-Null
& $AdbPath shell input keyevent KEYCODE_WAKEUP | Out-Null
& $AdbPath shell wm dismiss-keyguard | Out-Null
& $AdbPath shell am force-stop $Package | Out-Null

Write-Output "STEP=LAUNCH"
& $AdbPath shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null

Write-Output "STEP=MONKEY"
& $AdbPath shell monkey -p $Package --pct-syskeys 0 --throttle $ThrottleMs -v $Events | Out-Null
$monkeyExit = $LASTEXITCODE
Write-Output "MONKEY_EXIT=$monkeyExit"

Write-Output "STEP=CAPTURE"
& $AdbPath shell screencap -p $screenshotRemote | Out-Null
& $AdbPath pull $screenshotRemote $screenshotLocal | Out-Null

$logcat = & $AdbPath logcat -d -b main -b system -b crash
$logcat | Set-Content -Path $logLocal
$crashLines = $logcat | Select-String -SimpleMatch @(
    "FATAL EXCEPTION",
    "ANR in $Package",
    "Process: $Package"
)

if ($crashLines) {
    Write-Output "CRASH_SCAN=FOUND"
    $crashLines | Select-Object -First 25 | ForEach-Object { Write-Output $_.Line }
    Write-Output "LOG=$logLocal"
    Write-Output "SCREENSHOT=$screenshotLocal"
    exit 2
}

Write-Output "CRASH_SCAN=CLEAN"
Write-Output "LOG=$logLocal"
Write-Output "SCREENSHOT=$screenshotLocal"
