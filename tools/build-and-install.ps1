<#
  build-and-install.ps1 — build the (native Android) app and install it on the tethered phone.

  Works once the Gradle/Compose project exists (Phase 0 of docs/rework/00-migration-plan.md).
  Requires a JDK 17+ (JAVA_HOME or on PATH) and the Android SDK. The Gradle wrapper fetches Gradle.

  Usage:
    .\tools\build-and-install.ps1            # assembleDebug + install -r
    .\tools\build-and-install.ps1 -Launch    # also launch the app after install
#>
param([switch]$Launch)
. "$PSScriptRoot\common.ps1"
$adb = Get-Adb
$repo = Split-Path -Parent $PSScriptRoot
$gradlew = Join-Path $repo "gradlew.bat"

if (-not (Test-Path $gradlew)) {
    Write-Host "No gradlew.bat yet — the Android project hasn't been scaffolded." -ForegroundColor Yellow
    Write-Host "This script activates in Phase 0 of docs/rework/00-migration-plan.md." -ForegroundColor Yellow
    exit 1
}

Assert-Device -Adb $adb
Write-Host "Building (assembleDebug)..." -ForegroundColor Cyan
& $gradlew -p $repo assembleDebug --console=plain
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed" }

$apk = Join-Path $repo "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) { throw "APK not found at $apk" }
Write-Host "Installing $apk ..." -ForegroundColor Cyan
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) { throw "Install failed" }
Write-Host "Installed." -ForegroundColor Green

if ($Launch) {
    $pkg = "com.lukr99.workout"
    & $adb shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 | Out-Null
    Write-Host "Launched $pkg." -ForegroundColor Green
}
