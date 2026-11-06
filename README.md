# GlassCalc — Advanced Scientific Calculator

GlassCalc is a Windows desktop scientific calculator with a modern glassmorphism interface, Arabic/English localization, and a native x86-64 NASM math engine connected to Java through JNI.

## Scientific features

- Basic arithmetic, precedence, nested parentheses, implicit multiplication, percentages, factorials and scientific notation.
- DEG/RAD mode, Ans, and constants pi, e, tau, and phi.
- Trigonometric functions: sin, cos, tan, sec, csc, cot.
- Inverse trigonometric functions: asin, acos, atan, atan2.
- Hyperbolic and inverse hyperbolic functions: sinh, cosh, tanh, asinh, acosh, atanh.
- Exponents and roots: pow, exp, exp2, sqrt, cbrt, root(value, degree).
- Logarithms: ln, log/log10, log2, logbase(value, base).
- Numeric functions: abs, floor, ceil, round, trunc, sign, hypot.
- Integer and remainder operations: mod, remainder, idiv, gcd, lcm.
- Combinatorics: fact/factorial, perm/npr, comb/ncr.
- Statistics: mean, median, population variance/stdev, samplevar/samplestdev, sum, product, min, max.
- Separate Basic and Advanced Science keypads; session calculation history.

## Example expressions

~~~text
sin(30) + sqrt(81)       # 9.5 in DEG mode
2^3^2                    # 512
logbase(32, 2)           # 5
ln(e^2)                  # 2
root(-27, 3)             # -3
atan2(1, 1)              # 45 in DEG mode
mean(2, 4, 6)            # 4
variance(2, 4, 6)        # population variance = 8/3
comb(5, 2)               # 10
5!                       # 120
25%                      # 0.25
~~~

## Architecture

~~~text
JavaFX UI (Arabic / English)
          |
Expression parser and evaluator
          |
MathEngine (domain rules, stable fallbacks, statistics)
       /      \
 JNI + NASM   Java Math fallback
      |
 native/src/asm_math.asm
~~~

The native DLL currently accelerates arithmetic, x87 trigonometric/logarithmic routines, exp, exp2, log2, atan2, pow, and factorial. Java Math is deliberately used for functions where its IEEE-754 edge handling or range reduction is more robust. The parser and UI remain independent of the backend.

## Native numeric policy

- MathEngine rejects undefined calculator inputs such as division by zero, invalid logarithm domains, and non-real even roots of negative values.
- Factorial accepts non-negative integers. Values from 171! upward overflow a double to positive infinity; the expression evaluator reports out-of-range final values.
- x87 FSIN, FCOS, and FPTAN range failures are detected via C2. The facade uses Java's range-reduced implementation for arguments outside the x87 range.
- NASM routines that allocate stack space include Windows x64 unwind metadata.

## Build and run (Windows 10/11 x64)

Requirements: JDK 21, Maven 3.9+, CMake 3.24+, NASM 2.16+, and Visual Studio C++ Build Tools.

~~~powershell
./scripts/build-native.ps1
mvn clean verify
mvn javafx:run
~~~

The native CI workflow builds the DLL, requires the JNI integration tests to load it, runs all tests, packages the app, and uploads the JAR and DLL artifact.

## Known packaging limitation

The repository currently builds a runnable Maven/JAR application with its Windows native DLL. A signed MSI/EXE installer is a separate packaging step and is not included in this change.

## License

MIT. See LICENSE.
