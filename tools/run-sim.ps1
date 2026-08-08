<#
  run-sim.ps1 — trigger the debug GPS route simulator (RunSimReceiver) on the tethered phone.

  Replays a synthetic straight-line run straight into the live-run controller (no real GPS), so a
  full run records + saves in a few seconds. Debug builds only. Open the Run screen first to watch the
  ember polyline grow, or just check the Runs hub / pull the DB afterwards.

  Usage:
    .\tools\run-sim.ps1                                   # 1 km in 5:00/km from a default point
    .\tools\run-sim.ps1 -Meters 5000 -Seconds 1500        # 5 km @ 5:00/km
    .\tools\run-sim.ps1 -Lat 50.0876 -Lon 14.4207 -Bearing 90 -Meters 3000 -Seconds 900
#>
param(
    [double]$Lat = 50.0876,
    [double]$Lon = 14.4207,
    [int]$Meters = 1000,
    [int]$Seconds = 300,
    [double]$Bearing = 0.0,
    [switch]$UseRoute  # link the simulated run to the newest saved route (start-from-route test)
)
. "$PSScriptRoot\common.ps1"
$adb = Get-Adb
Assert-Device -Adb $adb

Write-Host "Simulating a $Meters m run over $Seconds s (bearing $Bearing) from $Lat,$Lon ..." -ForegroundColor Cyan
# Explicit component (-n): action-only broadcasts to a manifest receiver are blocked by Android's
# background-broadcast limits, so target the receiver directly.
$useRouteArg = if ($UseRoute) { "true" } else { "false" }
& $adb shell am broadcast -a com.lukr99.workout.SIM_RUN -n com.lukr99.workout/.data.location.RunSimReceiver `
    --ed lat $Lat --ed lon $Lon --ei meters $Meters --ei seconds $Seconds --ed bearing $Bearing `
    --ez useRoute $useRouteArg
Write-Host "Sent. Watch the Run screen, or open Runs to see the saved run (logcat tag: RunSim)." -ForegroundColor Green
