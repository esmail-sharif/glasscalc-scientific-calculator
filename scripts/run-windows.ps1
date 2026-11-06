$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

$dll = Join-Path $root 'src\main\resources\native\windows-x86_64\glasscalc.dll'
if (-not (Test-Path $dll)) {
    & (Join-Path $PSScriptRoot 'build-native.ps1')
}

Push-Location $root
try {
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

    # Always ensure target\classes is up to date
    if (-not (Test-Path "$targetClasses\com\esmail\calculator\GlassCalcApp.class")) {
        Write-Host "Compiling sources..." -ForegroundColor Cyan
        New-Item -ItemType Directory -Path $targetClasses -Force | Out-Null
        $mainSources = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
        & $javac -encoding UTF-8 --module-path $modulePath --add-modules javafx.controls -d $targetClasses $mainSources
        Copy-Item -Path "src\main\resources\*" -Destination "$targetClasses\" -Recurse -Force
    }

    Write-Host "Running CASIO fx-991ES PLUS via JavaFX..." -ForegroundColor Green
    & $java '-Dfile.encoding=UTF-8' --module-path $modulePath --add-modules javafx.controls -cp $targetClasses com.esmail.calculator.GlassCalcApp
} finally {
    Pop-Location
}
