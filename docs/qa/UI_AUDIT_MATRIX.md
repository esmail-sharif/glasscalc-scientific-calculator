# GlassCalc UI Control Coverage Matrix

**Baseline:** `9e428287fdec4a919c436d43f12aa277d1cc9386`  
**Audit branch:** `audit/ui-coverage-2026-11-07`  
**Scope:** JavaFX controls, button action wiring, state/menu routing, calculation dispatch, and mode-specific entry paths.

This document distinguishes source-level presence from automated execution. A source handler or calculator engine is not considered tested merely because it exists.

## Control inventory

### Top controls

| Control | Primary action | Alternate / navigation action | Automated coverage |
|---|---|---|---|
| SHIFT | Toggle shift modifier | Enables the printed shift legends | Shortcut routing tests |
| ALPHA | Toggle alpha modifier | Enables the printed alpha legends | Handler wiring only |
| REPLAY Up | Previous history / menu navigation | Context-dependent | Handler wiring only |
| REPLAY Down | Next history / menu navigation | Context-dependent | Handler wiring only |
| REPLAY Left | Move expression cursor left | — | Handler wiring only |
| REPLAY Right | Move expression cursor right | — | Handler wiring only |
| MODE | Open operating-mode menu | SHIFT+MODE opens SETUP | Mode and SETUP route tests |
| ON | Power on | — | Handler wiring only |

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
| RCL | Recall variable | Store variable | — | — |
| ENG | Engineering notation | Engineering shift | i | — |
| ( | Open parenthesis | Percent | — | — |
| ) | Close parenthesis | Comma | X | — |
| S⇔D | Exact/decimal toggle | Mixed fraction display | Y | — |
| M+ | Add to memory | Subtract from memory | M | — |

### Numeric keypad

| Key | Primary action | SHIFT action | ALPHA action |
|---|---|---|---|
| 7 | Digit 7 | CONST prompt | — |
| 8 | Digit 8 | CONV prompt | — |
| 9 | Digit 9 | Clear expression, variables and memory | — |
| DEL | Delete | Insert | — |
| AC | All clear | Power off | — |
| 4 | Digit 4 | MATRIX menu | — |
| 5 | Digit 5 | VECTOR menu | — |
| 6 | Digit 6 | — | — |
| × | Multiply | Permutation | — |
| ÷ | Divide | Combination | — |
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

The current JavaFX suite has **17 passing tests**. It constructs the production keypad and fires real JavaFX Controls on the JavaFX Application Thread.

- Every rendered button has a non-null action handler; all eight MODE selections reach their expected modes/states.
- SHIFT+MODE opens SETUP and RAD selection is applied. SHIFT shortcuts reach STAT, MATRIX, VECTOR, CONST and CONV entry paths.
- COMP calculation and Ans chaining; CALC variable prompting; ALPHA variable insertion; SHIFT inverse-trig insertion.
- STO stores a variable; RCL recalls Ans; M+ updates memory and M reads it back.
- CONST code 28 inserts the selected constant; CONV code 01 converts 10 in to cm and updates the Ans value.
- STAT data entry and descriptive mean; EQN quadratic coefficient entry; SOLVE with an edited initial guess.
- TABLE Start/End/Step first-input replacement and row navigation.
- BASE-N radix selection, bitwise menu operator insertion, and arithmetic; CMPLX expression calculation and complex menu insertion; DRG postfix insertion.
- MATRIX determinant and VECTOR dot-product scalar results synchronize `Ans`.
- ENTER executes, BACKSPACE deletes, and ESCAPE clears.

These tests verify event wiring, representative user journeys and resulting state. They do not claim pixel-perfect layout, every printed SHIFT/ALPHA legend, visual equivalence to a physical CASIO unit, or full exploratory interaction testing.

## Coverage gaps identified during source review

1. **STAT frequency entry is not exposed in the interactive data-entry flow.** The engine accepts a frequency argument, but the UI calls `addPoint(x)` / `addPoint(x,y)`, which always use frequency 1. There is no visible FREQ column or frequency-input state in `GlassCalcApp`.
2. **Several ALPHA and SHIFT legends are only covered by action-handler presence, not dedicated behavioral assertions.** This includes complex-number menu operations, RCL/STO, ENG, memory, DMS, and the complete inverse-trigonometric/hyperbolic set.
3. **REPLAY history navigation, delete/insert, OFF/ON, and every setup mode need dedicated event-sequence tests.**
4. **EQN coefficient workflows, all eight STAT regression models, all CONST/CONV codes, TABLE boundary cases, and MATRIX/VECTOR editor commit/cancel flows require separate end-to-end cases.**
5. **Keyboard handling is not equivalent to mouse-button handling in the current app.** The event filter explicitly handles ENTER, ESCAPE, arrows, BACK_SPACE, and digits in prompt states; all other keyboard mappings need an explicit decision and test.

## Acceptance rule

A control is marked **verified** only when a test fires the actual production JavaFX control (or sends the real keyboard event), checks the resulting state/output, and asserts the expected side effects. Static inventory alone is marked **mapped**, not verified.
