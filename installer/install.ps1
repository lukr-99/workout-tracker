$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Stop-WithMessage([string]$Message) {
    Write-Host ""
    Write-Host $Message -ForegroundColor Red
    exit 1
}

function Get-ConnectedDevices([string]$AdbPath) {
    @(& $AdbPath devices | Select-Object -Skip 1 | Where-Object { $_ -match "\S" })
}

Write-Host "Ember phone installer" -ForegroundColor Cyan
Write-Host "This installer does not delete your existing Ember data." -ForegroundColor DarkGray

$installerDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$apkPath = Join-Path $installerDir "Ember.apk"
if (-not (Test-Path -LiteralPath $apkPath -PathType Leaf)) {
    Stop-WithMessage "Ember.apk is missing. Keep all installer files together and try again."
}

$localAdb = Join-Path $installerDir "platform-tools\adb.exe"
if (-not (Test-Path -LiteralPath $localAdb -PathType Leaf)) {
    Write-Host "Downloading Google's Android USB installer tools (one time only)..." -ForegroundColor Cyan
    $installerTemp = Join-Path ([System.IO.Path]::GetTempPath()) "ember-phone-installer"
    New-Item -ItemType Directory -Path $installerTemp -Force | Out-Null
    $archivePath = Join-Path $installerTemp "platform-tools.zip"
    try {
        Invoke-WebRequest `
            -Uri "https://dl.google.com/android/repository/platform-tools-latest-windows.zip" `
            -OutFile $archivePath `
            -UseBasicParsing
        Expand-Archive -LiteralPath $archivePath -DestinationPath $installerDir -Force
    } catch {
        Stop-WithMessage "Could not download Google's Android tools. Check the internet connection and try again.`n$($_.Exception.Message)"
    }
}

if (-not (Test-Path -LiteralPath $localAdb -PathType Leaf)) {
    Stop-WithMessage "Android installer tools were not found after download."
}

Write-Host ""
Write-Host "On the phone:" -ForegroundColor Yellow
Write-Host "  1. Unlock it."
Write-Host "  2. Connect it with a USB data cable."
Write-Host "  3. If asked, choose File transfer / Android Auto."
Write-Host "  4. Accept 'Allow USB debugging' and tick 'Always allow'."
Write-Host ""
Write-Host "First time only: USB debugging must be enabled in Settings > Developer options." -ForegroundColor DarkGray

& $localAdb start-server | Out-Null
$deviceId = $null
while ($null -eq $deviceId) {
    $rows = Get-ConnectedDevices $localAdb
    $ready = @($rows | Where-Object { $_ -match "\tdevice$" })
    $unauthorized = @($rows | Where-Object { $_ -match "\tunauthorized$" })

    if ($ready.Count -gt 1) {
        Stop-WithMessage "More than one Android device is connected. Disconnect the others and run the installer again."
    }
    if ($ready.Count -eq 1) {
        $deviceId = ($ready[0] -split "\s+")[0]
        break
    }
    if ($unauthorized.Count -gt 0) {
        Write-Host "The phone is waiting for permission. Unlock it and accept 'Allow USB debugging'." -ForegroundColor Yellow
    } else {
        Write-Host "No ready phone found. Check the cable and USB debugging." -ForegroundColor Yellow
    }
    Read-Host "Press Enter to check again"
}

Write-Host ""
Write-Host "Installing Ember..." -ForegroundColor Cyan
$installOutput = (& $localAdb -s $deviceId install -r $apkPath 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $installOutput -notmatch "Success") {
    if ($installOutput -match "INSTALL_FAILED_UPDATE_INCOMPATIBLE") {
        Stop-WithMessage "A copy of Ember signed by a different publisher is already installed. Export its data before uninstalling it; then run this installer again. Nothing was removed."
    }
    Stop-WithMessage "Android could not install Ember:`n$installOutput"
}

& $localAdb -s $deviceId shell monkey -p com.lukr99.workout -c android.intent.category.LAUNCHER 1 | Out-Null
Write-Host ""
Write-Host "Ember is installed and has been opened on the phone." -ForegroundColor Green
Write-Host "You can unplug the cable now."
Read-Host "Press Enter to close"
