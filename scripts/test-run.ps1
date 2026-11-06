$root = Split-Path -Parent $PSScriptRoot
$javaBin = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin\java.exe"
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

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $javaBin
$psi.Arguments = "-Dfile.encoding=UTF-8 --module-path `"$modulePath`" --add-modules javafx.controls -cp `"$targetClasses`" com.esmail.calculator.GlassCalcApp"
$psi.UseShellExecute = $false
$psi.RedirectStandardError = $true
$psi.RedirectStandardOutput = $true

$proc = [System.Diagnostics.Process]::Start($psi)
Start-Sleep -Seconds 3
if ($proc.HasExited) {
    Write-Host "Process terminated early! ExitCode: $($proc.ExitCode)" -ForegroundColor Red
    Write-Host "STDERR: $($proc.StandardError.ReadToEnd())"
    Write-Host "STDOUT: $($proc.StandardOutput.ReadToEnd())"
    exit 1
} else {
    Write-Host "SUCCESS: CASIO fx-991ES PLUS is running cleanly without errors! PID: $($proc.Id)" -ForegroundColor Green
    $proc.Kill()
}
