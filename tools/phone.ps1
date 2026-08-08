<#
  phone.ps1 — one omni-tool for working with the tethered Android phone.

  Usage:
    .\tools\phone.ps1 <command> [args]

  Commands:
    devices                 List attached devices and their auth state.
    wait                    Wait until a device is authorized (accept the on-phone prompt).
    apps [filter]           List third-party (user-installed) packages, optionally filtered.
    find <fragment>         Find installed package ids matching a name fragment (e.g. 'lyfta').
    screenshot [outPath]    Grab a PNG screenshot to outPath (default: .\import\screen-<ts>.png).
    tap <x> <y>             Tap at device pixel (x,y). Coordinates are the real screen, not scaled.
    swipe <x1> <y1> <x2> <y2> [ms]   Swipe/drag (default 250 ms) — e.g. to scroll.
    key <keycode>           Send a key event (e.g. BACK, HOME, ENTER, or a numeric code).
    logcat [tagFilter]      Stream logcat, optionally grep-filtered (Ctrl+C to stop).
    install <apk>           adb install -r <apk>.
    launch <package>        Launch an app by package id.
    seed-route [name] [meters] [bearing]   Seed a synthetic saved route (debug RunDevReceiver).
    dump                    Log Run Mode DB counts and print them (RUNMODE_DUMP runs=.. routes=..).
    pull-lyfta              Pull Lyfta's exported CSV(s) off the phone (delegates to pull-lyfta.ps1).

  Examples:
    .\tools\phone.ps1 devices
    .\tools\phone.ps1 find lyfta
    .\tools\phone.ps1 apps fit
    .\tools\phone.ps1 screenshot
#>
param(
    [Parameter(Position = 0)][string]$Command = "devices",
    [Parameter(Position = 1, ValueFromRemainingArguments = $true)][string[]]$Args
)
. "$PSScriptRoot\common.ps1"
$adb = Get-Adb
$repo = Split-Path -Parent $PSScriptRoot

