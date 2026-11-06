$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

if (-not (Test-Path (Join-Path $root 'src\main\resources\native\windows-x86_64\glasscalc.dll'))) {
    & (Join-Path $PSScriptRoot 'build-native.ps1')
}

Push-Location $root
try {
    mvn javafx:run
} finally {
    Pop-Location
}
