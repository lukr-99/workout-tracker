param(
    [string]$SourceUrl = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$assetsDirectory = Join-Path $repoRoot "app\src\main\assets"
$outputPath = Join-Path $assetsDirectory "free_exercise_image_index.json"
$temporaryPath = Join-Path ([System.IO.Path]::GetTempPath()) "free-exercise-db-$([guid]::NewGuid()).json"

$suffixes = @(
    "with dumbbells",
    "with dumbbell",
    "with barbell",
    "dumbbells",
    "dumbbell",
    "barbell",
    "exercise",
    "machine"
)
$aliases = @{
    "back squat" = "barbell full squat"
    "barbell bench press" = "barbell bench press medium grip"
    "biceps curl" = "dumbbell bicep curl"
    "incline dumbbell press" = "incline dumbbell press"
    "lat pulldown" = "wide grip lat pulldown"
    "lateral raise" = "side lateral raise"
    "overhead press" = "standing military press"
    "outdoor walk" = "trail running walking"
    "romanian deadlift" = "romanian deadlift with dumbbells"
    "seated cable row" = "seated cable rows"
    "stationary bike" = "bicycling stationary"
    "rowing" = "rowing stationary"
    "jump rope" = "rope jumping"
    "triceps pushdown" = "triceps pushdown"
    "treadmill run" = "running treadmill"
}

function ConvertTo-ExerciseImageKey([string]$Value) {
    $decomposed = $Value.Normalize([Text.NormalizationForm]::FormD)
    $builder = [Text.StringBuilder]::new()
    foreach ($character in $decomposed.ToCharArray()) {
        if ([Globalization.CharUnicodeInfo]::GetUnicodeCategory($character) -ne
            [Globalization.UnicodeCategory]::NonSpacingMark) {
            [void]$builder.Append($character)
        }
    }
    $key = $builder.ToString().ToLowerInvariant()
    $key = [regex]::Replace($key, "[^a-z0-9\s]", " ")
    $key = [regex]::Replace($key, "\s+", " ").Trim()
    foreach ($suffix in $suffixes) {
        if ($key.EndsWith(" $suffix")) {
            $key = $key.Substring(0, $key.Length - $suffix.Length - 1).Trim()
            break
        }
    }
    if ($aliases.ContainsKey($key)) {
        return $aliases[$key]
    }
    return $key
}

try {
    Invoke-WebRequest -Uri $SourceUrl -OutFile $temporaryPath
    $source = Get-Content -Raw -LiteralPath $temporaryPath | ConvertFrom-Json
    $index = [ordered]@{}
    foreach ($exercise in $source) {
        if (-not $exercise.name -or -not $exercise.images -or $exercise.images.Count -eq 0) {
            continue
        }
        $key = ConvertTo-ExerciseImageKey $exercise.name
        if (-not $index.Contains($key)) {
            $index[$key] = [ordered]@{
                images = @($exercise.images)
                muscle = @($exercise.primaryMuscles)[0]
                equipment = $exercise.equipment
            }
        }
    }
    New-Item -ItemType Directory -Force -Path $assetsDirectory | Out-Null
    $json = $index | ConvertTo-Json -Depth 5 -Compress
    [IO.File]::WriteAllText($outputPath, $json, [Text.UTF8Encoding]::new($false))
    Write-Output "Wrote $($index.Count) image entries to $outputPath"
}
finally {
    Remove-Item -LiteralPath $temporaryPath -Force -ErrorAction SilentlyContinue
}
