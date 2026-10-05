<#
  One-shot "does it actually run" check for agents: build, install without clearing data, launch,
  detect a crash, then capture a screenshot, the UI hierarchy, and a logcat snapshot. Optionally
  runs Maestro flows. Everything lands in one run folder with a machine-readable result.json.

  Prefers an emulator: when several devices are attached and -Serial is omitted, the single running
  emulator is chosen and physical phones are left alone.

  Examples:
    .\tools\ui-check.ps1 -PackageName com.example.product
    .\tools\ui-check.ps1 -PackageName com.example.product -Flow .maestro
    .\tools\ui-check.ps1 -PackageName com.example.product -NoBuild -Serial emulator-5556
#>
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^[A-Za-z][A-Za-z0-9_.]+$')]
    [string]$PackageName,
    [string]$Serial,
    [ValidatePattern('^[A-Za-z0-9_-]+$')]
    [string]$Module = 'app',
    [switch]$NoBuild,
    # Maestro flow file or folder, relative to -ProjectRoot.
    [string]$Flow,
    [ValidateRange(0, 60)]
    [int]$SettleSeconds = 3,
    [ValidateRange(50, 5000)]
    [int]$LogLines = 400,
    [string]$ProjectRoot,
    [string]$OutputRoot
)

$ErrorActionPreference = 'Stop'
# Defaults that use $PSScriptRoot are resolved here, not in param(): Windows PowerShell 5.1 leaves
# $PSScriptRoot empty inside parameter defaults of an advanced script started with `powershell -File`.
if (-not $ProjectRoot) { $ProjectRoot = Join-Path $PSScriptRoot '..' }
. "$PSScriptRoot\common.ps1"

$repo = (Resolve-Path -LiteralPath $ProjectRoot).Path
if (-not $OutputRoot) { $OutputRoot = Join-Path $repo 'artifacts\ui-check' }
$runDirectory = Join-Path ([System.IO.Path]::GetFullPath($OutputRoot)) (Get-Date -Format 'yyyyMMdd-HHmmss')
[System.IO.Directory]::CreateDirectory($runDirectory) | Out-Null

$adb = Get-AdbPath
if (-not $Serial) {
    $authorized = @(Get-AdbDeviceRecord -Adb $adb | Where-Object { $_.State -eq 'device' })
    $emulators = @($authorized | Where-Object { $_.Serial -match '^emulator-\d+$' })
    if ($emulators.Count -eq 1) { $Serial = $emulators[0].Serial }
}
$device = Get-AuthorizedDeviceSerial -Adb $adb -RequestedSerial $Serial

$result = [ordered]@{
    status = 'running'
    device = $device
    packageName = $PackageName
    runDirectory = $runDirectory
    apk = $null
    screenshot = $null
    layout = $null
    logcat = $null
    maestro = $null
    failure = $null
}

function Save-Result {
    $result | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $runDirectory 'result.json') -Encoding utf8
}

function Save-Logcat {
    param([string]$Name, [string[]]$Arguments)
    $path = Join-Path $runDirectory $Name
    $lines = Invoke-NativeCommand -FilePath $adb -Arguments (@('-s', $device, 'logcat', '-d', '-t', "$LogLines") + $Arguments)
    [System.IO.File]::WriteAllLines($path, [string[]]$lines, [System.Text.UTF8Encoding]::new($false))
    return $path
}

try {
    $apk = Join-Path $repo "$Module\build\outputs\apk\debug\$Module-debug.apk"
    if (-not $NoBuild) {
        $wrapper = Join-Path $repo 'gradlew.bat'
        if (-not (Test-Path -LiteralPath $wrapper)) { throw "Gradle wrapper not found at $wrapper" }
        & $wrapper -p $repo ":${Module}:assembleDebug" --console=plain --quiet
        if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed.' }
    }
    if (-not (Test-Path -LiteralPath $apk)) { throw "Expected APK was not produced: $apk" }
    $result.apk = $apk

    Invoke-NativeCommand -FilePath $adb -Arguments @('-s', $device, 'install', '-r', $apk) `
        -FailureMessage 'ADB install failed. Existing data was not intentionally cleared' | Out-Null

    Invoke-NativeCommand -FilePath $adb -Arguments @('-s', $device, 'logcat', '-c') | Out-Null
    Invoke-NativeCommand -FilePath $adb -Arguments @('-s', $device, 'shell', 'am', 'force-stop', $PackageName) | Out-Null
    Start-AppPackage -Adb $adb -Serial $device -PackageName $PackageName
    Start-Sleep -Seconds $SettleSeconds

    $processId = (@(Invoke-NativeCommand -FilePath $adb -Arguments @('-s', $device, 'shell', 'pidof', $PackageName)) -join ' ').Trim()
    if ($processId -notmatch '^[0-9]+( [0-9]+)*$') {
        $result.logcat = Save-Logcat -Name 'crash-logcat.txt' -Arguments @('-b', 'crash', '-b', 'main')
        $result.screenshot = Save-DeviceScreenshot -Adb $adb -Serial $device -Output (Join-Path $runDirectory 'screen.png')
        throw "$PackageName is not running after launch; it probably crashed. See $($result.logcat)"
    }

    $result.screenshot = Save-DeviceScreenshot -Adb $adb -Serial $device -Output (Join-Path $runDirectory 'screen.png')
    $result.layout = Save-DeviceLayout -Adb $adb -Serial $device -Output (Join-Path $runDirectory 'layout.xml')
    $result.logcat = Save-Logcat -Name 'logcat.txt' -Arguments @('--pid', ($processId -split ' ')[0])

    if ($Flow) {
        $maestro = Get-Command maestro -ErrorAction SilentlyContinue
        if (-not $maestro) { throw 'maestro is not on PATH; install it or omit -Flow.' }
        $flowPath = (Resolve-Path -LiteralPath (Join-Path $repo $Flow)).Path
        $maestroDirectory = Join-Path $runDirectory 'maestro'
        $report = Join-Path $runDirectory 'maestro-junit.xml'
        $env:MAESTRO_CLI_NO_ANALYTICS = '1'
        $env:MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED = 'true'
        $maestroOutput = Invoke-NativeCommand -FilePath $maestro.Source -Arguments @(
            '--device', $device, '--no-ansi', 'test', $flowPath,
            '--format', 'JUNIT', '--output', $report, '--test-output-dir', $maestroDirectory)
        $maestroExit = $LASTEXITCODE
        $maestroOutput | Set-Content -LiteralPath (Join-Path $runDirectory 'maestro.log') -Encoding utf8
        $result.maestro = [ordered]@{
            flow = $flowPath; exitCode = $maestroExit; report = $report; outputDirectory = $maestroDirectory
        }
        if ($maestroExit -ne 0) { throw "Maestro flows failed (exit $maestroExit). See maestro.log." }
    }

    $result.status = 'passed'
}
catch {
    $result.status = 'failed'
    $result.failure = $_.Exception.Message
}
finally {
    Save-Result
}

$result | ConvertTo-Json -Depth 4
if ($result.status -ne 'passed') { exit 1 }
