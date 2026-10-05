<#
  Start, inspect, and stop local Android Emulators for agent-driven testing. Physical devices are
  never touched: every serial-scoped command requires an emulator-NNNN serial.

  Examples:
    .\tools\emulator.ps1 list
    .\tools\emulator.ps1 create -Avd agent_phone_api36 -SystemImage system-images/android-36/google_apis/x86_64
    .\tools\emulator.ps1 start                       # the only AVD, headless, waits for boot
    .\tools\emulator.ps1 start -Avd medium_phone -Window
    .\tools\emulator.ps1 start -DisableAnimations    # stable screenshots and Maestro runs
    .\tools\emulator.ps1 status
    .\tools\emulator.ps1 stop -Serial emulator-5554
#>
param(
    [Parameter(Position = 0, Mandatory)]
    [ValidateSet('list', 'create', 'start', 'status', 'stop')]
    [string]$Command,
    [ValidatePattern('^[A-Za-z0-9._-]+$')]
    [string]$Avd,
    # An installed image in sdkmanager or Android CLI form, e.g. system-images/android-36/google_apis/x86_64.
    [ValidatePattern('^system-images[;/]android-[0-9A-Za-z.-]+[;/][a-z0-9_-]+[;/](x86_64|arm64-v8a)$')]
    [string]$SystemImage,
    [ValidatePattern('^emulator-\d{4,5}$')]
    [string]$Serial,
    [switch]$Window,
    [switch]$DisableAnimations,
    # Keep the emulator's saved state untouched so every run starts from the same snapshot.
    [switch]$SaveSnapshot,
    [ValidateSet('auto', 'host', 'swiftshader_indirect', 'guest')]
    [string]$Gpu = 'auto',
    [ValidateRange(30, 900)]
    [int]$TimeoutSeconds = 300
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

$adb = Get-AdbPath

function Get-RunningEmulatorSerial {
    @(Get-AdbDeviceRecord -Adb $adb | Where-Object { $_.Serial -match '^emulator-\d+$' })
}

function Get-AvdName {
    $emulator = Get-EmulatorPath
    $ErrorActionPreference = 'Continue'
    @(& $emulator -list-avds 2>$null | ForEach-Object { "$_".Trim() } |
            Where-Object { $_ -and $_ -notmatch '^(INFO|WARNING|ERROR)\b' })
}

function Get-BootCompleted {
    param([string]$Target)
    $ErrorActionPreference = 'Continue'
    $value = & $adb -s $Target shell getprop sys.boot_completed 2>$null
    return ("$value".Trim() -eq '1')
}

switch ($Command) {
    'list' {
        $names = @(Get-AvdName)
        if ($names.Count -eq 0) {
            Write-Host 'No AVDs. Create one with: android emulator create medium_phone' -ForegroundColor Yellow
        }
        $names
    }
    'create' {
        # Writes the same two files avdmanager writes. Useful when `android emulator create`
        # insists on downloading a different (Play Store) image than the one already installed.
        if (-not $Avd -or -not $SystemImage) { throw '-Avd and -SystemImage are required for create.' }
        $parts = $SystemImage -split '[;/]'
        $imageDirectory = Join-Path (Get-AndroidSdkRoot) ($parts -join '\')
        if (-not (Test-Path -LiteralPath (Join-Path $imageDirectory 'system.img'))) {
            throw "System image is not installed: $imageDirectory. Run: android sdk install $($parts -join '/')"
        }
        $avdHome = if ($env:ANDROID_AVD_HOME) { $env:ANDROID_AVD_HOME } else { Join-Path $env:USERPROFILE '.android\avd' }
        $avdDirectory = Join-Path $avdHome "$Avd.avd"
        $avdIni = Join-Path $avdHome "$Avd.ini"
        if ((Test-Path -LiteralPath $avdDirectory) -or (Test-Path -LiteralPath $avdIni)) {
            throw "AVD '$Avd' already exists; choose another name."
        }
        [System.IO.Directory]::CreateDirectory($avdDirectory) | Out-Null
        $api = $parts[1] -replace '^android-', ''
        $config = @(
            "AvdId=$Avd", "avd.ini.displayname=$Avd", 'avd.ini.encoding=UTF-8', 'PlayStore.enabled=false',
            "abi.type=$($parts[3])", "hw.cpu.arch=$(if ($parts[3] -eq 'x86_64') { 'x86_64' } else { 'arm64' })",
            'hw.cpu.ncore=4', 'hw.ramSize=3072', 'vm.heapSize=512', 'disk.dataPartition.size=6G',
            "image.sysdir.1=$($parts -join '\')\", "tag.id=$($parts[2])", "target=android-$api",
            'hw.lcd.width=1080', 'hw.lcd.height=2400', 'hw.lcd.density=420', 'hw.keyboard=yes',
            'hw.gpu.enabled=yes', 'hw.gpu.mode=auto', 'hw.mainKeys=no', 'hw.camera.back=none',
            'hw.camera.front=none', 'hw.audioInput=no', 'hw.sdCard=no', 'showDeviceFrame=no'
        )
        $utf8 = [System.Text.UTF8Encoding]::new($false)
        [System.IO.File]::WriteAllLines((Join-Path $avdDirectory 'config.ini'), [string[]]$config, $utf8)
        [System.IO.File]::WriteAllLines($avdIni, [string[]]@(
                'avd.ini.encoding=UTF-8', "path=$avdDirectory", "path.rel=avd\$Avd.avd", "target=android-$api"), $utf8)
        Write-Host "Created AVD $Avd from $SystemImage." -ForegroundColor Green
    }
    'status' {
        $running = @(Get-RunningEmulatorSerial)
        if ($running.Count -eq 0) {
            Write-Host 'No emulator is running.'
        }
        foreach ($record in $running) {
            $booted = $record.State -eq 'device' -and (Get-BootCompleted $record.Serial)
            [pscustomobject]@{ Serial = $record.Serial; State = $record.State; Booted = $booted }
        }
    }
    'start' {
        $names = @(Get-AvdName)
        if (-not $Avd) {
            if ($names.Count -ne 1) {
                throw "Pass -Avd explicitly. Available AVDs: $($names -join ', ')"
            }
            $Avd = $names[0]
        }
        elseif ($names -notcontains $Avd) {
            throw "AVD '$Avd' does not exist. Available AVDs: $($names -join ', ')"
        }

        # Emulators listen on even console ports 5554-5584; the serial is derived from the port.
        $used = @(Get-RunningEmulatorSerial | ForEach-Object { [int]($_.Serial -replace '^emulator-', '') })
        $port = @(5554..5584 | Where-Object { $_ % 2 -eq 0 -and $used -notcontains $_ })[0]
        if (-not $port) { throw 'No free emulator console port between 5554 and 5584.' }
        $target = "emulator-$port"

        $arguments = @('-avd', $Avd, '-port', "$port", '-gpu', $Gpu, '-no-boot-anim', '-no-audio')
        if (-not $Window) { $arguments += '-no-window' }
        if (-not $SaveSnapshot) { $arguments += '-no-snapshot-save' }

        $logDirectory = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) 'artifacts\emulator'))
        [System.IO.Directory]::CreateDirectory($logDirectory) | Out-Null
        $log = Join-Path $logDirectory "$Avd-$port.log"
        Start-Process -FilePath (Get-EmulatorPath) -ArgumentList $arguments -WindowStyle Hidden `
            -RedirectStandardOutput $log -RedirectStandardError "$log.err" | Out-Null

        & $adb start-server | Out-Null
        $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
        while (-not (Get-BootCompleted $target)) {
            if ([DateTimeOffset]::UtcNow -ge $deadline) {
                throw "Emulator $target did not finish booting within $TimeoutSeconds s. See $log"
            }
            Start-Sleep -Seconds 2
        }

        if ($DisableAnimations) {
            foreach ($setting in 'window_animation_scale', 'transition_animation_scale', 'animator_duration_scale') {
                & $adb -s $target shell settings put global $setting 0
                if ($LASTEXITCODE -ne 0) { throw "Failed to set $setting on $target." }
            }
        }
        Write-Host "Emulator $Avd is booted as $target (log: $log)." -ForegroundColor Green
        $target
    }
    'stop' {
        if (-not $Serial) {
            $running = @(Get-RunningEmulatorSerial)
            if ($running.Count -ne 1) {
                throw "Pass -Serial explicitly. Running emulators: $(($running | ForEach-Object Serial) -join ', ')"
            }
            $Serial = $running[0].Serial
        }
        & $adb -s $Serial emu kill | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Failed to stop $Serial." }
        Write-Host "Stopped $Serial." -ForegroundColor Green
    }
}
