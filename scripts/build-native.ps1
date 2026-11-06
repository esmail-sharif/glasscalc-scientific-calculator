$ErrorActionPreference = 'Stop'

if (-not $env:JAVA_HOME) {
    throw 'JAVA_HOME must point to a JDK 21 installation.'
}
if (-not (Get-Command cmake -ErrorAction SilentlyContinue)) {
    throw 'CMake was not found. Install CMake 3.24 or newer.'
}
if (-not (Get-Command nasm -ErrorAction SilentlyContinue)) {
    throw 'NASM was not found. Install NASM 2.16 or newer and add it to PATH.'
}

$root = Split-Path -Parent $PSScriptRoot
$build = Join-Path $root 'native\build'
$nativeResourceDir = Join-Path $root 'src\main\resources\native\windows-x86_64'

cmake -S (Join-Path $root 'native') -B $build -A x64
cmake --build $build --config Release

New-Item -ItemType Directory -Force -Path $nativeResourceDir | Out-Null
$dll = Join-Path $build 'bin\Release\glasscalc.dll'
if (-not (Test-Path $dll)) {
    $dll = Join-Path $build 'bin\glasscalc.dll'
}
if (-not (Test-Path $dll)) {
    throw "Native DLL was not produced in $build"
}
Copy-Item $dll (Join-Path $nativeResourceDir 'glasscalc.dll') -Force
Write-Host "Native Assembly DLL copied to $nativeResourceDir" -ForegroundColor Green
