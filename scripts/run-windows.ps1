$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

$dll = Join-Path $root 'src\main\resources\native\windows-x86_64\glasscalc.dll'
if (-not (Test-Path $dll)) {
    & (Join-Path $PSScriptRoot 'build-native.ps1')
}

Push-Location $root
try {
    if (Get-Command mvn -ErrorAction SilentlyContinue) {
        mvn javafx:run
    } else {
        $javaBin = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin"
        $java = if (Test-Path "$javaBin\java.exe") { "$javaBin\java.exe" } else { "java.exe" }
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

        Write-Host "Running CASIO fx-991ES PLUS via JavaFX..." -ForegroundColor Green
        & $java '-Dfile.encoding=UTF-8' --module-path $modulePath --add-modules javafx.controls -cp $targetClasses com.esmail.calculator.GlassCalcApp
    }
} finally {
    Pop-Location
}
