# GlassCalc Runner Script for Windows
$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$javaBin = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin"
$java = if (Test-Path "$javaBin\java.exe") { "$javaBin\java.exe" } else { "java.exe" }
$javac = if (Test-Path "$javaBin\javac.exe") { "$javaBin\javac.exe" } else { "javac.exe" }

$m2fx = "$env:USERPROFILE\.m2\repository\org\openjfx"
$jars = @(
    "$m2fx\javafx-base\21.0.13\javafx-base-21.0.13-win.jar",
    "$m2fx\javafx-base\21.0.13\javafx-base-21.0.13.jar",
    "$m2fx\javafx-graphics\21.0.13\javafx-graphics-21.0.13-win.jar",
    "$m2fx\javafx-graphics\21.0.13\javafx-graphics-21.0.13.jar",
    "$m2fx\javafx-controls\21.0.13\javafx-controls-21.0.13-win.jar",
    "$m2fx\javafx-controls\21.0.13\javafx-controls-21.0.13.jar"
)
$modulePath = $jars -join ";"

# Compile if target\classes does not exist
$targetClasses = Join-Path $root "target\classes"
if (-not (Test-Path "$targetClasses\com\esmail\calculator\GlassCalcApp.class")) {
    Write-Host "Compiling GlassCalc..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $targetClasses | Out-Null
    $sources = Get-ChildItem -Path (Join-Path $root "src\main\java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
    & $javac -encoding UTF-8 --module-path $modulePath --add-modules javafx.controls -d $targetClasses $sources
    Copy-Item -Path (Join-Path $root "src\main\resources\*") -Destination $targetClasses -Recurse -Force
}

Write-Host "Starting GlassCalc..." -ForegroundColor Green
Start-Process -FilePath $java -ArgumentList @(
    "--module-path", "`"$modulePath`"",
    "--add-modules", "javafx.controls",
    "-cp", "`"$targetClasses`"",
    "com.esmail.calculator.GlassCalcApp"
)
