<#
  run-mode-check.ps1 — one-command, chainable Run Mode verification on the tethered phone.

  Codifies the manual "install → seed → click through → screenshot → confirm" loop into a repeatable
  script an agent (or a human) can run to set up and confirm Run Mode state without hand-driving the UI.
  Seeding is fully headless (debug broadcasts); navigation uses resolution-relative taps; state is
  asserted from a parseable DB dump (not just screenshots).

  Usage:
    .\tools\run-mode-check.ps1                 # seed + navigate + screenshot + assert on the current build
    .\tools\run-mode-check.ps1 -Build          # build+install first
    .\tools\run-mode-check.ps1 -Seed           # also seed a route + 3 runs (one route-linked)
    .\tools\run-mode-check.ps1 -Build -Seed -OutDir docs\run-mode\verify

  Exit code is non-zero if the DB assertions fail, so it is CI/agent chainable.
#>
param(
    [switch]$Build,
    [switch]$Seed,
    [switch]$Clean,   # wipe all runs+routes before seeding → deterministic exact-count assertions
    [string]$OutDir
)
. "$PSScriptRoot\common.ps1"
$adb = Get-Adb
$repo = Split-Path -Parent $PSScriptRoot
Assert-Device -Adb $adb
$pkg = "com.lukr99.workout"

if (-not $OutDir) { $OutDir = Join-Path $repo ("import\run-mode-check-{0}" -f (Get-Date -Format yyyyMMdd-HHmmss)) }
New-Item -ItemType Directory -Force $OutDir | Out-Null

# --- Resolution-relative helpers -------------------------------------------------------------------
$sizeRaw = (& $adb shell wm size) -join " "
if ($sizeRaw -match '(\d+)x(\d+)') { $W = [int]$Matches[1]; $H = [int]$Matches[2] } else { $W = 1080; $H = 2340 }
function Tap($fx, $fy) { & $adb shell input tap ([int]($W * $fx)) ([int]($H * $fy)); Start-Sleep -Milliseconds 700 }
function Shot($name) {
    $out = Join-Path $OutDir "$name.png"
    & $adb shell screencap -p /sdcard/__rmc.png
    & $adb pull /sdcard/__rmc.png $out | Out-Null
    & $adb shell rm -f /sdcard/__rmc.png
    Write-Host "  shot: $out" -ForegroundColor DarkGray
}
function Dev($action, [string[]]$extra) {
    & $adb shell am broadcast -a "com.lukr99.workout.$action" -n "$pkg/.data.location.RunDevReceiver" @extra | Out-Null
}

# Bottom-nav tab centres (5 equal slots) + the 16 KB debug dialog's OK button.
$navY = 0.855
function DismissDebugDialog { Tap 0.23 0.888 }   # "OK" on the One-UI 16 KB-page warning (debug builds)

if ($Build) {
    Write-Host "Building + installing..." -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot "build-and-install.ps1")
    if ($LASTEXITCODE -ne 0) { throw "build-and-install failed" }
}

if ($Clean) {
    Write-Host "Clearing all runs + routes..." -ForegroundColor Cyan
    Dev "DEV_CLEAR" @()
    Start-Sleep -Milliseconds 900
}

if ($Seed) {
    Write-Host "Seeding a route + 3 runs (one route-linked)..." -ForegroundColor Cyan
    Dev "DEV_SEED_ROUTE" @("--es", "name", "Check route", "--ei", "meters", "1200", "--ed", "bearing", "40")
    Start-Sleep -Milliseconds 800
    & (Join-Path $PSScriptRoot "run-sim.ps1") -Meters 5000 -Seconds 1500 -Bearing 30 | Out-Null
    Start-Sleep -Seconds 2
    & (Join-Path $PSScriptRoot "run-sim.ps1") -Meters 3000 -Seconds 780 -Bearing 90 | Out-Null
    Start-Sleep -Seconds 2
    & (Join-Path $PSScriptRoot "run-sim.ps1") -UseRoute -Meters 1100 -Seconds 360 -Bearing 40 | Out-Null
    Start-Sleep -Seconds 3
}

Write-Host "Capturing screens..." -ForegroundColor Cyan
& $adb shell input keyevent KEYCODE_WAKEUP | Out-Null   # wake the screen (was off → AOD)
& $adb shell wm dismiss-keyguard | Out-Null              # dismiss the lock (no-op if secured)
Start-Sleep -Milliseconds 600
& $adb shell am start -n "$pkg/.MainActivity" | Out-Null
Start-Sleep -Seconds 2
DismissDebugDialog
Tap 0.14 $navY;  Shot "01-home"                    # Home tab
Tap 0.32 $navY;  Shot "02-runs-hub"                # Runs tab
Tap 0.68 $navY                                     # Progress tab
Tap 0.50 0.095;  Shot "03-progress-running"        # Running segment (middle of the 3-way toggle)

# --- Non-visual assertion via the DB dump ----------------------------------------------------------
Write-Host "Asserting DB state..." -ForegroundColor Cyan
& $adb logcat -c
Dev "DEV_DUMP" @()
Start-Sleep -Milliseconds 900
$dump = ((& $adb logcat -d -s RunDev:D) -split "`r?`n" | Where-Object { $_ -match "RUNMODE_DUMP" } | Select-Object -Last 1)
if (-not $dump) { Write-Host "FAIL: no DB dump (is a debug build installed?)" -ForegroundColor Red; exit 1 }
$dump = ($dump -replace '.*RUNMODE_DUMP', 'RUNMODE_DUMP')
Write-Host "  $dump" -ForegroundColor Gray
$runs = [int]([regex]::Match($dump, 'runs=(\d+)').Groups[1].Value)
$routes = [int]([regex]::Match($dump, 'routes=(\d+)').Groups[1].Value)
$linked = [int]([regex]::Match($dump, 'linkedRuns=(\d+)').Groups[1].Value)

$ok = $true
function Check($name, $cond) {
    if ($cond) { Write-Host "  PASS: $name" -ForegroundColor Green }
    else { Write-Host "  FAIL: $name" -ForegroundColor Red; $script:ok = $false }
}
if ($Clean -and $Seed) {
    # Deterministic: a clean seed leaves exactly the 1 seeded route, 3 runs, and 1 route-linked run.
    Check "exactly 3 runs" ($runs -eq 3)
    Check "exactly 1 route" ($routes -eq 1)
    Check "exactly 1 linked run" ($linked -eq 1)
} else {
    Check "at least one run recorded" ($runs -ge 1)
    Check "at least one route saved" ($routes -ge 1)
    if ($Seed) { Check "a run is linked to a route" ($linked -ge 1) }
}

Write-Host ""
Write-Host "Screenshots: $OutDir" -ForegroundColor Cyan
if ($ok) { Write-Host "Run Mode check: PASS ($runs runs, $routes routes, $linked linked)" -ForegroundColor Green; exit 0 }
else { Write-Host "Run Mode check: FAIL" -ForegroundColor Red; exit 1 }
