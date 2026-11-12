<#
.SYNOPSIS
    Uploads Ember's release signing key to the GitHub Actions secrets that .github/workflows/release.yml
    signs with: KEYSTORE_BASE64, KEYSTORE_STORE_PASSWORD, KEYSTORE_KEY_ALIAS, KEYSTORE_KEY_PASSWORD.

.DESCRIPTION
    Ember's key already exists, so this never makes one: a new key could never update the installed
    app in place (docs/RELEASING.md). It reads keystore.properties and the keystore it names from
    -From (the repository root by default, or the backup folder), checks that the key's certificate
    is the release certificate, and uploads the four secrets. The passwords are never printed and
    never appear on a command line.

    Run it once, and again only if the key or its password changes.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\setup-signing.ps1 -From D:\android-keystores\workout-tracker

.NOTES
    Requires keytool (from a JDK) on PATH or under JAVA_HOME, and the GitHub CLI `gh`, signed in with
    access to lukr-99/workout-tracker.
#>
[CmdletBinding()]
param(
    # The folder with keystore.properties and the keystore it names.
    [string] $From,
    [string] $Repository = "lukr-99/workout-tracker"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($From)) { $From = $repoRoot }

# SHA-256 of the release signing certificate, as in tools/publish-release.ps1 and release.yml.
$ExpectedCertSha256 = "1db09253a4eec90556cbcc8f1a013e2d1b7037cec721f00e88cb73bdd88c19e3"
# UTF-8 without a byte-order mark, which gh would read as part of the first name.
$Utf8NoBom = New-Object System.Text.UTF8Encoding $false

$propertiesPath = Join-Path $From "keystore.properties"
if (-not (Test-Path -LiteralPath $propertiesPath)) { throw "No keystore.properties in $From." }
$props = @{}
foreach ($line in [IO.File]::ReadAllLines($propertiesPath, $Utf8NoBom)) {
    if ($line -match "^\s*([^#=]+)=(.*)$") { $props[$matches[1].Trim()] = $matches[2].Trim() }
}
foreach ($name in "storeFile", "storePassword", "keyAlias", "keyPassword") {
    if ([string]::IsNullOrWhiteSpace($props[$name])) { throw "keystore.properties has no $name." }
}
$keystorePath = if ([IO.Path]::IsPathRooted($props["storeFile"])) { $props["storeFile"] } else { Join-Path $From $props["storeFile"] }
if (-not (Test-Path -LiteralPath $keystorePath)) { throw "No keystore at $keystorePath." }

$keytool = if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\keytool.exe"))) { Join-Path $env:JAVA_HOME "bin\keytool.exe" } else { "keytool" }
if (-not (Get-Command $keytool -ErrorAction SilentlyContinue)) { throw "keytool not found. Install a JDK or set JAVA_HOME." }
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { throw "gh not found on PATH." }

# keytool reads the password from an environment variable, so it never shows in a process list.
$env:EMBER_STOREPASS = $props["storePassword"]
try {
    $listing = & $keytool -list -v -keystore $keystorePath -alias $props["keyAlias"] -storepass:env EMBER_STOREPASS
    if ($LASTEXITCODE -ne 0) { throw "keytool could not open the keystore with that password and alias." }
}
finally {
    Remove-Item Env:EMBER_STOREPASS -ErrorAction SilentlyContinue
}
$found = (($listing | Select-String -Pattern 'SHA256:\s*([0-9A-F:]+)' | Select-Object -First 1).Matches.Groups[1].Value) -replace ':', ''
if ($found.ToLowerInvariant() -ne $ExpectedCertSha256) {
    throw "This keystore does not hold Ember's release key (found $found). Nothing was uploaded."
}
Write-Host "The keystore holds Ember's release key." -ForegroundColor Green

# Values go through a short-lived dotenv file: a pipe would add a newline to the password, and
# --body would put it on gh's command line.
$secrets = [IO.Path]::Combine([IO.Path]::GetTempPath(), "ember-secrets-$([guid]::NewGuid().ToString('N')).env")
try {
    $b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))
    $lines = "KEYSTORE_BASE64=$b64`nKEYSTORE_STORE_PASSWORD=$($props['storePassword'])`nKEYSTORE_KEY_ALIAS=$($props['keyAlias'])`nKEYSTORE_KEY_PASSWORD=$($props['keyPassword'])`n"
    [IO.File]::WriteAllText($secrets, $lines, $Utf8NoBom)
    & gh secret set --repo $Repository --env-file $secrets
    if ($LASTEXITCODE -ne 0) { throw "gh secret set failed with exit code $LASTEXITCODE." }
}
finally {
    Remove-Item -LiteralPath $secrets -Force -ErrorAction SilentlyContinue
}
Write-Host "Uploaded KEYSTORE_BASE64, KEYSTORE_STORE_PASSWORD, KEYSTORE_KEY_ALIAS and KEYSTORE_KEY_PASSWORD to $Repository." -ForegroundColor Green
Write-Host "Try it: GitHub, Actions, Release, Run workflow. A run by hand builds and checks but creates no release." -ForegroundColor Cyan
