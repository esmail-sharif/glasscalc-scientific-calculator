# Calculator Architecture Boundaries

## Current ownership after the UI audit

| Component | Responsibility | Must not own |
|---|---|---|
| `GlassCalcApp` | JavaFX application lifecycle, view construction, UI states, key dispatch, prompting, and rendering | Native ABI details, numeric algorithms, or calculation-history data structures |
| `CalculationHistory` | Ordered expression/result records, replay cursor, previous/next semantics | JavaFX nodes, labels, caret movement, or view styling |
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

## Remaining structural work

`GlassCalcApp` is still the top-level UI coordinator and remains large. The next safe seams are:

1. Extract a `StatEditorSession` to own X/Y/FREQ cell state and validation, leaving the app to render prompts and translate keypad events.
2. Extract mode dispatch into a `CalculationDispatcher` returning a typed result (display string, optional scalar Ans value, and history value).
3. Move keypad construction into a view factory whose callbacks are injected by the application controller.
4. Keep the root application as a small composition/lifecycle layer.

These larger moves are intentionally separate from the behavior fixes in this audit: each changes ownership across many state transitions and should be performed behind the current regression suite, one boundary at a time. No claim is made that the controller has already been fully decomposed.

## Dependency rule

Domain and engine packages remain JavaFX-free. UI code may depend on engines and formatting services; engines must not import JavaFX or invoke view callbacks. Native calls remain behind `NativeMath` and the Java fallback contract.