switch ($Command.ToLower()) {

    "devices" {
        Write-Host "adb: $adb" -ForegroundColor DarkGray
        & $adb devices -l
        $state = Get-DeviceState -Adb $adb
        if ($state -eq "device") { Write-Host "OK: one authorized device." -ForegroundColor Green }
        elseif ($state -eq "unauthorized") { Write-Host "UNAUTHORIZED: accept the USB-debugging prompt on the phone." -ForegroundColor Yellow }
        elseif (-not $state) { Write-Host "No device attached." -ForegroundColor Yellow }
    }

    "wait" {
        Write-Host "Waiting for an authorized device (accept the prompt on the phone)..." -ForegroundColor Cyan
        & $adb wait-for-device
        for ($i = 0; $i -lt 60; $i++) {
            if ((Get-DeviceState -Adb $adb) -eq "device") { Write-Host "Authorized." -ForegroundColor Green; return }
            Start-Sleep -Milliseconds 500
        }
        throw "Timed out waiting for authorization."
    }

    "apps" {
        Assert-Device -Adb $adb
        $filter = if ($Args) { $Args[0] } else { "" }
        $pkgs = (& $adb shell pm list packages -3) -split "`r?`n" |
            ForEach-Object { $_ -replace '^package:', '' } | Where-Object { $_ }
        if ($filter) { $pkgs = $pkgs | Where-Object { $_ -match [regex]::Escape($filter) } }
        $pkgs | Sort-Object | ForEach-Object { Write-Host $_ }
        Write-Host ("{0} package(s)." -f @($pkgs).Count) -ForegroundColor DarkGray
    }

    "find" {
        Assert-Device -Adb $adb
        if (-not $Args) { throw "Usage: phone.ps1 find <fragment>" }
        $hits = Find-Package -Fragment $Args[0] -Adb $adb
        if (-not $hits) { Write-Host "No package matches '$($Args[0])'." -ForegroundColor Yellow; return }
        $hits | ForEach-Object {
            Write-Host $_ -ForegroundColor Green
            $path = (& $adb shell pm path $_) -replace '^package:', ''
            Write-Host ("  apk: {0}" -f ($path -join ', ')) -ForegroundColor DarkGray
        }
    }

    "screenshot" {
        Assert-Device -Adb $adb
        $out = if ($Args) { $Args[0] } else { Join-Path $repo ("import\screen-{0}.png" -f (Get-Date -Format yyyyMMdd-HHmmss)) }
        New-Item -ItemType Directory -Force (Split-Path -Parent $out) | Out-Null
        # NB: `adb exec-out screencap -p > $out` corrupts the PNG under Windows PowerShell
        # (the `>` operator re-encodes the binary stream as text). Capture on-device then pull,
        # which is byte-safe on every shell.
        $remote = "/sdcard/__wt_screenshot.png"
        & $adb shell screencap -p $remote
        & $adb pull $remote $out | Out-Null
        & $adb shell rm -f $remote
        Write-Host "Saved $out" -ForegroundColor Green
    }

    "tap" {
        Assert-Device -Adb $adb
        if (-not $Args -or $Args.Count -lt 2) { throw "Usage: phone.ps1 tap <x> <y>" }
        & $adb shell input tap $Args[0] $Args[1]
    }

    "swipe" {
        Assert-Device -Adb $adb
        if (-not $Args -or $Args.Count -lt 4) { throw "Usage: phone.ps1 swipe <x1> <y1> <x2> <y2> [ms]" }
        $ms = if ($Args.Count -ge 5) { $Args[4] } else { "250" }
        & $adb shell input swipe $Args[0] $Args[1] $Args[2] $Args[3] $ms
    }

    "key" {
        Assert-Device -Adb $adb
        if (-not $Args) { throw "Usage: phone.ps1 key <keycode>  (e.g. BACK, HOME, or a number)" }
        $code = $Args[0]
        if ($code -notmatch '^[0-9]+$') { $code = "KEYCODE_$($code.ToUpper())" }
        & $adb shell input keyevent $code
    }

    "seed-route" {
        Assert-Device -Adb $adb
        $name = if ($Args.Count -ge 1) { $Args[0] } else { "Dev route" }
        $meters = if ($Args.Count -ge 2) { $Args[1] } else { "1000" }
        $bearing = if ($Args.Count -ge 3) { $Args[2] } else { "0" }
        & $adb shell am broadcast -a com.lukr99.workout.DEV_SEED_ROUTE -n com.lukr99.workout/.data.location.RunDevReceiver `
            --es name "$name" --ei meters $meters --ed bearing $bearing | Out-Null
        Write-Host "Seeded route '$name' ($meters m)." -ForegroundColor Green
    }

    "dump" {
        Assert-Device -Adb $adb
        & $adb logcat -c
        & $adb shell am broadcast -a com.lukr99.workout.DEV_DUMP -n com.lukr99.workout/.data.location.RunDevReceiver | Out-Null
        Start-Sleep -Milliseconds 700
        $line = (& $adb logcat -d -s RunDev:D) -split "`r?`n" | Where-Object { $_ -match "RUNMODE_DUMP" } | Select-Object -Last 1
        if ($line) { Write-Host ($line -replace '.*RUNMODE_DUMP', 'RUNMODE_DUMP') -ForegroundColor Green }
        else { Write-Host "No dump captured (is a debug build installed?)." -ForegroundColor Yellow }
    }

    "logcat" {
        Assert-Device -Adb $adb
        if ($Args) { & $adb logcat | Select-String -Pattern $Args[0] }
        else { & $adb logcat }
    }

    "install" {
        Assert-Device -Adb $adb
        if (-not $Args) { throw "Usage: phone.ps1 install <apk>" }
        & $adb install -r $Args[0]
    }

    "launch" {
        Assert-Device -Adb $adb
        if (-not $Args) { throw "Usage: phone.ps1 launch <package>" }
        & $adb shell monkey -p $Args[0] -c android.intent.category.LAUNCHER 1 | Out-Null
        Write-Host "Launched $($Args[0])." -ForegroundColor Green
    }

    "pull-lyfta" { & (Join-Path $PSScriptRoot "pull-lyfta.ps1") @Args }

    default { throw "Unknown command '$Command'. Run with no args for usage, or open the script header." }
}
