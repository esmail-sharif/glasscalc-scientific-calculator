# GlassCalc UI Audit — Execution Record

**Audit branch:** `audit/ui-coverage-2026-11-07`  
**Baseline commit:** `9e428287fdec4a919c436d43f12aa277d1cc9386`  
**Audit date:** 2026-11-07

## Eight-stage status

| Stage | Scope | Status | Evidence / remaining work |
|---|---|---|---|
| 1 | Inventory every visible control and modifier legend | **Completed — source mapping** | See [UI Control Coverage Matrix](UI_AUDIT_MATRIX.md). This stage inventories control labels and their registered actions. |
| 2 | Trace each control through UI state and calculator engine | **Completed — primary routes** | MODE choices, SHIFT mode shortcuts, COMP, CMPLX, BASE-N, MATRIX, VECTOR, STAT, EQN and TABLE entry routes were traced in `GlassCalcApp`. Several secondary modifier flows remain unverified. |
| 3 | Add automated engine/UI tests | **Completed for the audited baseline** | `GlassCalcUiCoverageTest` contains 17 tests that build and fire actual JavaFX keypad controls. The latest Windows native run passed 96 tests; the forced Java fallback run passed 86 tests. STAT regression/weight boundary coverage is included. |
| 4 | Fix confirmed UI/engine boundary defects | **Implemented and verified by CI** | Scalar results from MATRIX/VECTOR synchronize `Ans` and numeric history. STAT rejects non-finite X/Y/FREQ and limits aggregate frequency to 1,000,000. TABLE/EQN/SOLVE prompt values replace defaults on first input; prompt backspace is supported; EQN accepts an untouched zero coefficient. CONV now updates `Ans` with the converted value. |
| 5 | Verify Native and Java fallback parity | **Completed for current regression suite** | Windows CI builds the x64 DLL, verifies the PE/unwind structure, 10 JNI exports and 11 NASM symbols, then runs the suite twice. Native: 96 passed. Forced Java fallback: 86 passed; the JNI-only class is correctly skipped because the DLL is deliberately disabled. |
| 6 | Exercise keyboard, SHIFT, ALPHA, and state transitions | **Broad automated sample coverage; full legend matrix remains open** | 17 JavaFX tests now cover all MODE choices, SHIFT+MODE, CONST/CONV prompts, STAT/MATRIX/VECTOR shortcuts, ALPHA variable insertion, SHIFT inverse-trig insertion, ENTER/BACKSPACE/ESCAPE, COMP/Ans, CALC prompts, STO/RCL, M+, one-variable STAT, TABLE prompt editing/row navigation, EQN coefficients, BASE-N radix/logic menu, CMPLX, DRG, SOLVE, MATRIX/VECTOR scalar Ans. Unexercised modifier combinations remain listed in the coverage matrix. |
| 7 | Architecture and maintainability review | **Reviewed; refactor deferred** | `GlassCalcApp` remains a large controller. Splitting it before stable regression tests would increase risk; defer extraction to a dedicated change set after this test baseline. |
| 8 | Run Windows CI, review results, and prepare release evidence | **Completed; Draft PR remains unmerged** | Latest run [42316203896](https://github.com/esmail-sharif/glasscalc-scientific-calculator/actions/runs/42316203896) completed successfully on Windows: 96 native tests passed, 86 forced-fallback tests passed, native binary verification passed, and the application artifact uploaded. The earlier initial UI keyboard test failure was corrected and re-run successfully. |

## Changes made

- `StatEngine.addPoint`: validates finite values and a positive integral frequency; limits total expanded observations.
- `GlassCalcApp.executeStatement`: synchronizes scalar `Ans` / current result values for MATRIX and VECTOR.
- `jni_bridge.c`: validates array capacity before fixed-size native access; rejects mismatched X/Y regression lengths.
- `GlassCalcUiCoverageTest`: constructs the production keypad and fires its actual JavaFX buttons, including representative multi-step mode workflows.
- `StatEngineTest`: adds non-finite input and frequency-limit tests, exponential A·B^X regression, and weighted two-variable descriptive-statistics tests.

## Test evidence policy

The GitHub branch commit is not a test result. Tests are considered executed only when the Windows workflow reports a completed status and its job logs show the JUnit test counts and native verification step. If the JavaFX toolkit is unavailable, the JavaFX test class aborts as an environment limitation rather than claiming UI success.

## Remaining release gates

- [x] Windows Maven `clean verify` passes (96 tests, zero failures).
- [x] `verify-native.ps1` passes with the built x64 DLL and NASM object.
- [x] JavaFX keypad smoke tests execute (17 tests, not aborted).
- [x] Forced Java fallback suite passes (86 tests, zero failures).
- [ ] Full keyboard / modifier matrix is tested.
- [ ] STAT FREQ entry is either implemented in UI or explicitly marked unsupported.
- [x] Native and forced Java fallback produce matching reference outputs for the shared engine/UI regression suite.
- [ ] Full regression cases for all eight STAT models and EQN/TABLE editor flows pass.
- [ ] Draft PR reviewed before merge.
