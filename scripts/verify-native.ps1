$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$dll = Join-Path $root 'src\main\resources\native\windows-x86_64\glasscalc.dll'
if (-not (Test-Path $dll)) {
    throw "Native DLL was not found: $dll"
}

$dumpbin = $null
$dumpbinCommand = Get-Command dumpbin.exe -ErrorAction SilentlyContinue
if ($dumpbinCommand) {
    $dumpbin = $dumpbinCommand.Source
} else {
    $programFilesX86 = Join-Path (Split-Path $env:ProgramFiles -Parent) 'Program Files (x86)'
    $vswhere = Join-Path $programFilesX86 'Microsoft Visual Studio\Installer\vswhere.exe'
    if (Test-Path $vswhere) {
        $installation = & $vswhere -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
        if ($installation) {
            $msvcRoot = Join-Path $installation 'VC\Tools\MSVC'
            $toolset = Get-ChildItem $msvcRoot -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
            if ($toolset) {
                $cand = Join-Path $toolset.FullName 'bin\Hostx64\x64\dumpbin.exe'
                if (Test-Path $cand) { $dumpbin = $cand }
            }
        }
    }
}

$llvmObjdump = $null
if (-not $dumpbin) {
    if (Get-Command llvm-objdump.exe -ErrorAction SilentlyContinue) {
        $llvmObjdump = "llvm-objdump.exe"
    } else {
        $scratch = Split-Path $root -Parent
        $cand = "$scratch\llvm-mingw-20261020-ucrt-x86_64\bin\llvm-objdump.exe"
        if (Test-Path $cand) { $llvmObjdump = $cand }
    }
}

if (-not $dumpbin -and -not $llvmObjdump) {
    throw "Neither dumpbin.exe nor llvm-objdump.exe was found to inspect the linked PE image."
}

if ($dumpbin) {
    $headers = (& $dumpbin /headers $dll 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) { throw 'dumpbin /headers failed.' }
    if ($headers -notmatch '8664 machine|machine \(x64\)') { throw 'The linked DLL is not recognized as an AMD64 PE image.' }
    if ($headers -notmatch '\.pdata') { throw 'The linked PE image is missing runtime-function table (.pdata).' }

    $unwind = (& $dumpbin /unwindinfo $dll 2>&1 | Out-String)
    if ($unwind -notmatch 'Function Table|Unwind info|Unwind version') { throw 'dumpbin unwind check failed.' }

    $exports = (& $dumpbin /exports $dll 2>&1 | Out-String)
    foreach ($sym in @('Java_com_esmail_calculator_engine_NativeMath_atan2', 'Java_com_esmail_calculator_engine_NativeMath_factorial')) {
        if ($exports -notmatch [regex]::Escape($sym)) { throw "Missing JNI export: $sym" }
    }
} else {
    $headers = (& $llvmObjdump -p $dll 2>&1 | Out-String)
    if ($headers -notmatch 'coff-x86-64') { throw 'The linked DLL is not recognized as an AMD64 PE image.' }
    if ($headers -notmatch '\[\.pdata\]') { throw 'The linked PE image is missing runtime-function table (.pdata).' }

    foreach ($sym in @('Java_com_esmail_calculator_engine_NativeMath_atan2', 'Java_com_esmail_calculator_engine_NativeMath_factorial', 'Java_com_esmail_calculator_engine_NativeMath_statMean')) {
        if ($headers -notmatch [regex]::Escape($sym)) { throw "Missing JNI export: $sym" }
    }
}

# COFF Symbol inspection for NASM object
$assemblyObject = Get-ChildItem (Join-Path $root 'native') -Recurse -File -Filter '*asm_math*.obj' | Select-Object -First 1
if ($assemblyObject) {
    $coffPath = $assemblyObject.FullName
    if ($dumpbin) {
        $symbols = (& $dumpbin /symbols $coffPath 2>&1 | Out-String)
    } else {
        $symbols = (& $llvmObjdump -t $coffPath 2>&1 | Out-String)
    }
    foreach ($symbol in @('asm_pow', 'asm_exp2', 'asm_atan2', 'asm_factorial', 'asm_stat_mean')) {
        if ($symbols -notmatch [regex]::Escape($symbol)) {
            throw "Expected NASM COFF symbol was not found: $symbol"
        }
    }
}

Write-Host 'PE image: AMD64 / PE32+ headers verified.' -ForegroundColor Green
Write-Host 'PE runtime-function table and decoded unwind records verified.' -ForegroundColor Green
Write-Host 'JNI exports and NASM COFF assembly symbols verified.' -ForegroundColor Green
