<#
  Read-only check of the phone-free Android agent toolchain. Prints what is missing and how to
  install it; never installs or changes anything itself.

  Examples:
    .\tools\agent-doctor.ps1
    .\tools\agent-doctor.ps1 -Json
#>
param(
    [switch]$Json
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

$checks = [System.Collections.Generic.List[object]]::new()

function Add-Check {
    param(
        [string]$Name,
        [ValidateSet('ok', 'missing', 'warn')]
        [string]$Status,
        [string]$Detail,
        [bool]$Required,
        [string]$Fix = ''
    )
    $checks.Add([pscustomobject]@{
            Name = $Name; Status = $Status; Required = $Required; Detail = $Detail; Fix = $Fix
        })
}

function Invoke-Captured {
    param([string]$FilePath, [string[]]$Arguments)
    $lines = Invoke-NativeCommand -FilePath $FilePath -Arguments $Arguments
    return [pscustomobject]@{ ExitCode = $LASTEXITCODE; Lines = @($lines) }
}

function Get-FirstLine {
    param([object[]]$Lines)
    $text = @($Lines | ForEach-Object { "$_".Trim() } | Where-Object { $_ })
    if ($text.Count -gt 0) { return $text[0] }
    return ''
}

# JDK: AGP 9 needs 17+.
$java = Get-Command java -ErrorAction SilentlyContinue
if ($java) {
    $versionLine = Get-FirstLine (Invoke-Captured $java.Source @('-version')).Lines
    $major = if ($versionLine -match '"(?:1\.)?(\d+)') { [int]$Matches[1] } else { 0 }
    if ($major -ge 17) {
        Add-Check 'JDK 17+' 'ok' $versionLine $true
    }
    else {
        Add-Check 'JDK 17+' 'missing' $versionLine $true 'winget install Microsoft.OpenJDK.21'
    }
}
else {
    Add-Check 'JDK 17+' 'missing' 'java not on PATH' $true 'winget install Microsoft.OpenJDK.21'
}

$sdk = $null
try {
    $sdk = Get-AndroidSdkRoot
    Add-Check 'Android SDK' 'ok' $sdk $true
}
catch {
    Add-Check 'Android SDK' 'missing' $_.Exception.Message $true 'Set ANDROID_HOME, then: android sdk install platform-tools'
}

try {
    Add-Check 'adb' 'ok' (Get-AdbPath) $true
}
catch {
    Add-Check 'adb' 'missing' $_.Exception.Message $true 'android sdk install platform-tools'
}

$emulator = $null
try {
    $emulator = Get-EmulatorPath
    Add-Check 'Android Emulator' 'ok' $emulator $true
}
catch {
    Add-Check 'Android Emulator' 'missing' $_.Exception.Message $true 'android sdk install emulator'
}

if ($emulator) {
    $accel = Invoke-Captured $emulator @('-accel-check')
    $accelText = ($accel.Lines -join ' ') -replace '\s+', ' '
    if ($accel.ExitCode -eq 0) {
        Add-Check 'Hardware acceleration' 'ok' $accelText.Trim() $true
    }
    else {
        Add-Check 'Hardware acceleration' 'missing' $accelText.Trim() $true `
            'Windows: enable "Windows Hypervisor Platform" in Windows Features and reboot (admin).'
    }

    $avds = @((Invoke-Captured $emulator @('-list-avds')).Lines | ForEach-Object { $_.Trim() } |
            Where-Object { $_ -and $_ -notmatch '^(INFO|WARNING|ERROR)\b' })
    if ($avds.Count -gt 0) {
        Add-Check 'Virtual device (AVD)' 'ok' ($avds -join ', ') $true
    }
    else {
        Add-Check 'Virtual device (AVD)' 'missing' 'no AVDs' $true 'android emulator create medium_phone'
    }
}

$androidCli = Get-Command android -ErrorAction SilentlyContinue
if ($androidCli) {
    $version = Get-FirstLine ((Invoke-Captured $androidCli.Source @('--no-metrics', '--version')).Lines |
            Where-Object { $_ -match '^\d+\.\d+' })
    Add-Check 'Android CLI' 'ok' "$version ($($androidCli.Source))" $false
}
else {
    Add-Check 'Android CLI' 'warn' 'android not on PATH' $false 'winget install Google.AndroidCLI'
}

$maestro = Get-Command maestro -ErrorAction SilentlyContinue
if ($maestro) {
    Add-Check 'Maestro CLI' 'ok' $maestro.Source $false
}
else {
    Add-Check 'Maestro CLI' 'warn' 'maestro not on PATH' $false `
        'Download maestro.zip from github.com/mobile-dev-inc/maestro/releases, extract, add bin to PATH'
}

$node = Get-Command node -ErrorAction SilentlyContinue
if ($node) {
    Add-Check 'Node.js (optional MCP servers)' 'ok' (Get-FirstLine (Invoke-Captured $node.Source @('--version')).Lines) $false
}
else {
    Add-Check 'Node.js (optional MCP servers)' 'warn' 'node not on PATH' $false 'winget install OpenJS.NodeJS.LTS'
}

$failed = @($checks | Where-Object { $_.Required -and $_.Status -ne 'ok' })

if ($Json) {
    [pscustomobject]@{ ready = ($failed.Count -eq 0); checks = $checks } | ConvertTo-Json -Depth 4
}
else {
    foreach ($check in $checks) {
        $color = switch ($check.Status) { 'ok' { 'Green' } 'warn' { 'Yellow' } default { 'Red' } }
        $label = $check.Status.ToUpperInvariant().PadRight(7)
        Write-Host "[$label] $($check.Name): $($check.Detail)" -ForegroundColor $color
        if ($check.Status -ne 'ok' -and $check.Fix) {
            Write-Host "          fix: $($check.Fix)"
        }
    }
    if ($failed.Count -eq 0) {
        Write-Host 'Emulator toolchain is ready.' -ForegroundColor Green
    }
}

if ($failed.Count -gt 0) { exit 1 }
