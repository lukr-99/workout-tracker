<#
  pull-data.ps1 — pull OUR app's exported data off the phone (debug builds only).

  Because our debug build is debuggable, `adb run-as <pkg>` can read the app's private files dir
  (/data/data/<pkg>/files) without root — the same trick the ring-set app uses. Export from inside
  the app first (Settings -> Export) so there's a fresh bundle/CSV to pull.

  Usage:
    .\tools\pull-data.ps1                       # -> .\import\app-data\
    .\tools\pull-data.ps1 -Dest D:\backups\wt   # custom destination
    .\tools\pull-data.ps1 -Package com.lukr99.workout.debug
#>
param(
    [string]$Dest,
    [string]$Package = "com.lukr99.workout"
)
. "$PSScriptRoot\common.ps1"
$adb = Get-Adb
Assert-Device -Adb $adb

$repo = Split-Path -Parent $PSScriptRoot
if (-not $Dest) { $Dest = Join-Path $repo "import\app-data" }
New-Item -ItemType Directory -Force $Dest | Out-Null

# -1 forces one filename per line (plain `ls` prints columns under exec-out).
$list = & $adb exec-out run-as $Package sh -c "ls -1 files" 2>$null
if ($LASTEXITCODE -ne 0 -or -not $list) {
    Write-Host "Could not read app data for '$Package'." -ForegroundColor Yellow
    Write-Host "Is it installed as a DEBUG build, and have you exported from Settings first?" -ForegroundColor Yellow
    exit 1
}
$files = $list -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -match '\.(json|csv)$' }
if (-not $files) { Write-Host "No exported files yet. Export from the app first." -ForegroundColor Yellow; exit 0 }

foreach ($f in $files) {
    $out = Join-Path $Dest $f
    $text = & $adb exec-out run-as $Package cat "files/$f"
    [System.IO.File]::WriteAllText($out, ($text -join "`r`n"), (New-Object System.Text.UTF8Encoding($false)))
    Write-Host ("Pulled {0}  ->  {1}" -f $f, $out) -ForegroundColor Green
}
Write-Host "Done. Data in $Dest" -ForegroundColor Cyan
