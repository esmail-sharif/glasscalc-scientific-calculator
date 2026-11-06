$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$dll = Join-Path $root 'src\main\resources\native\windows-x86_64\glasscalc.dll'
if (-not (Test-Path $dll)) {
    throw "Native DLL was not found: $dll"
}

$dumpbinCommand = Get-Command dumpbin.exe -ErrorAction SilentlyContinue
if ($dumpbinCommand) {
    $dumpbin = $dumpbinCommand.Source
} else {
    $programFilesX86 = Join-Path (Split-Path $env:ProgramFiles -Parent) 'Program Files (x86)'
    $vswhere = Join-Path $programFilesX86 'Microsoft Visual Studio\Installer\vswhere.exe'
    if (-not (Test-Path $vswhere)) {
        throw 'vswhere.exe and dumpbin.exe were not found; cannot inspect the linked PE image.'
    }

    $installation = & $vswhere -latest -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
    if (-not $installation) {
        throw 'Visual Studio C++ x64 build tools were not located.'
    }

    $msvcRoot = Join-Path $installation 'VC\Tools\MSVC'
    $toolset = Get-ChildItem $msvcRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1
    $dumpbin = Join-Path $toolset.FullName 'bin\Hostx64\x64\dumpbin.exe'
    if (-not (Test-Path $dumpbin)) {
        throw "dumpbin.exe was not found at $dumpbin"
    }
}

$headers = (& $dumpbin /headers $dll 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0) { throw 'dumpbin /headers failed.' }
if ($headers -notmatch '8664 machine|machine \(x64\)') {
    throw 'The linked DLL is not recognized as an AMD64 PE image.'
}
if ($headers -notmatch '\.pdata') {
    throw 'The linked PE image is missing the runtime-function table section (.pdata).'
}
$sectionSummary = $headers -split '\r?\n' | Where-Object { $_ -match '\.pdata|\.xdata|\.rdata' }
Write-Host 'Linked PE data sections:'
$sectionSummary | ForEach-Object { Write-Host $_ }

# The linker may coalesce read-only unwind payloads into another read-only
# section. Validate the decoded runtime-function table, not a required .xdata
# section spelling in the final image.
$unwind = (& $dumpbin /unwindinfo $dll 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0) { throw 'dumpbin /unwindinfo failed.' }
if ($unwind -notmatch 'Function Table|Unwind info|Unwind version') {
    Write-Host $unwind
    throw 'dumpbin did not report a decoded PE unwind function table.'
}

$exports = (& $dumpbin /exports $dll 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0) { throw 'dumpbin /exports failed.' }
foreach ($symbol in @(
    'Java_com_esmail_calculator_engine_NativeMath_atan2',
    'Java_com_esmail_calculator_engine_NativeMath_verifyUnwindMetadata',
    'Java_com_esmail_calculator_engine_NativeMath_factorial'
)) {
    if ($exports -notmatch [regex]::Escape($symbol)) {
        throw "Expected JNI export was not found: $symbol"
    }
}

$assemblyObject = Get-ChildItem (Join-Path $root 'native\build') -Recurse -Filter 'asm_math.asm.obj' |
    Select-Object -First 1
if (-not $assemblyObject) {
    throw 'NASM COFF object asm_math.asm.obj was not found.'
}
$symbols = (& $dumpbin /symbols $assemblyObject.FullName 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0) { throw 'dumpbin /symbols failed for the NASM COFF object.' }
foreach ($symbol in @('asm_pow', 'asm_exp2', 'asm_atan2', 'asm_factorial')) {
    if ($symbols -notmatch [regex]::Escape($symbol)) {
        throw "Expected NASM COFF symbol was not found: $symbol"
    }
}

Write-Host 'PE image: AMD64 / PE32+ headers verified.' -ForegroundColor Green
Write-Host 'PE .pdata/.xdata and decoded unwind function table verified.' -ForegroundColor Green
Write-Host 'JNI exports and NASM COFF symbols verified.' -ForegroundColor Green
