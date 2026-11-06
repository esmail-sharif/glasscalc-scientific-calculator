# CASIO fx-991ES PLUS Launcher Script for Windows
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
$targetClasses = Join-Path $root "target\classes"

# If native DLL or classes need building
if (-not (Test-Path "$targetClasses\native\windows-x86_64\glasscalc.dll")) {
    & (Join-Path $root "scripts\build-native.ps1")
}

Write-Host "Launching CASIO fx-991ES PLUS (Native Assembly Engine Active)..." -ForegroundColor Green
Start-Process -FilePath $java -ArgumentList @(
    "-Dfile.encoding=UTF-8",
    "--module-path", $modulePath,
    "--add-modules", "javafx.controls",
    "-cp", $targetClasses,
    "com.esmail.calculator.GlassCalcApp"
)
