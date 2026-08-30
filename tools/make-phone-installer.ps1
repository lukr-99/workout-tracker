<#
  Builds a signed release APK and packages a double-click Windows phone installer.

  Usage:
    .\tools\make-phone-installer.ps1
    .\tools\make-phone-installer.ps1 -Debug
    .\tools\make-phone-installer.ps1 -SkipBuild
#>
param(
    [switch]$Debug,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
$config = if ($Debug) { "debug" } else { "release" }
$task = if ($Debug) { ":app:assembleDebug" } else { ":app:assembleRelease" }
$gradlew = Join-Path $repo "gradlew.bat"
$buildFile = Join-Path $repo "app\build.gradle.kts"
$buildText = Get-Content -LiteralPath $buildFile -Raw
$versionMatch = [regex]::Match($buildText, 'versionName\s*=\s*"([^"]+)"')
if (-not $versionMatch.Success) { throw "Could not read versionName from app/build.gradle.kts" }
$version = $versionMatch.Groups[1].Value

if (-not $SkipBuild) {
    Write-Host "Building Ember $version ($config)..." -ForegroundColor Cyan
    & $gradlew -p $repo $task --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed." }
}

$apk = Join-Path $repo "app\build\outputs\apk\$config\app-$config.apk"
if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) {
    throw "APK not found at $apk. Build it first or omit -SkipBuild."
}

$distRoot = Join-Path $repo "dist"
$stage = Join-Path $distRoot "Ember-Phone-Installer-v$version"
New-Item -ItemType Directory -Path $stage -Force | Out-Null
Copy-Item -LiteralPath $apk -Destination (Join-Path $stage "Ember.apk") -Force
Copy-Item -LiteralPath (Join-Path $repo "installer\Install Ember.cmd") -Destination $stage -Force
Copy-Item -LiteralPath (Join-Path $repo "installer\install.ps1") -Destination $stage -Force
Copy-Item -LiteralPath (Join-Path $repo "installer\README.txt") -Destination $stage -Force

$zip = Join-Path $distRoot "Ember-Phone-Installer-v$version.zip"
Compress-Archive -Path (Join-Path $stage "*") -DestinationPath $zip -Force
Write-Host "Ready: $zip" -ForegroundColor Green
