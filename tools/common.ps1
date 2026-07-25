# Shared helpers for the workout-tracker phone/ADB tooling.
# Dot-source this from the other scripts:  . "$PSScriptRoot\common.ps1"

$ErrorActionPreference = "Stop"

# Locate adb.exe: ANDROID_HOME / ANDROID_SDK_ROOT, PATH, then common install dirs.
function Get-Adb {
    foreach ($root in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)) {
        if ($root) {
            $p = Join-Path $root "platform-tools\adb.exe"
            if (Test-Path $p) { return $p }
        }
    }
    $onPath = Get-Command adb -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }

    $candidates = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "${env:ProgramFiles(x86)}\Android\android-sdk\platform-tools\adb.exe",
        "$env:ProgramFiles\Android\android-sdk\platform-tools\adb.exe",
        "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    )
    foreach ($c in $candidates) { if ($c -and (Test-Path $c)) { return $c } }

    throw "adb.exe not found. Set ANDROID_HOME or install Android platform-tools."
}

# Return the state of the first attached device: 'device', 'unauthorized', 'offline', or $null.
function Get-DeviceState {
    param([string]$Adb = (Get-Adb))
    $lines = & $Adb devices | Select-Object -Skip 1 | Where-Object { $_.Trim() }
    if (-not $lines) { return $null }
    $first = ($lines | Select-Object -First 1) -split "\s+"
    return $first[1]
}

# Ensure exactly one authorized device is connected; give actionable guidance otherwise.
function Assert-Device {
    param([string]$Adb = (Get-Adb))
    $state = Get-DeviceState -Adb $Adb
    switch ($state) {
        "device"       { return }
        "unauthorized" { throw "Phone connected but UNAUTHORIZED. On the phone, accept the 'Allow USB debugging?' prompt (tick 'Always allow'), then rerun." }
        "offline"      { throw "Device is offline. Replug USB or run: `"$Adb`" kill-server; `"$Adb`" start-server" }
        $null          { throw "No device attached. Connect the phone over USB with USB debugging enabled." }
        default        { throw "Unexpected device state: '$state'." }
    }
}

# Find the package id of an installed app by a name fragment (case-insensitive).
function Find-Package {
    param([Parameter(Mandatory)][string]$Fragment, [string]$Adb = (Get-Adb))
    (& $Adb shell pm list packages) `
        -split "`r?`n" `
        | ForEach-Object { $_ -replace '^package:', '' } `
        | Where-Object { $_ -match [regex]::Escape($Fragment) }
}
