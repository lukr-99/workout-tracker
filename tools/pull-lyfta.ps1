<#
  pull-lyfta.ps1 — pull Lyfta's exported CSV(s) off the tethered phone to this PC.

  Lyfta is a release-signed app, so its private DB isn't reachable via `adb run-as`. The supported
  path is Lyfta's own export: in the app, Settings -> Export data -> CSV. Lyfta writes the file to
  shared storage (usually Downloads) or offers the share sheet — save it to the phone first, then
  run this to copy it here.

  Usage:
    .\tools\pull-lyfta.ps1                 # scan common dirs, copy CSV candidates to .\import\lyfta\
    .\tools\pull-lyfta.ps1 -Dest D:\lyfta  # custom destination
    .\tools\pull-lyfta.ps1 -All            # copy every *.csv found, not just Lyfta-looking ones
#>
param(
    [string]$Dest,
    [switch]$All
)
. "$PSScriptRoot\common.ps1"
$adb = Get-Adb
Assert-Device -Adb $adb

$repo = Split-Path -Parent $PSScriptRoot
if (-not $Dest) { $Dest = Join-Path $repo "import\lyfta" }
New-Item -ItemType Directory -Force $Dest | Out-Null

$searchDirs = @(
    "/sdcard/Download", "/sdcard/Downloads", "/sdcard/Documents",
    "/sdcard/Lyfta", "/sdcard/lyfta", "/sdcard/Android/data/com.lyfta/files",
    "/sdcard"
)

Write-Host "Scanning phone for CSV exports..." -ForegroundColor Cyan
$found = @()
foreach ($d in $searchDirs) {
    # -maxdepth keeps the /sdcard sweep cheap; ignore errors for missing dirs.
    $cmd = "find '$d' -maxdepth 2 -iname '*.csv' 2>/dev/null"
    $out = & $adb shell $cmd
    if ($out) { $found += ($out -split "`r?`n" | Where-Object { $_.Trim() }) }
}
$found = $found | Sort-Object -Unique
if (-not $found) {
    Write-Host "No .csv files found in the usual places." -ForegroundColor Yellow
    Write-Host "In Lyfta: Settings -> Export data -> CSV, save it to Downloads, then rerun." -ForegroundColor Yellow
    return
}

if (-not $All) {
    $likely = $found | Where-Object { $_ -match '(?i)lyfta|workout|training|exercise|strength|gym|log' }
    if ($likely) { $found = $likely }
    else { Write-Host "No obviously-Lyfta names; showing all CSVs (use -All to always keep all)." -ForegroundColor DarkGray }
}

Write-Host ("Found {0} candidate CSV(s):" -f @($found).Count) -ForegroundColor Green
foreach ($f in $found) {
    $name = Split-Path -Leaf $f
    $out = Join-Path $Dest $name
    & $adb pull "$f" "$out" | Out-Null
    if (Test-Path $out) {
        $rows = [Math]::Max(0, (Get-Content $out | Measure-Object -Line).Lines - 1)
        Write-Host ("  {0}  ({1} rows)  ->  {2}" -f $name, $rows, $out) -ForegroundColor Green
    }
}
Write-Host "Done. Inspect the header row, then finalize LyftaCsvImporter (docs/rework/05-lyfta-import.md)." -ForegroundColor Cyan
