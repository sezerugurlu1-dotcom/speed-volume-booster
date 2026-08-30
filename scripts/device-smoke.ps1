param(
    [ValidateSet('setup', 'manual', 'adaptive', 'stopped')]
    [string]$Phase = 'setup',
    [string]$ApkPath = (Join-Path $PSScriptRoot '..\releases\RoadGain-v1.0.3-debug.apk'),
    [string]$ResultsRoot = (Join-Path $PSScriptRoot '..\device-test-results')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
$adbPath = if ($adbCommand) {
    $adbCommand.Source
} else {
    Join-Path $env:LOCALAPPDATA 'Android\platform-tools\adb.exe'
}
if (-not (Test-Path -LiteralPath $adbPath)) { throw "adb not found: $adbPath" }

$deviceLines = @(& $adbPath devices | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" })
if (@($deviceLines).Count -ne 1) {
    throw "Exactly one authorized Android device is required. Found $(@($deviceLines).Count)."
}
$serial = ($deviceLines[0] -split "\t")[0]
$packageName = 'com.sezeros.speedboost'
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$resultDirectory = Join-Path $ResultsRoot "$timestamp-$Phase-$serial"
New-Item -ItemType Directory -Path $resultDirectory -Force | Out-Null

function Invoke-AdbCapture {
    param([string]$Name, [string[]]$Arguments)
    $output = & $adbPath -s $serial @Arguments 2>&1
    $output | Set-Content -LiteralPath (Join-Path $resultDirectory "$Name.txt") -Encoding utf8
    return $output
}

if ($Phase -eq 'setup') {
    if (-not (Test-Path -LiteralPath $ApkPath)) { throw "APK not found: $ApkPath" }
    & $adbPath -s $serial install -r $ApkPath
    if ($LASTEXITCODE -ne 0) { throw 'APK installation failed.' }

    foreach ($permission in @(
        'android.permission.ACCESS_COARSE_LOCATION',
        'android.permission.ACCESS_FINE_LOCATION',
        'android.permission.POST_NOTIFICATIONS'
    )) {
        & $adbPath -s $serial shell pm grant $packageName $permission 2>$null
    }
    & $adbPath -s $serial shell am start -n "$packageName/.MainActivity"
}

Invoke-AdbCapture 'device' @('shell', 'getprop') | Out-Null
Invoke-AdbCapture 'package' @('shell', 'dumpsys', 'package', $packageName) | Out-Null
Invoke-AdbCapture 'audio' @('shell', 'dumpsys', 'audio') | Out-Null
Invoke-AdbCapture 'location' @('shell', 'dumpsys', 'location') | Out-Null
Invoke-AdbCapture 'services' @('shell', 'dumpsys', 'activity', 'services', $packageName) | Out-Null
Invoke-AdbCapture 'battery' @('shell', 'dumpsys', 'batterystats', $packageName) | Out-Null

$appPid = (& $adbPath -s $serial shell pidof $packageName).Trim()
if ($appPid) {
    Invoke-AdbCapture 'logcat' @('logcat', '-d', '-t', '1000', '--pid', $appPid) | Out-Null
}

$instructions = switch ($Phase) {
    'setup' { 'App installed and opened. Start low-volume music, then run this script with -Phase manual after enabling Manual mode.' }
    'manual' { 'Record audible A/B results for speaker, wired/USB, and Bluetooth. Then enable Adaptive mode and run with -Phase adaptive.' }
    'adaptive' { 'Record GPS response, route changes, screen-off behavior, and GPS loss. Stop the service, then run with -Phase stopped.' }
    'stopped' { 'Confirm the notification disappeared and audio returned to baseline. Add results to compatibility-matrix.md.' }
}
$instructions | Set-Content -LiteralPath (Join-Path $resultDirectory 'NEXT.txt') -Encoding utf8

Write-Output "Captured $Phase evidence: $resultDirectory"
Write-Output $instructions
