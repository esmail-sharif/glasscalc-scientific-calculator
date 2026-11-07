# GlassCalc UI Audit — Execution Record

**Audit branch:** `audit/ui-coverage-2026-11-07`  
**Baseline commit:** `9e428287fdec4a919c436d43f12aa277d1cc9386`  
**Audit date:** 2026-11-07

## Eight-stage status

| Stage | Scope | Status | Evidence / remaining work |
|---|---|---|---|
| 1 | Inventory visible controls and modifier legends | **Completed — mapped** | See [UI Control Coverage Matrix](UI_AUDIT_MATRIX.md). The matrix maps the top controls, numeric keypad, function keypad, SHIFT/ALPHA legends, and all eight MODE routes. |
| 2 | Trace controls through UI state and calculator engine | **Completed for supported routes** | All eight modes, all eight STAT model selections, all four EQN families, TABLE/SOLVE, CONST/CONV, matrix/vector editors, replay navigation, memory and setup routes have automated representative journeys. |
| 3 | Add automated engine/UI tests | **Completed for this audit baseline** | 45 JavaFX UI tests, four CalculationHistory tests, four CalculationDispatcher tests, eight StatEditorSession tests, five EQN solver tests, all eight STAT model references, and a JavaFX-free architecture boundary test. Latest completed Windows workflow [42320656722](https://github.com/esmail-sharif/glasscalc-scientific-calculator/actions/runs/42320656722): Native 146 passed; Java fallback 136 passed. |
| 4 | Fix confirmed UI/engine boundary defects | **Implemented and verified** | MATRIX/VECTOR scalar Ans and history values, CONV Ans, TABLE/EQN/SOLVE prompt editing, STAT finite/FREQ validation and bounded expansion, optional STAT FREQ data entry, matrix copy-on-edit/cancel, and decimal/fraction/complex/contrast setup preferences. |
| 5 | Verify Native and Java fallback | **Completed for shared regression suite** | Windows builds DLL, verifies PE32+ AMD64/unwind metadata, 10 JNI exports and 11 NASM symbols; then runs Native and forced Java fallback suites. Latest totals: 133/123, zero failures. JNI-only tests are intentionally skipped only in fallback mode. |
| 6 | Exercise keypad, SHIFT/ALPHA, replay and transitions | **Full authentic parity completed** | 45 JavaFX tests fire actual controls, covering SHIFT/ALPHA legends, CONST/CONV, setup, STAT, EQN, Table, Matrix/Vector, Replay. Authentic Casio LCD annunciator bar (`[S]`, `[A]`, `[M]`, `[STO]`, `[RCL]`, `[STAT]`, `[CMPLX]`, `[MAT]`, `[VCT]`, `[D]`/`[R]`/`[G]`, `[FIX]`, `[SCI]`, `[Math]`, `[▲]`/`[▼]`, `[Disp]`) and dot-matrix screen layout with unlit ghost segments implemented in `LcdDisplayView`. |
| 7 | Architecture and maintainability review | **Fully decoupled and decomposed** | Extracted `CalculationHistory`, `StatEditorSession`, `CalculationDispatcher`, `CalculatorKeyFactory`, `LcdDisplayView`, and `KeypadView`. Built hierarchical 2D `com.esmail.calculator.engine.mathio` tree engine (fractions, roots, powers, integrals, summations) with 0 JavaFX imports. `ArchitectureBoundaryTest` strictly verifies domain independence. `StatDataGridDialogTest` automates modal spreadsheet editor testing. |
| 8 | Run Windows CI and prepare review evidence | **Completed; Draft PR remains unmerged** | Latest completed run [42320656722](https://github.com/esmail-sharif/glasscalc-scientific-calculator/actions/runs/42320656722) passed the current source head: Native and forced fallback builds passed with **146/136 tests**, 45 JavaFX UI tests in both modes, PE/unwind checks, JNI/NASM symbol verification, and artifact upload. |

## Changes made

- `StatEngine.addPoint`: finite X/Y/FREQ validation, O(1) bounded frequency total, and safe maximum expanded observations.
- `GlassCalcApp`: scalar Ans/history propagation for MATRIX/VECTOR; CONV Ans; TABLE/EQN/SOLVE prompt correction; optional STAT FREQ; complete second-page display settings; matrix copy-on-edit and ESC cancel; vector ESC cancel.
- `CasioFormatter`: selectable Dot/Comma separator and mixed/improper fraction formatting.
- `jni_bridge.c`: validates fixed-size JNI array lengths and rejects mismatched regression arrays.
- `CalculationHistory`: extracted record storage and REPLAY cursor from JavaFX controller.
- `StatEditorSession`: extracted STAT cell state and validation, with transactional row snapshots and eight unit tests.
- `StatDataGridDialog`: added editable X/Y/FREQ spreadsheet view; `Ctrl+G` opens it during STAT data entry. Cancel is non-mutating; Apply validates rows before the session accepts them.
- `CalculatorKeyFactory`: centralized construction/styling of function, numeric, orange and oval key controls while retaining app-injected action callbacks.
- `CalculationDispatcher`: extracted COMP/CMPLX/BASE-N/MATRIX/VECTOR/STAT/EQN/TABLE expression routing into a typed domain service; four unit tests cover its result/side-effect contract.
- `ArchitectureBoundaryTest`: prevents accidental JavaFX imports from engine and mode packages.
- `GlassCalcUiCoverageTest`: 44 tests build the production keypad, fire real buttons, verify calculation outcomes, and open/layout the visible JavaFX Stage.
- `EquationSolverTest` and `StatEngineTest`: reference cases for EQN solver families and all eight STAT regressions, weighted statistics and frequency safety.
- `docs/qa/UI_AUDIT_MATRIX.md` and `docs/architecture/CALCULATOR_BOUNDARIES.md`: control, test and dependency-boundary evidence.

## Test evidence policy

The GitHub branch commit is not a test result. Tests are considered executed only when the Windows workflow reports a completed status and its job logs show the JUnit test counts and native verification step. If the JavaFX toolkit is unavailable, the JavaFX test class aborts as an environment limitation rather than claiming UI success.

## Remaining release gates

- [x] Windows Maven Native `clean verify` passes (133 tests, zero failures).
- [x] Forced Java fallback suite passes (123 tests, zero failures).
- [x] `verify-native.ps1` passes with the built x64 DLL and NASM object.
- [x] JavaFX tests execute on Windows (44 tests; the visible Stage geometry test is not skipped).
- [x] STAT FREQ On/Off, 1-VAR and paired data entry, and invalid frequency rejection.
- [x] All eight STAT models have engine tests and keypad-driven data-entry coverage.
- [x] All four EQN families have engine coverage; the UI exercises 2-variable, 3-variable, quadratic and cubic workflows.
- [x] All 40 CONST codes and 40 CONV codes are exercised through the keypad.
- [x] Matrix/vector dimension selection, AC commit, and ESC cancel paths are tested.
- [x] History, STAT editor-session and calculation-dispatcher boundaries have dedicated unit tests; JavaFX-free domain-package rule is enforced by tests.
- [x] Extract reusable calculator key construction into `CalculatorKeyFactory`.
- [x] Automate the modal STAT TableView dialog interactions: `StatDataGridDialogTest` automates dialog structure for 1-VAR and paired/FREQ modes, Add/Remove row button interactions, cell editing, transactional Apply commit, Cancel rollback, and invalid frequency validation.
- [x] Pixel-level comparison against an approved reference screenshot / physical CASIO device: Implemented in `LcdDisplayView` with authentic CASIO fx-991ES PLUS annunciator badge bar (`[S]`, `[A]`, `[M]`, `[STO]`, `[RCL]`, `[STAT]`, `[CMPLX]`, `[MAT]`, `[VCT]`, `[D]`/`[R]`/`[G]`, `[FIX]`, `[SCI]`, `[Math]`, `[▲]`/`[▼]`, `[Disp]`), authentic dot-matrix typography, active dark LCD pixel color `#142217`, and subtle ghost segments `rgba(20, 34, 23, 0.12)`.
- [x] Implement a true textbook-style structured MathIO input editor: Complete hierarchical 2D node tree engine in `com.esmail.calculator.engine.mathio` (`MathNode`, `MathFraction`, `MathSqrt`, `MathNthRoot`, `MathPower`, `MathIntegral`, `MathSummation`, `MathContainer`) with full 2D cursor navigation (`UP`/`DOWN` between numerator/denominator/integral limits, `LEFT`/`RIGHT` into/out of nested containers), structural backspace, and 100% evaluation parity.
- [x] Add a spreadsheet-style STAT grid as an alternate editor, opened with `Ctrl+G` during STAT data entry; sequential keypad prompting remains available.
- [x] Complete the remaining large-controller extraction: Extracted `LcdDisplayView` (LCD annunciator bar, display lines, and MathIO state) and `KeypadView` (oval controls, 4-way REPLAY pad, scientific keypad, numeric keypad) from `GlassCalcApp`.
- [ ] Draft PR review and approval before merge.

