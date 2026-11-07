# GlassCalc UI Control Coverage Matrix

**Baseline:** `9e428287fdec4a919c436d43f12aa277d1cc9386`  
**Audit branch:** `audit/ui-coverage-2026-11-07`  
**Scope:** JavaFX controls, button action wiring, state/menu routing, calculation dispatch, and mode-specific entry paths.

This document distinguishes source-level presence from automated execution. A source handler or calculator engine is not considered tested merely because it exists.

## Control inventory

### Top controls

| Control | Primary action | Alternate / navigation action | Automated coverage |
|---|---|---|---|
| SHIFT | Toggle shift modifier | Enables the printed shift legends | SHIFT action and alternate-legend tests |
| ALPHA | Toggle alpha modifier | Enables the printed alpha legends | A–F, X/Y/M, i/e, RanInt, CALC and statement-separator tests |
| REPLAY Up | Previous history / menu navigation | Context-dependent | History previous-entry test |
| REPLAY Down | Next history / menu navigation | Context-dependent | History next-entry and menu-page navigation tests |
| REPLAY Left | Move expression cursor left | — | Button and physical-key caret tests |
| REPLAY Right | Move expression cursor right | — | Button and physical-key caret tests |
| MODE | Open operating-mode menu | SHIFT+MODE opens SETUP | Mode and SETUP route tests |
| ON | Power on | — | OFF/ON cycle test |

### Function keypad

| Key | Primary action | SHIFT legend/action | ALPHA legend/action | BASE-N legend/action |
|---|---|---|---|---|
| CALC | Variable input / evaluate | SOLVE | Insert `=` | — |
| ∫ dx | Insert integral | Insert derivative | Statement separator | — |
| x⁻¹ | Reciprocal | Factorial | — | — |
| log_■ | Logarithm with base | Summation | — | — |
| ■/□ | Fraction separator | Mixed fraction | — | — |
| √■ | Square root | Cube root | — | — |
| x² | Square | Cube | — | DEC |
| x^■ | Power | nth root | — | HEX |
| log | Common logarithm | 10^x | — | BIN |
| ln | Natural logarithm | e^x | — | OCT |
| (−) | Unary minus | Complex angle | A | — |
| ° ′ ″ | DMS input | Backspace mapping | B | — |
| hyp | Hyperbolic modifier | Absolute-value route | C | — |
| sin | Sine | arcsine | D | — |
| cos | Cosine | arccosine | E | — |
| tan | Tangent | arctangent | F | — |
| RCL | Recall variable | Store variable | STO, RCL, and stored-variable result tests | — |
| ENG | Engineering notation | Engineering shift | i | — |
| ( | Open parenthesis | Percent | — | — |
| ) | Close parenthesis | Comma | X | — |
| S⇔D | Exact/decimal toggle | Mixed fraction display | Y | — |
| M+ | Add to memory | Subtract from memory | M | M+, M−, M recall, and ALPHA-M tests |

### Numeric keypad

| Key | Primary action | SHIFT action | ALPHA action |
|---|---|---|---|
| 7 | Digit 7 | CONST prompt | — |
| 8 | Digit 8 | CONV prompt | — |
| 9 | Digit 9 | Clear expression, variables and memory | SHIFT+9 reset test |
| DEL | Delete | Insert | DEL and SHIFT+DEL overwrite toggle tests |
| AC | All clear / commit data editor | Power off | Matrix/vector AC commit and SHIFT+AC power-cycle tests |
| 4 | Digit 4 | MATRIX menu | MATRIX dimension and data-editor tests |
| 5 | Digit 5 | VECTOR menu | VECTOR dimension and data-editor tests |
| 6 | Digit 6 | — | — |
| × | Multiply | Permutation | SHIFT permutation legend test | — |
| ÷ | Divide | Combination | SHIFT combination legend test | — |
| 1 | Digit 1 | STAT menu | — |
| 2 | Digit 2 | CMPLX menu | — |
| 3 | Digit 3 | BASE-N menu | — |
| + | Add | Polar coordinate conversion | — |
| − | Subtract | Rectangular coordinate conversion | — |
| 0 | Digit 0 | Rnd | — |
| . | Decimal point | Ran# | RanInt# |
| ×10ˣ | Scientific exponent | π | e |
| Ans | Insert previous answer | DRG conversion | — |
| = | Execute / accept prompt | — | — |

