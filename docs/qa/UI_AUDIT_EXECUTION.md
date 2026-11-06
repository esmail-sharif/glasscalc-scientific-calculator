# GlassCalc UI Audit — Execution Record

**Audit branch:** `audit/ui-coverage-2026-11-07`  
**Baseline commit:** `9e428287fdec4a919c436d43f12aa277d1cc9386`  
**Audit date:** 2026-11-07

## Eight-stage status

| Stage | Scope | Status | Evidence / remaining work |
|---|---|---|---|
| 1 | Inventory every visible control and modifier legend | **Completed — source mapping** | See [UI Control Coverage Matrix](UI_AUDIT_MATRIX.md). This stage inventories control labels and their registered actions. |
| 2 | Trace each control through UI state and calculator engine | **Completed — primary routes** | MODE choices, SHIFT mode shortcuts, COMP, CMPLX, BASE-N, MATRIX, VECTOR, STAT, EQN and TABLE entry routes were traced in `GlassCalcApp`. Several secondary modifier flows remain unverified. |
| 3 | Add automated engine/UI tests | **Implemented; CI pending** | Added `GlassCalcUiCoverageTest` for actual JavaFX buttons and added STAT input-boundary tests. Existing engine JUnit suites remain in place. |
| 4 | Fix confirmed UI/engine boundary defects | **Implemented on audit branch; CI pending** | Scalar results from MATRIX/VECTOR now synchronize `Ans` and the numeric history value. STAT now rejects non-finite X/Y/FREQ and limits aggregate frequency to 1,000,000 observations. |
| 5 | Verify Native and Java fallback parity | **Partially completed** | JNI now validates fixed-size array capacities and equal regression array lengths. Full Native-vs-Java result parity still requires Windows execution with the DLL plus a separate forced-fallback test run. |
| 6 | Exercise keyboard, SHIFT, ALPHA, and state transitions | **Partially implemented** | JavaFX smoke tests cover modifier routing, mode selection, scalar Ans chaining, and keypad action handlers. Full modifier-by-modifier event-sequence coverage and keyboard events remain. |
| 7 | Architecture and maintainability review | **Reviewed; refactor deferred** | `GlassCalcApp` remains a large controller. Splitting it before stable regression tests would increase risk; defer extraction to a dedicated change set after this test baseline. |
| 8 | Run Windows CI, review results, and prepare release evidence | **Pending workflow run** | This branch must be tested through a draft PR / Windows Actions run. Do not treat the source changes as green until the run is complete and logs are checked. |

## Changes made

- `StatEngine.addPoint`: validates finite values and a positive integral frequency; limits total expanded observations.
- `GlassCalcApp.executeStatement`: synchronizes scalar `Ans` / current result values for MATRIX and VECTOR.
- `jni_bridge.c`: validates array capacity before fixed-size native access; rejects mismatched X/Y regression lengths.
- `GlassCalcUiCoverageTest`: constructs the production keypad and fires its actual JavaFX buttons.
- `StatEngineTest`: adds non-finite input and frequency-limit tests.

## Test evidence policy

The GitHub branch commit is not a test result. Tests are considered executed only when the Windows workflow reports a completed status and its job logs show the JUnit test counts and native verification step. If the JavaFX toolkit is unavailable, the JavaFX test class aborts as an environment limitation rather than claiming UI success.

## Remaining release gates

- [ ] Windows Maven `clean verify` passes.
- [ ] `verify-native.ps1` passes with the built x64 DLL and NASM object.
- [ ] JavaFX keypad smoke tests execute (not aborted).
- [ ] Full keyboard / modifier matrix is tested.
- [ ] STAT FREQ entry is either implemented in UI or explicitly marked unsupported.
- [ ] Native and forced Java fallback produce matching reference results.
- [ ] Full regression cases for all eight STAT models and EQN/TABLE editor flows pass.
- [ ] Draft PR reviewed before merge.
