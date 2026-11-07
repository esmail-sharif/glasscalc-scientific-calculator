# Calculator Architecture Boundaries

## Current ownership after the UI audit

| Component | Responsibility | Must not own |
|---|---|---|
| `GlassCalcApp` | JavaFX application lifecycle, view construction, UI states, key dispatch, prompting, and rendering | Native ABI details, numeric algorithms, or calculation-history data structures |
| `CalculationHistory` | Ordered expression/result records, replay cursor, previous/next semantics | JavaFX nodes, labels, caret movement, or view styling |
| `StatEditorSession` | Active STAT engine, pending X/Y/FREQ cell, validation, and immutable observation rows | JavaFX controls, dialogs, rendering, or keyboard dispatch |
| `CalculationDispatcher` | Mode-specific calculation routing and typed display/Ans/history effects | JavaFX controls, UI states, or direct LCD updates |
| `CalculatorKeyFactory` | Reusable key control construction, styling, sizing, and modifier legends | Calculator state, mode routing, or key action decisions |
| `StatDataGridDialog` | Editable spreadsheet-style STAT table and detached edit transaction | Statistical calculations or mutation before Apply |
| `ExpressionParser` / `ComplexExpressionParser` | Parsing and evaluation of scalar and complex expressions | JavaFX controls or stage lifecycle |
| Mode engines (`StatEngine`, `EquationSolver`, `MatrixEngine`, `VectorEngine`, `TableGenerator`, `BaseNEngine`) | Domain rules and mode-specific calculations/state | Rendering or direct JavaFX event handling |
| `CasioFormatter` | Exact/decimal/fraction/DMS formatting and decimal separator preference | UI node mutation |
| `MathEngine` / `NativeMath` | Select supported native acceleration or Java fallback | UI prompts or keyboard routing |
| `native/src/jni_bridge.c` + NASM | Native ABI, buffer validation and low-level math primitives | Mode state or interface rendering |

## Extraction completed in this audit

Calculation history used to live as three parallel mutable fields in `GlassCalcApp`:
- expression list;
- numeric result list;
- replay cursor index.

It is now one `CalculationHistory` service with a typed immutable entry record and explicit operations:
- `seed` for the startup showcase entry;
- `record` for a new calculation;
- `previous` and `next` for replay navigation;
- `canPrevious` and `canNext` for the LCD navigation indicators.

The JavaFX application now only renders the selected entry. Four dedicated unit tests cover the service's empty, seeded, boundary and navigation behavior. UI replay tests still exercise the actual REPLAY controls.

STAT data entry has also been moved into `StatEditorSession`. It owns the transient X/Y/FREQ cell state, blank-cell defaults, finite-value and frequency validation, and committed observation rows. Grid-style updates are applied atomically by rebuilding a replacement engine before swapping it into the session. Eight unit tests cover sequential entry, weighted rows, invalid values, clearing, and row add/update/delete behavior. The JavaFX layer now translates keypad actions into session operations and renders the active cell.

Mode evaluation has been extracted into `CalculationDispatcher`. It returns a typed result containing display text, optional Ans/current-result updates, and the REPLAY history value. This keeps mode-specific expression routing outside the JavaFX controller. Four unit tests cover COMP, BASE-N, scalar MATRIX/VECTOR results, and CMPLX result/history semantics.

Reusable function, numeric, orange, and oval key construction has been extracted into `CalculatorKeyFactory`. It receives application callbacks and centralizes key sizing, printed SHIFT/ALPHA/BASE legends, event wiring, and indicator refresh without holding calculator state.

## Remaining structural work

`GlassCalcApp` remains a large coordinator. The key-component factory is extracted, but the keypad row composition, top-level view hierarchy, and application lifecycle are still built in `GlassCalcApp`. A further extraction of complete keypad composition and a smaller application composition layer remains. History, STAT editing state, and expression-mode dispatch are extracted; the full controller decomposition is not complete.

## Dependency rule

Domain and engine packages remain JavaFX-free. UI code may depend on engines and formatting services; engines must not import JavaFX or invoke view callbacks. Native calls remain behind `NativeMath` and the Java fallback contract.