## Mode routing inventory

| MODE menu choice | Target mode | Expected initial UI state | Test status |
|---:|---|---|---|
| 1 | COMP | NORMAL | Automated route test |
| 2 | CMPLX | NORMAL | Automated route test |
| 3 | STAT | STAT_TYPE_SELECT | Automated route test |
| 4 | BASE-N | NORMAL | Automated route test |
| 5 | EQN | EQN_TYPE_SELECT | Automated route test |
| 6 | MATRIX | NORMAL | Automated route test |
| 7 | TABLE | TABLE_FX_INPUT | Automated route test |
| 8 | VECTOR | NORMAL | Automated route test |

## Confirmed automated UI paths

The current JavaFX suite has **45 passing tests**. The suite builds the production calculator and fires the real JavaFX keypad buttons. One test shows the actual JavaFX Stage, loads the CASIO stylesheet and verifies that the LCD fields and every key receive visible, non-zero layout bounds.

- **Mode routing:** all eight MODE choices and their expected initial states.
- **Setup:** first-page Math/Line, Deg/Rad/Gra, Fix/Sci/Norm choices; second-page mixed/improper fractions, rectangular/polar complex format, STAT FREQ, Dot/Comma decimal separator and contrast 0–9.
- **STAT:** all eight model selections and data-entry journeys; optional X→FREQ (1‑VAR) and X→Y→FREQ (paired); blank FREQ defaults to 1; invalid zero FREQ does not commit; changing FREQ setting clears the editor. An alternate spreadsheet editor is now available with `Ctrl+G` while entering STAT data; it supports X/Y/FREQ columns, row add/delete/edit, validation and Apply/Cancel semantics. The UI suite verifies that the shortcut refuses to open with a partially entered sequential row.
- **CONST/CONV:** all 40 constant codes and all 40 conversion codes entered through the keypad; conversion result updates Ans.
- **EQN:** two-variable linear, three-variable linear, quadratic and cubic UI workflows, with result navigation.
- **TABLE/SOLVE:** Start/End/Step prompt replacement, row navigation, invalid zero-step handling, and edited SOLVE initial guess.
- **MATRIX/VECTOR:** scalar Ans propagation; dimension selection for a 2×2 matrix and a 2D vector; editor AC commits and ESC cancels without mutating stored data.
- **BASE-N/CMPLX:** radix changes, bitwise menu, arithmetic, complex expression and complex display format.
- **SHIFT/ALPHA:** all printed A–F, X/Y/M, i/e, RanInt, inverse-trig/hyperbolic, fraction, root, DMS, engineering, arithmetic and navigation-related alternate legends exercised.
- **Memory/replay:** STO, RCL, M+, M−, SHIFT+9 clear, DEL/INS toggle, OFF/ON cycle, REPLAY Up/Down/Left/Right and physical-key ENTER/ESCAPE/BACKSPACE.

The current tests validate event wiring, representative calculation journeys, mode state changes, and rendered layout geometry. They do not claim pixel-for-pixel equivalence to a physical CASIO unit.

## Remaining coverage and environmental limits

1. **Visual reference validation:** the Windows test opens and lays out the actual JavaFX Stage, but no calibrated physical fx-991ES PLUS device or approved pixel-baseline capture was available for a true hardware/image comparison.
2. **Natural textbook input:** the application currently uses a text-field expression editor. The MathIO/LineIO preference changes its result-display mode; it does not reproduce the physical calculator's full stacked fraction/radical input editor.
3. **STAT editor presentation:** sequential X/Y/FREQ prompts remain the default keypad workflow. A separate spreadsheet-style grid is also available via `Ctrl+G`; its full interactive-window behavior still needs dedicated JavaFX UI automation on Windows.
4. **Controller decomposition:** history and STAT editing state are now extracted into `CalculationHistory` and `StatEditorSession`, and CI enforces JavaFX-free engine/domain packages. `GlassCalcApp` still owns the remaining UI state and event routing; a keypad/view factory and application composition cleanup remain.

## Acceptance rule

A control is marked **verified** only when a test fires the actual production JavaFX control (or sends the real keyboard event), checks the resulting state/output, and asserts the expected side effects. Static inventory alone is marked **mapped**, not verified.
