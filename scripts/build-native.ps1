$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$nativeDir = Join-Path $root 'native'
$nativeResourceDir = Join-Path $root 'src\main\resources\native\windows-x86_64'
$targetResourceDir = Join-Path $root 'target\classes\native\windows-x86_64'

# Locate JDK
$jdk = $env:JAVA_HOME
if (-not $jdk -or -not (Test-Path "$jdk\include\jni.h")) {
    $candidates = @(
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot",
        "C:\Program Files\Java\jdk-17",
        "C:\Program Files\Java\jdk-21"
    )
    foreach ($cand in $candidates) {
        if (Test-Path "$cand\include\jni.h") { $jdk = $cand; break }
    }
}
if (-not $jdk) {
    throw 'JDK with jni.h was not found. Please set JAVA_HOME.'
}

# Locate NASM
$nasm = "nasm.exe"
if (-not (Get-Command nasm -ErrorAction SilentlyContinue)) {
    $nasmPath = "$env:LOCALAPPDATA\bin\NASM\nasm.exe"
    if (Test-Path $nasmPath) { $nasm = $nasmPath }
    else { throw 'NASM 2.16+ was not found.' }
}

# Locate Clang or GCC
$clang = "clang.exe"
if (-not (Get-Command clang -ErrorAction SilentlyContinue)) {
    $scratch = Split-Path $root -Parent
    $candidateClang = "$scratch\llvm-mingw-20261020-ucrt-x86_64\bin\x86_64-w64-mingw32-clang.exe"
    if (Test-Path $candidateClang) { $clang = $candidateClang }
}

Write-Host "Assembling x86-64 NASM math engine..." -ForegroundColor Cyan
& $nasm -f win64 (Join-Path $nativeDir 'src\asm_math.asm') -o (Join-Path $nativeDir 'asm_math.obj')
if ($LASTEXITCODE -ne 0) { throw "NASM assembly failed with code $LASTEXITCODE" }

Write-Host "Compiling JNI bridge and linking glasscalc.dll..." -ForegroundColor Cyan
$dllOutput = Join-Path $nativeDir 'glasscalc.dll'
& $clang -shared -O3 -DWIN32_LEAN_AND_MEAN `
    "-I$jdk\include" "-I$jdk\include\win32" `
    (Join-Path $nativeDir 'src\jni_bridge.c') `
    (Join-Path $nativeDir 'asm_math.obj') `
    -o $dllOutput
if ($LASTEXITCODE -ne 0) { throw "Clang compilation failed with code $LASTEXITCODE" }

New-Item -ItemType Directory -Force -Path $nativeResourceDir | Out-Null
New-Item -ItemType Directory -Force -Path $targetResourceDir | Out-Null
Copy-Item $dllOutput (Join-Path $nativeResourceDir 'glasscalc.dll') -Force
Copy-Item $dllOutput (Join-Path $targetResourceDir 'glasscalc.dll') -Force

Write-Host "SUCCESS: glasscalc.dll deployed to $nativeResourceDir" -ForegroundColor Green
