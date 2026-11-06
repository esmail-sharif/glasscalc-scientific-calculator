# GlassCalc — Advanced Scientific Calculator

GlassCalc is a Windows desktop scientific calculator with a modern glassmorphism interface, Arabic/English localization, and a native x86-64 NASM math engine connected to Java through JNI.

## Features

- JavaFX desktop interface with Arabic RTL and English LTR localization.
- DEG/RAD angle modes.
- Arithmetic, parentheses, powers, factorial, percentages, and constants.
- Trigonometric and inverse trigonometric functions.
- Logarithms, roots, absolute value, exponentials, min/max.
- Session calculation history.
- NASM x86-64 floating-point routines through a small JNI C ABI bridge.
- Java fallback for development when the native DLL is not present.

## Architecture

```text
JavaFX UI (Arabic / English)
          |
Java expression parser
          |
MathEngine facade
       /      \
 JNI + NASM   Java fallback
      |
 native/src/asm_math.asm
```

The parser and UI are Java. Native numeric primitives are routed to NASM when `glasscalc.dll` is available. The C source is only a JNI bridge; the arithmetic routines are implemented in Assembly.

## Requirements (Windows 10/11 x64)

- JDK 21
- Maven 3.9+
- CMake 3.24+
- NASM 2.16+ on PATH
- Visual Studio C++ Build Tools (MSVC)

## Build and run

Run PowerShell from the repository root:

```powershell
./scripts/build-native.ps1
mvn clean verify
mvn javafx:run
```

For Java UI/parser development without the native DLL, run `mvn javafx:run`; the app uses its Java fallback and indicates development mode.

## Example expressions

```text
sin(30) + sqrt(81)       # 9.5 in DEG mode
2^3^2                    # 512
log(1000)                # 3
ln(e^2)                  # 2
5!                       # 120
25%                      # 0.25
pow(2, 10)               # 1024
asin(0.5)                # 30 in DEG mode
```

## CI

The workflow in `.github/workflows/windows-ci.yml` builds the native DLL on Windows, runs tests, packages the app, and uploads build artifacts.

## Current limitations

- Native backend targets Windows x86-64.
- The x87 trigonometric routines may have range-reduction limitations for extremely large arguments.
- The repository builds a runnable Maven/JAR app; a signed MSI/EXE installer is a separate packaging step and has not yet been validated.

## License

MIT. See [LICENSE](LICENSE).
