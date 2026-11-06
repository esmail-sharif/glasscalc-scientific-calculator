$ErrorActionPreference = "Stop"

Write-Host "0. Verifying native x86-64 NASM binaries..."
& powershell -ExecutionPolicy Bypass -File "$PSScriptRoot\verify-native.ps1"
if ($LASTEXITCODE -ne 0) { throw "Native verification failed!" }

$jdk = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
$javac = "$jdk\bin\javac.exe"
$java = "$jdk\bin\java.exe"

$m2fx = "$env:USERPROFILE\.m2\repository\org\openjfx"
$fxJars = @(
    "$m2fx\javafx-base\21.0.13\javafx-base-21.0.13-win.jar",
    "$m2fx\javafx-base\21.0.13\javafx-base-21.0.13.jar",
    "$m2fx\javafx-graphics\21.0.13\javafx-graphics-21.0.13-win.jar",
    "$m2fx\javafx-graphics\21.0.13\javafx-graphics-21.0.13.jar",
    "$m2fx\javafx-controls\21.0.13\javafx-controls-21.0.13-win.jar",
    "$m2fx\javafx-controls\21.0.13\javafx-controls-21.0.13.jar"
)
$modulePath = $fxJars -join ";"

if (Test-Path "target\classes") {
    Remove-Item -Recurse -Force "target\classes\*" -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Path "target\classes" -Force | Out-Null
}
if (Test-Path "target\test-classes") {
    Remove-Item -Recurse -Force "target\test-classes\*" -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Path "target\test-classes" -Force | Out-Null
}

Write-Host "1. Compiling main Java sources..."
$mainSources = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 --module-path $modulePath --add-modules javafx.controls -d "target\classes" $mainSources
if ($LASTEXITCODE -ne 0) { throw "Compilation of main Java sources failed!" }

Write-Host "2. Copying resources..."
Copy-Item -Path "src\main\resources\*" -Destination "target\classes\" -Recurse -Force

Write-Host "3. Finding dependencies for tests..."
$m2junit = "$env:USERPROFILE\.m2\repository\org\junit"
$junitJars = Get-ChildItem -Path $m2junit -Recurse -Filter "*.jar" | ForEach-Object { $_.FullName }

$m2repo = "$env:USERPROFILE\.m2\repository"
$otherJars = Get-ChildItem -Path $m2repo -Recurse -Filter "*.jar" | Where-Object {
    $_.FullName -match 'opentest4j' -or $_.FullName -match 'apiguardian'
} | ForEach-Object { $_.FullName }

$testCp = @("target\classes", "target\test-classes") + $junitJars + $otherJars + $fxJars
$testCpStr = $testCp -join ";"

Write-Host "4. Compiling test Java sources..."
$testSources = Get-ChildItem -Path "src\test\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -cp $testCpStr -d "target\test-classes" $testSources
if ($LASTEXITCODE -ne 0) { throw "Compilation of test Java sources failed!" }

Write-Host "5. Running QuickTestRunner..."
& $java '-Dfile.encoding=UTF-8' --module-path $modulePath --add-modules javafx.controls -cp "$testCpStr" com.esmail.calculator.QuickTestRunner
if ($LASTEXITCODE -ne 0) { throw "QuickTestRunner failed!" }

Write-Host "6. Running UiIntegrationTest..."
& $java '-Dfile.encoding=UTF-8' --module-path $modulePath --add-modules javafx.controls -cp "$testCpStr" com.esmail.calculator.UiIntegrationTest
if ($LASTEXITCODE -ne 0) { throw "UiIntegrationTest failed!" }
