<#
  publish-release.ps1 - build the signed release APK and create a DRAFT GitHub Release that the
  in-app updater can verify: Ember-<version>.apk plus Ember-<version>.apk.sha256.

  Nothing becomes visible to the updater until you publish the draft on GitHub, so you can check the
  notes and the files first. Run it from a clean main with the release keystore in place
  (docs/RELEASING.md).

  Usage:
    .\tools\publish-release.ps1              # build, verify the signer, write the checksum, draft
    .\tools\publish-release.ps1 -SkipBuild   # reuse app\build\outputs\apk\release\app-release.apk
    .\tools\publish-release.ps1 -DryRun      # do everything except talk to GitHub
#>
param(
    [switch]$SkipBuild,
    [switch]$DryRun
)
. "$PSScriptRoot\common.ps1"
$repo = Split-Path -Parent $PSScriptRoot

# SHA-256 of the release signing certificate (CN=lukr-99, OU=workout-tracker). An APK signed with
# any other key could never update the installed app, so it must never be published.
$ExpectedCertSha256 = "1db09253a4eec90556cbcc8f1a013e2d1b7037cec721f00e88cb73bdd88c19e3"
$utf8 = New-Object System.Text.UTF8Encoding($false)

# --- Preconditions --------------------------------------------------------------------------------
$buildText = [IO.File]::ReadAllText((Join-Path $repo "app\build.gradle.kts"), $utf8)
$versionMatch = [regex]::Match($buildText, 'versionName\s*=\s*"([^"]+)"')
if (-not $versionMatch.Success) { throw "Could not read versionName from app/build.gradle.kts." }
$version = $versionMatch.Groups[1].Value
$tag = "v$version"

$branch = (& git -C $repo rev-parse --abbrev-ref HEAD).Trim()
if ($branch -ne "main") { throw "Release from main, not '$branch'." }
& git -C $repo diff --quiet HEAD
if ($LASTEXITCODE -ne 0) { throw "There are uncommitted changes. Commit or stash them first." }
if (& git -C $repo ls-remote --tags origin $tag) { throw "Tag $tag already exists on GitHub. Bump versionName first." }

$changelog = [IO.File]::ReadAllText((Join-Path $repo "CHANGELOG.md"), $utf8)
$section = [regex]::Match($changelog, "(?ms)^## \[$([regex]::Escape($version))\][^\n]*\n(.*?)(?=^## \[|\z)")
if (-not $section.Success) { throw "CHANGELOG.md has no '## [$version]' section. Move [Unreleased] under it first." }
$notes = $section.Groups[1].Value.Trim()

# --- Build and verify -----------------------------------------------------------------------------
if (-not $SkipBuild) {
    Write-Host "Building the release APK..." -ForegroundColor Cyan
    & (Join-Path $repo "gradlew.bat") -p $repo :app:assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed." }
}
$built = Join-Path $repo "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path -LiteralPath $built -PathType Leaf)) {
    throw "No signed release APK at $built. Is keystore.properties in place? (docs/RELEASING.md)"
}

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
$apksigner = Join-Path $sdk "build-tools\35.0.0\apksigner.bat"
if (-not (Test-Path -LiteralPath $apksigner)) { throw "apksigner not found at $apksigner." }
$certs = (& $apksigner verify --print-certs $built) -join "`n"
if ($LASTEXITCODE -ne 0) { throw "apksigner could not verify the APK. It may be unsigned." }
$certMatch = [regex]::Match($certs, 'Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})')
if (-not $certMatch.Success -or $certMatch.Groups[1].Value -ne $ExpectedCertSha256) {
    throw "The APK is not signed with the release key. Found: $($certMatch.Groups[1].Value)"
}
Write-Host "Signed with the release key." -ForegroundColor Green

# --- Assets: Ember-<version>.apk and its checksum, the names the updater looks for ---------------
$dist = Join-Path $repo "dist\release-$tag"
New-Item -ItemType Directory -Force $dist | Out-Null
$apkName = "Ember-$version.apk"
$apk = Join-Path $dist $apkName
Copy-Item -LiteralPath $built -Destination $apk -Force
$hash = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
$sumFile = "$apk.sha256"
[IO.File]::WriteAllText($sumFile, "$hash  $apkName`n", $utf8)
$notesFile = Join-Path $dist "notes.md"
[IO.File]::WriteAllText($notesFile, "$notes`n", $utf8)

Write-Host "Version : $version"
Write-Host "APK     : $apk"
Write-Host "SHA-256 : $hash"

if ($DryRun) {
    Write-Host "Dry run: nothing was sent to GitHub." -ForegroundColor Yellow
    exit 0
}

& gh release create $tag $apk $sumFile --draft --target main --title "Ember $version" --notes-file $notesFile
if ($LASTEXITCODE -ne 0) { throw "gh release create failed." }
Write-Host "Draft release $tag created. Review it on GitHub, then publish it so the updater can see it." -ForegroundColor Green
